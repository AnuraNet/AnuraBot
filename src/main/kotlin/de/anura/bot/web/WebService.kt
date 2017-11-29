package de.anura.bot.web

import de.anura.bot.config.WebConfig
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer

class WebService(private val config: WebConfig) {

    private lateinit var server: Http4kServer

    init {
        start()
    }

    fun start() {
        val app: HttpHandler = { request -> handle(request) }
        server = app.asServer(Netty(config.port)).start()
    }

    fun handle(request: Request): Response {
        val query = request.query("test")

        return if (query != null) {
            Response(Status.OK).body("Wow $query")
        } else {
            Response(Status.OK).body("Hey")
        }
    }

    fun getUri() {

    }

    fun stop() {
        server.stop()
    }

}