package de.anura.bot.config

import com.github.theholywaffle.teamspeak3.TS3Query
import org.apache.commons.configuration.HierarchicalINIConfiguration
import java.io.File
import java.nio.file.Files

object AppConfig {
    lateinit var teamspeak: TsConfig
    lateinit var web: WebConfig
    lateinit var mysql: SqlConfig
    lateinit var steam: SteamConfig

    private lateinit var ini: HierarchicalINIConfiguration

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
        if (stream != null) {
            Files.copy(stream, to.toPath())
        } else {
            throw ConfigException("Cannot find config file inside JAR")
        }
    }

    private fun readConfig(from: File) {
        ini = HierarchicalINIConfiguration(from)

        teamspeak = TsConfig(
            getString("teamspeak", "host"),
            getInt("teamspeak", "port"),
            getString("teamspeak", "user"),
            getString("teamspeak", "password"),
            getInt("teamspeak", "virtualserver"),
            getString("teamspeak", "nickname"),
            getString("teamspeak", "channel"),
            getFloodRate("teamspeak", "flood_rate")
        )

        web = WebConfig(
            getBoolean("web", "enabled"),
            getString("web", "host"),
            getInt("web", "port"),
            if (getString("web", "proxy_url").equals("null", true)) null else getString("web", "proxy_url"),
            getInt("web", "maxSteamGroups"),
            getString("web", "external_url")
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
        val value = ini.getSection(section).getString(key)

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

    private fun getBoolean(section: String, key: String): Boolean {
        val string = getString(section, key).trim()

        return string == "1" || string.lowercase() == "true"
    }

    private fun getFloodRate(section: String, key: String): TS3Query.FloodRate {
        val string = getString(section, key).trim()

        return when (string.uppercase()) {
            "DEFAULT" -> TS3Query.FloodRate.DEFAULT
            "UNLIMITED" -> TS3Query.FloodRate.UNLIMITED
            else -> throw ConfigException("Value for key $section.$key must be 'DEFAULT' or 'UNLIMTED'")
        }
    }

}