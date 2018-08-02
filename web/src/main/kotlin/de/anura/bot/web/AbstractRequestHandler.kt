package de.anura.bot.web

import de.anura.bot.config.WebConfig
import de.anura.bot.web.NettyWebService.RequestInfo
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

abstract class AbstractRequestHandler(requestInfo: RequestInfo) {

    protected val request: Request = requestInfo.request
    protected val redirect: String = requestInfo.redirect
    protected val session: SessionManager.Session = requestInfo.session
    protected val config: WebConfig = requestInfo.config
    private val tokens: TokenManager = requestInfo.tokens

    protected fun verifyToken(): String {
        val token = request.query("token") ?: throw WebException(0x100, "Can't find toke in request query!")
        val uniqueId = tokens.destroyToken(token)
                ?: throw WebException(0x101, "Can't get the uniqueId while destroying")

        session.data["uniqueId"] = uniqueId

        return uniqueId
    }

    /**
     * Constructs an response which redirects the user to the [url]
     */
    protected fun redirectToUrl(url: String, keepPost: Boolean = false): Response {
        return Response(if (!keepPost) Status.SEE_OTHER else Status.TEMPORARY_REDIRECT)
                .header("Location", url)
    }

    /**
     * Constructs an response which redirects the user to a [path] on this web page
     */
    protected fun redirectToPath(path: String): Response {
        return redirectToUrl("$redirect/$path")
    }

    protected fun htmlConstruct(body: String, head: String = ""): Response {
        //language=HTML
        return Response(Status.OK)
                .body("""
                    <!DOCTYPE HTML>
                    <html>
                    <head>
                       <meta charset='UTF-8'>
                       <title>Teamspeak Bot</title>
                       <style>
                        body {
                            font-family: monospace;
                            line-height: 1.3;
                        }
                       </style>
                       $head
                    </head>
                    <body>
                       $body
                    </body>
                    """.trimIndent())
                .header("Content-Type", "text/html")
    }

    /**
     * Constructs an response which contains
     */
    protected fun textOK(text: String): Response {
        return htmlConstruct("<p>$text</p>")
    }


}