package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.TS3Api
import com.github.theholywaffle.teamspeak3.TS3Config
import com.github.theholywaffle.teamspeak3.TS3Query
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy
import de.anura.bot.config.TsConfig
import org.slf4j.LoggerFactory

class TsBot(private val appConfig: TsConfig) {

    private var connected: Boolean = false
    private val logger = LoggerFactory.getLogger(javaClass)

    private lateinit var query: TS3Query

    init {
        connect()
    }

    private fun connect() {
        val config = TS3Config()

        config.setHost(appConfig.host)
        config.setQueryPort(appConfig.port)
        config.setReconnectStrategy(ReconnectStrategy.linearBackoff())

        config.setConnectionHandler(object : ConnectionHandler {
            override fun onConnect(query: TS3Query?) {
                val api = query?.api ?: return
                connected = true

                api.login(appConfig.user, appConfig.password) // todo abort when the credentials are rejected
                api.selectVirtualServerById(appConfig.virtualserver) // todo abort when this server doesn't exists
                api.setNickname(appConfig.nickname)

                logger.info("Connected to the Teamspeak server (${appConfig.host})")

                api.registerEvent(TS3EventType.TEXT_PRIVATE)
                val channel = api.getChannelByNameExact(appConfig.channel, false)
                if (channel != null) {
                    api.moveQuery(channel)
                    logger.info("Joined the channel '${appConfig.channel}'")
                } else {
                    logger.warn("Couldn't find the channel '${appConfig.channel}'.")
                }

                api.clients.forEach { client -> TimeManager.load(client.uniqueIdentifier) }

            }

            override fun onDisconnect(query: TS3Query?) {
                connected = false

                TimeManager.saveAll(true)

                logger.info("Lost connection to the Teamspeak server (${appConfig.host})")
            }
        })

        query = TS3Query(config)
        query.connect()

        val api = query.api

        api.addTS3Listeners(EventListener(api))
    }

    fun getApi(): TS3Api? {
        return if (connected) query.api else null
    }

    fun disconnect() {
        query.exit()
    }

}