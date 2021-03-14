package de.anura.bot.web.handlers

import de.anura.bot.web.AbstractRequestHandler
import de.anura.bot.web.NettyWebService.RequestInfo
import org.http4k.core.Response
import org.http4k.core.Status

class AssetRequestHandler(requestInfo: RequestInfo) : AbstractRequestHandler(requestInfo) {

    /**
     * Serves a file from classpath with the [path]
     */
    fun serveAsset(path: String): Response {
        val stream = this.javaClass
            .getResourceAsStream(path)

        return Response(Status.OK)
            .body(stream)
    }

}