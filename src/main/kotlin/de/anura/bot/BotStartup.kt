package de.anura.bot

import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException
import de.anura.bot.config.AppConfig
import de.anura.bot.config.ConfigException
import de.anura.bot.database.Database
import de.anura.bot.teamspeak.TsBot
import de.anura.bot.web.WebService
import org.jdbi.v3.core.ConnectionException
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread

fun main(args: Array<String>) {

    val logger = LoggerFactory.getLogger("de.anura.bot.Startup")

    // Reading the configuration
    val config = try {
        AppConfig()
    } catch (ex: ConfigException) {
        logger.error("There was an error while reading the configuration:", ex)
        return
    }

    // Connecting to the database
    try {
        Database.connect(config.mysql)
    } catch (ex: ConnectionException) {
        logger.error("Couldn't connect to the database! Please check the credentials. \n{}", ex.message)
        return
    }

    // Connecting to the teamspeak server
    val tsbot = try {
        TsBot(config.teamspeak)
    } catch (ex: TS3ConnectionFailedException) {
        logger.error("Couldn't connect to the teamspeak server:", ex)
        return
    }

    val web = WebService(config.web)

    Runtime.getRuntime().addShutdownHook(thread {
        Scheduler.stop()
        web.stop()
        tsbot.disconnect()

    })

}