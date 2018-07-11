package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.TS3Api
import com.github.theholywaffle.teamspeak3.TS3Config
import com.github.theholywaffle.teamspeak3.TS3Query
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy
import de.anura.bot.config.AppConfig
import org.slf4j.LoggerFactory

object TsBot {

    private val appConfig = AppConfig.teamspeak
    private var connected = false
    private val logger = LoggerFactory.getLogger(javaClass)
    private var firstJoin = true

    private lateinit var query: TS3Query
    private lateinit var eventListener: EventListener
    val api: TS3Api by lazy { query.api }
    var queryChannel: Int = -1
    var queryClientId: Int = -1

    init {
        connect()
    }

    private fun connect() {
        val config = TS3Config()

        config.setHost(appConfig.host)
        config.setQueryPort(appConfig.port)
        config.setReconnectStrategy(ReconnectStrategy.linearBackoff())
        config.setFloodRate(appConfig.floodRate)

        config.setConnectionHandler(object : ConnectionHandler {
            override fun onConnect(query: TS3Query?) {
                val api = if (query?.api != null) {
                    query.api
                } else {
                    logger.error("There's no Teamspeak query while connecting")
                    return
                }

                // Authenticating
                try {
                    api.login(appConfig.user, appConfig.password)
                } catch (ex: TS3CommandFailedException) {
                    // If the credentials are rejected, we stop the bot
                    logger.error("Can't connect to the Teamspeak server, " +
                            "because the credentials were rejected", appConfig.virtualserver)
                    connected = false
                    if (firstJoin) {
                        System.exit(1)
                    }
                    return
                }
                // Selecting the correct virtual server
                try {
                    api.selectVirtualServerById(appConfig.virtualserver)
                } catch (ex: TS3CommandFailedException) {
                    // If a server with the id doesn't exists, we stop the bot
                    logger.error("Can't connect to the Teamspeak server, " +
                            "because there's no virtaul server with the id '{}'", appConfig.virtualserver)
                    connected = false
                    if (firstJoin) {
                        System.exit(1)
                    }
                    return
                }

                api.setNickname(appConfig.nickname)

                connected = true

                logger.info("Connected to the Teamspeak server (${appConfig.host})")

                // Registering events
                api.registerEvent(TS3EventType.TEXT_PRIVATE)
                api.registerEvent(TS3EventType.CHANNEL)

                queryClientId = api.whoAmI().id

                // Moving the query client to the channel, if it's set and found
                val queryChannel = api.getChannelByNameExact(appConfig.channel, false)
                if (queryChannel != null) {
                    api.moveQuery(queryChannel)
                    logger.info("Joined the channel '${appConfig.channel}'")
                    TsBot.queryChannel = queryChannel.id
                } else {
                    logger.warn("Couldn't find the channel '${appConfig.channel}'.")
                }

                // Populating the cache with client data
                api.clients.forEach { client ->
                    eventListener.populateCache(client)
                    TimeManager.load(client.uniqueIdentifier)
                }

                if (firstJoin) {
                    // This must be done in a thread, otherwise it would block the whole ts3 api
                    Thread {
                        Thread.sleep(15)
                        logger.info("Adding missing time & game groups to all online users...")
                        SteamConnector.setAllClientsGroups()
                        TimeGroups.checkAllClients()
                    }.start()
                }

                firstJoin = false
            }

            override fun onDisconnect(query: TS3Query?) {
                connected = false

                eventListener.clearCache()
                TimeManager.saveAll(true)

                logger.info("Lost connection to the Teamspeak server (${appConfig.host})")
            }
        })

        // Building the query object using the configuration
        query = TS3Query(config)

        // Creating the EventListener & registering it
        eventListener = EventListener(this, query)
        query.api.addTS3Listeners(eventListener)

        // Connecting to the teamspeak server
        query.connect()
    }

    fun disconnect() {
        query.exit()
        TimeManager.saveAll(true)
        logger.info("Disconnected from the Teamspeak server (${appConfig.host})")
    }

}