package de.anura.bot.config

import com.github.theholywaffle.teamspeak3.TS3Query

data class TsConfig(
        val host: String,
        val port: Int,
        val user: String,
        val password: String,
        val virtualserver: Int,
        val nickname: String,
        val channel: String,
        val floodRate: TS3Query.FloodRate
)

data class WebConfig(
        val enabled: Boolean,
        val host: String,
        val port: Int,
        val externalUrl: String
) {
    fun hostUri(): String {
        return if (port == 80 || port == 443) host else host + ":" + port
    }
}

data class SqlConfig(
        val host: String,
        val port: Int,
        val user: String,
        val password: String,
        val database: String
)

data class SteamConfig(
        val apiKey: String
)