package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.TS3Config
import com.github.theholywaffle.teamspeak3.TS3Query
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel
import de.anura.bot.config.TsConfig
import org.slf4j.LoggerFactory

class TsBot(val appConfig: TsConfig) {

    private var connected: Boolean = false
    private val logger = LoggerFactory.getLogger(javaClass)

    private var query: TS3Query? = null // todo improve this
    private var eventListener: EventListener? = null
    var queryChannel: Channel? = null

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
                val api = query?.api ?: return
                connected = true

                api.login(appConfig.user, appConfig.password) // todo abort when the credentials are rejected
                api.selectVirtualServerById(appConfig.virtualserver) // todo abort when this server doesn't exists
                api.setNickname(appConfig.nickname)

                logger.info("Connected to the Teamspeak server (${appConfig.host})")

                api.registerEvent(TS3EventType.TEXT_PRIVATE)
                api.registerEvent(TS3EventType.CHANNEL)

                queryChannel = api.getChannelByNameExact(appConfig.channel, false)
                if (queryChannel != null) {
                    api.moveQuery(queryChannel)
                    logger.info("Joined the channel '${appConfig.channel}'")
                } else {
                    logger.warn("Couldn't find the channel '${appConfig.channel}'.")
                }

                api.clients.forEach { client ->
                    eventListener?.populateCache(client)
                    TimeManager.load(client.uniqueIdentifier)
                }

            }

            override fun onDisconnect(query: TS3Query?) {
                connected = false

                eventListener?.clearCache()
                TimeManager.saveAll(true)

                logger.info("Lost connection to the Teamspeak server (${appConfig.host})")
            }
        })

        val query = TS3Query(config)
        val api = query.api
        eventListener = EventListener(this, api)
        query.connect()
        api.addTS3Listeners(eventListener)
    }

    fun disconnect() {
        query?.exit()
        TimeManager.saveAll(true)
        logger.info("Disconnected from the Teamspeak server (${appConfig.host})")
    }

}