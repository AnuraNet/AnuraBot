package de.anura.bot

import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException
import de.anura.bot.config.AppConfig
import de.anura.bot.config.ConfigException
import de.anura.bot.database.Database
import de.anura.bot.teamspeak.TsBot
import de.anura.bot.web.SteamAPI
import de.anura.bot.web.SteamException
import de.anura.bot.web.SteamHttpException
import de.anura.bot.web.WebServiceLoader
import org.jdbi.v3.core.ConnectionException
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {

    val logger = LoggerFactory.getLogger("de.anura.bot.BotStartupKt")

    // Reading the configuration
    val config = try {
        AppConfig
    } catch (ex: ConfigException) {
        logger.error("There was an error while reading the configuration:", ex)
        return
    }

    // Connecting to the database
    try {
        Database.connect()
    } catch (ex: ConnectionException) {
        logger.error("Couldn't connect to the database! Please check the credentials. \n{}", ex.message)
        return
    }

    // Checking the Steam API Key
    try {
        SteamAPI.setKey(config.steam.apiKey)
        logger.info("Your Steam API Key is valid")
    } catch (ex: SteamException) {
        if (ex is SteamHttpException && ex.status == 403)
            logger.error("The Steam API Key is invalid. Please check the config!")
        else
            logger.error("There was an error contacting the Steam API")
        return
    }

    // Connecting to the teamspeak server
    val tsbot = try {
        TsBot
    } catch (ex: TS3ConnectionFailedException) {
        logger.error("Couldn't connect to the teamspeak server:", ex)
        return
    }

    // Enabling the web service if required
    val web = try {
        WebServiceLoader.service
    } catch (ex: ClassNotFoundException) {
        System.exit(1)
        null
    }

    Runtime.getRuntime().addShutdownHook(Thread({
        Scheduler.stop()
        web?.stop()
        tsbot.disconnect()
    }))

}