package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.TS3Api
import com.github.theholywaffle.teamspeak3.TS3Config
import com.github.theholywaffle.teamspeak3.TS3Query
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy
import de.anura.bot.config.TsConfig

class TsBot(private val appConfig: TsConfig) {

    private var connected: Boolean = false

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

                api.login(appConfig.user, appConfig.password)
                api.selectVirtualServerById(appConfig.virtualserver)
                api.setNickname(appConfig.nickname)

                api.registerEvent(TS3EventType.TEXT_PRIVATE)

                api.clients.forEach { client -> TimeManager.load(client.uniqueIdentifier) }
            }

            override fun onDisconnect(query: TS3Query?) {
                connected = false

                TimeManager.saveAll(true)
            }
        })

        query = TS3Query(config)
        query.connect()

        val api = query.api

        // inaktiv seit 5 Min nicht mehr zählen
        // abwesend

        api.addTS3Listeners(EventListener(api))
    }

    fun getApi(): TS3Api? {
        return if (connected) query.api else null
    }

    fun disconnect() {
        query.exit()
    }

}