package de.anura.bot.web.handlers

import de.anura.bot.web.AbstractRequestHandler
import de.anura.bot.web.NettyWebService
import de.anura.bot.web.WebException
import org.http4k.core.Response

class DefaultRequestHandler(requestInfo: NettyWebService.RequestInfo) : AbstractRequestHandler(requestInfo) {

    /**
     * A page which will be displayed when something gone wrong.
     * The page shows also the error code.
     */
    fun error(): Response {
        val code = session.data.remove("errorCode")?.toInt() ?: 0x900

        return textOK(
            "<h3>Something went wrong</h3>" +
                    "Please use the url you'll get when you join the channel for the " +
                    "Steam authentication. <br/>" +
                    "If don't know what you should do contact an admin. <br/>" +
                    "Error code: 0x${code.toString(16)}"
        )
    }

    /**
     * Constructs an response which redirects the user to an error page.
     * The error code of [ex] will be shown on this page.
     */
    fun handleError(ex: WebException): Response {
        session.data["errorCode"] = ex.code.toString()
        return redirectToPath("error")
    }

    /**
     * This page should the user see if he accidentally browses to our url
     */
    fun mainPage(): Response {
        return textOK(
            "<h3>Oh Sorry</h3>" +
                    "But I think you came here the wrong way. <br/>" +
                    "If you feel lost just visit our <a href='${config.externalUrl}'>website</a>."
        )
    }

}