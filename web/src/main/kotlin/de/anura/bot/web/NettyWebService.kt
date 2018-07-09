package de.anura.bot.web

import de.anura.bot.config.WebConfig
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.cookie
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.openid4java.consumer.ConsumerManager
import org.openid4java.discovery.DiscoveryInformation
import org.slf4j.LoggerFactory
import java.net.URLEncoder

class NettyWebService(private val config: WebConfig) : WebService {

    private lateinit var server: Http4kServer
    private val logger = LoggerFactory.getLogger(javaClass)
    private val idManager = ConsumerManager()
    private val discovered: DiscoveryInformation
    private val tokens = TokenManager()
    private val sessions = SessionManager()

    init {
        idManager.maxAssocAttempts = 0

        val discoveries = idManager.discover("https://steamcommunity.com/openid")
        discovered = discoveries[0] as DiscoveryInformation

        start()
    }

    // Application control

    private fun start() {
        val app: HttpHandler = { handle(it) }
        server = app.asServer(Netty(config.port)).start()
        logger.info("Started web server (Netty) on port ${config.port}")
    }

    override fun stop() {
        server.stop()
        logger.info("Stopped the web server")
    }

    // Web Handler

    private fun handle(request: Request): Response {
        val host = request.header("Host") ?: ""

        // Getting the session token
        val sessionToken = request.cookie(sessions.cookieName)

        // Getting the session by the token
        val session = if (sessionToken != null) {
            // If there's already a session with the token, we'll use it
            // If there's no session with this token, we'll start a new one
            sessions.findSession(sessionToken.value) ?: sessions.startSession()
        } else {
            // If there isn't a session token, we'll start a new session
            sessions.startSession()
        }

        // Updating the last access date of the session
        session.lastAccess = System.currentTimeMillis() / 1000

        val redirectUrl = config.proxyUrl ?: if (host.isBlank()) config.hostUri() else "http://$host"
        val handler = RequestHandler(request, redirectUrl, session, config)

        // Checking for the correct host
        if (!host.equals(config.hostUri(), true)) {
            logger.warn("Request from invalid host: $host")
            return handler.mainPage()
        }

        // Routing
        val response = when (request.uri.path.toLowerCase()) {
            "/authenticate" -> handler.authenticate(tokens)
            "/accept" -> handler.accept(idManager, discovered)
            "/error" -> handler.error()
            "/redirect" -> handler.redirect(idManager, discovered)
            "/success" -> handler.success()
            else -> handler.mainPage()
        }

        return if (session.fresh) {
            // Setting the cookie for the new session
            response.cookie(sessions.cookieForSession(session))
        } else {
            // Or just returing the response
            response
        }
    }

    // Url

    override fun getLoginUrl(uid: String): String {
        val token = tokens.tokenFor(uid)
        val encodedToken = URLEncoder.encode(token, "UTF-8")
        return if (config.proxyUrl != null) {
            "${config.proxyUrl}/authenticate?token=$encodedToken"
        } else {
            "http://${config.host}:${config.port}/authenticate?token=$encodedToken"
        }
    }

}