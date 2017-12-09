package de.anura.bot.web

import de.anura.bot.config.WebConfig
import org.http4k.core.*
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.openid4java.consumer.ConsumerManager
import org.openid4java.discovery.DiscoveryInformation
import org.openid4java.message.AuthSuccess
import org.openid4java.message.MessageException
import org.openid4java.message.ParameterList
import org.slf4j.LoggerFactory

class NettyWebService(private val config: WebConfig) : WebService {

    private lateinit var server: Http4kServer
    private val logger = LoggerFactory.getLogger(javaClass)
    private val idManager = ConsumerManager()
    private val discovered: DiscoveryInformation

    init {
        // todo change logging behavior of id manager
        idManager.maxAssocAttempts = 0

        val discoveries = idManager.discover("http://steamcommunity.com/openid")
        discovered = idManager.associate(discoveries)

        start()
    }

    private fun start() {
        val app: HttpHandler = { handle(it) }
        server = app.asServer(Netty(config.port)).start()
        logger.info("Started web server (Netty) on port ${config.port}")
    }

    private fun handle(request: Request): Response {
        val host = request.header("Host") ?: ""

        if (!host.equals(config.hostUri(), true)) {
            logger.warn("Request from invalid host: $host")
            return mainPage(request)
        }

        return when (request.uri.path.toLowerCase()) {
            "/authenticate" -> authenticate(request)
            "/accept" -> accept(request)
            else -> mainPage(request)
        }
    }

    private fun authenticate(request: Request): Response {
        // todo token
        val returnUrl = "http://" + (request.header("Host") ?: "") + "/accept"

        val authRequest = idManager.authenticate(discovered, returnUrl)

        return Response(Status.TEMPORARY_REDIRECT)
                .header("Location", authRequest.getDestinationUrl(true))
    }

    private fun accept(request: Request): Response {
        val parameters = ParameterList(request.uri.queries().toMap())
        val receivingUrl = "http://" + (request.header("Host") ?: "") + request.uri.path

        val verification = try {
            idManager.verify(receivingUrl, parameters, discovered)
        } catch (ex: MessageException) {
            return textOK("Oh there it seems as something is missing. Please try it again!")
        }

        val identifier = verification.verifiedId

        return if (identifier != null) {
            val authSuccess = verification.authResponse as AuthSuccess

            val steamid = authSuccess.claimed.replace("http://steamcommunity.com/openid/id/", "")

            val player = SteamAPI.getPlayerSummaries(steamid)
            val games = SteamAPI.getOwnedGames(steamid)

            if (player != null) {
                val gamesStr = games.joinToString(", ") { it.name }
                textOK("Hey ${player.personaname} with claimed ${authSuccess.claimed}<br>Games:$gamesStr")
            } else {
                textOK("What $steamid")
            }

        } else {
            textOK("There was an error with your login. Please try it again or contact our team!")
        }
    }

    private fun mainPage(request: Request): Response {
        return textOK("Although this is the wrong page you can visit our <a href='http://example.com'>website</a>")
    }

    private fun textOK(text: String): Response {
        return Response(Status.OK)
                .body("""
                    <!DOCTYPE HTML>
                    <html>
                    <head>
                       <meta charset='UTF-8'>
                       <title>Teamspeak Bot</title>
                    </head>
                    <body>
                       <div style='font-family:monospace;'>$text</div>
                    </body>
                    """.trimIndent())
                .header("Content-Type", "text/html")
    }

    override fun getLoginUrl(uid: String): String {
        return ""
    }

    override fun stop() {
        server.stop()
        logger.info("Stopped the web server")
    }

}