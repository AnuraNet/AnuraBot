package de.anura.bot.web

import de.anura.bot.config.WebConfig
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.slf4j.LoggerFactory

class WebService(private val config: WebConfig) {

    private lateinit var server: Http4kServer
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        start()
    }

    fun start() {
        val app: HttpHandler = { request -> handle(request) }
        server = app.asServer(Netty(config.port)).start()
        logger.info("Started web server (Netty) on port ${config.port}")
    }

    fun handle(request: Request): Response {
        val query = request.query("test")

        return if (query != null) {
            Response(Status.OK).body("Wow $query")
        } else {
            Response(Status.OK).body("Hey")
        }
    }

    fun getUri(uid: String) {

    }

    fun stop() {
        server.stop()
        logger.info("Stopped the web server")
    }

}