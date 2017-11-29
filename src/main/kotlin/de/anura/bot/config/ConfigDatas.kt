package de.anura.bot.config

data class TsConfig(
        val host: String,
        val port: Int,
        val user: String,
        val password: String,
        val virtualserver: Int,
        val nickname: String,
        val channel: String
)

data class WebConfig(
        val host: String,
        val port: Int
)

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