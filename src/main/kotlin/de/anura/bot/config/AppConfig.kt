package de.anura.bot.config

import org.ini4j.Wini
import java.io.File
import java.nio.file.Files

class AppConfig {
    lateinit var teamspeak: TsConfig
    lateinit var web: WebConfig
    lateinit var mysql: SqlConfig
    lateinit var steam: SteamConfig

    private lateinit var ini: Wini

    init {
        val file = File("config.ini")

        if (!file.exists()) {
            copyConfig(file)
            throw ConfigException("A new config file was copied! Please edit it and start this bot again")
        } else {
            readConfig(file)
        }

    }

    private fun copyConfig(to: File) {
        val stream = this.javaClass.getResourceAsStream("/config.example.ini")
        Files.copy(stream, to.toPath())
    }

    private fun readConfig(from: File) {
        ini = Wini(from)

        teamspeak = TsConfig(
                getString("teamspeak", "host"),
                getInt("teamspeak", "port"),
                getString("teamspeak", "user"),
                getString("teamspeak", "password"),
                getInt("teamspeak", "virtualserver"),
                getString("teamspeak", "nickname"),
                getString("teamspeak", "channel")
        )

        web = WebConfig(
                getString("web", "host"),
                getInt("web", "port")
        )

        mysql = SqlConfig(
                getString("mysql", "host"),
                getInt("mysql", "port"),
                getString("mysql", "user"),
                getString("mysql", "password"),
                getString("mysql", "database")
        )

        steam = SteamConfig(
                getString("steam", "api_key")
        )
    }

    private fun getString(section: String, key: String): String {
        val value = ini[section]?.get(key)

        if (value == null || value.trim() == "") {
            throw ConfigException("No value found for $section.$key")
        }

        return value
    }

    private fun getInt(section: String, key: String): Int {
        val string = getString(section, key)

        return try {
            string.toInt()
        } catch (ex: NumberFormatException) {
            throw ConfigException("Value for key $section.$key isn't a integer", ex)
        }
    }

}