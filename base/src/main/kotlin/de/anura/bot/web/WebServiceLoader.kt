package de.anura.bot.web

import de.anura.bot.config.AppConfig
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import kotlin.reflect.full.primaryConstructor

object WebServiceLoader {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val config = AppConfig
    val service: WebService

    init {
        service = loadWeb() ?: ConfigWebService()
    }

    private fun loadWeb(): WebService? {

        val serviceClass = try {
            Class.forName("de.anura.bot.web.NettyWebService").kotlin
        } catch (ex: ClassNotFoundException) {
            null
        }

        if (config.web.enabled) {
            if (serviceClass == null) {
                // The web service is enabled, but the jar isn't compiled with the web module
                logger.error("The web service is enabled, but the jar isn't compiled with the web module!\n" +
                        "Please disable the web serive in the configuration or use the jar compiled with the web module.")
                throw ClassNotFoundException("Couldn't find the class NettyWebService")
            } else {
                // The web service is enabled and jar is compiled with the web module
                val constructor = serviceClass.primaryConstructor
                val newInstance = constructor?.call(config.web)
                return newInstance as WebService
            }
        } else {
            // Then web service isn't enabled
            logger.info(when (serviceClass) {
                null -> "Compiled with the web module but not enabled"
                else -> "Compiled without the web module which isn't enabled"
            })
            return null
        }
    }

    class ConfigWebService : WebService {

        private val config = AppConfig

        override fun getLoginUrl(uid: String): String {
            val externalUrl = config.web.externalUrl
            val encodedUid = URLEncoder.encode(uid, "UTF-8")

            // Appending the unique id to the url
            return if (externalUrl.contains('?')) {
                externalUrl + "&uid=$encodedUid"
            } else {
                externalUrl + "?uid=$encodedUid"
            }
        }

        override fun stop() {
            // Doing nothing
        }
    }

}