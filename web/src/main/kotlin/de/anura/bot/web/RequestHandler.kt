package de.anura.bot.web

import de.anura.bot.config.WebConfig
import de.anura.bot.database.Database
import de.anura.bot.teamspeak.SteamConnector
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.queries
import org.jdbi.v3.core.kotlin.withHandleUnchecked
import org.openid4java.consumer.ConsumerManager
import org.openid4java.discovery.DiscoveryInformation
import org.openid4java.message.AuthSuccess
import org.openid4java.message.MessageException
import org.openid4java.message.ParameterList

class RequestHandler(
        private val request: Request,
        private val host: String,
        private val session: SessionManager.Session,
        private val config: WebConfig
) {

    /**
     * This page saves unique id of the user to the session
     */
    fun authenticate(tokens: TokenManager): Response {
        val token = request.query("token") ?: return redirectToError(0x100)
        val uniqueId = tokens.destroyToken(token) ?: return redirectToError(0x101)

        session.data["uniqueId"] = uniqueId

        return redirect("http://$host/redirect")
    }

    /**
     * This page redirect the user to the Steam login
     */
    fun redirect(idManager: ConsumerManager, discovered: DiscoveryInformation): Response {
        val returnUrl = "http://$host/accept"
        val authRequest = idManager.authenticate(discovered, returnUrl)

        return redirect(authRequest.getDestinationUrl(true))
    }

    /**
     * This page is the return_url for the Open Id process.
     * It accepts the datas given by Steam.
     */
    fun accept(idManager: ConsumerManager, discovered: DiscoveryInformation): Response {

        // Extracting the unique id from the session
        val uniqueId = session.data["uniqueId"] ?: return redirectToError(0x300)

        // Listing the parameters and checking the result of the authentication
        val parameters = ParameterList(request.uri.queries().toMap())
        val receivingUrl = "http://" + (request.header("Host") ?: config.hostUri()) + request.uri.path

        val verification = try {
            idManager.verify(receivingUrl, parameters, discovered)
        } catch (ex: MessageException) {
            return redirectToError(0x301)
        }

        // Checking whether the authentication was successful and contains and claimed username
        verification.verifiedId ?: return redirectToError(0x302)

        // Extracting the Steam Id
        val authSuccess = verification.authResponse as AuthSuccess
        val steamid = authSuccess.claimed.replace("http://steamcommunity.com/openid/id/", "")

        // Requesting player data from Steam
        val player = SteamAPI.getPlayerSummaries(steamid) ?: return redirectToError(0x303)
        session.data["steamName"] = player.personaname
        session.data["steamUrl"] = "https://steamcommunity.com/profiles/$steamid"

        // Saving it to the database and getting the row
        val rowChanges = try {
            Database.get().withHandleUnchecked {
                it.execute(
                        "UPDATE ts_user SET steam_id = ? WHERE id = 1 AND (steam_id IS NULL OR steam_id NOT LIKE ?)",
                        steamid, uniqueId, steamid)
            }
        } catch (ex: Exception) {
            // We show the user an error to, if there was an database error
            return redirectToError(0x304)
        }

        // Only updating the games if the steam_id has changed
        if (rowChanges > 0) {
            // Updating the groups on the Teamspeak
            SteamConnector.setGroups(uniqueId)
        }

        return redirect("http://$host/success")
    }

    /**
     * A page which will be displayed if everything went fine
     */
    fun success(): Response {
        val name = session.data["steamName"] ?: return redirectToError(0x400)
        val url = session.data["steamUrl"] ?: return redirectToError(0x401)
        val uniqueId = session.data["uniqueId"] ?: return redirectToError(0x402)

        return textOK("<h3>Success</h3>" +
                "Congratulations $name!<br/>" +
                "Your <a href='$url'>Steam profile</a> is now connected with your Teamspeak Identity ($uniqueId).<br/>" +
                "You should get your game icons instant, if not just rejoin.")
    }

    /**
     * A page which will be displayed when something gone wrong.
     * The page shows also the error code.
     */
    fun error(): Response {
        val code = session.data.remove("errorCode")?.toInt() ?: 0x900

        return textOK("<h3>Something went wrong</h3>" +
                "Please use the url you'll get when you join the channel for the " +
                "Steam authentication. <br/>" +
                "If don't know what you should do contact an admin. <br/>" +
                "Error code: 0x${code.toString(16)}")
    }

    /**
     * This page should the user see if he accidentally browses to our url
     */
    fun mainPage(): Response {
        return textOK("<h3>Oh Sorry</h3>" +
                "But I think you came here the wrong way. <br/>" +
                "If you feel lost just visit our <a href='${config.externalUrl}'>website</a>.")
    }

    // Responses

    /**
     * Constructs an response which contains
     */
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

    /**
     * Constructs an response which redirects the user to the [url]
     */
    private fun redirect(url: String): Response {
        return Response(Status.TEMPORARY_REDIRECT)
                .header("Location", url)
    }

    /**
     * Constructs an response which redirects the user to an error page.
     * The error [code] will be shown on this page.
     */
    private fun redirectToError(code: Int): Response {
        session.data["errorCode"] = code.toString()

        return redirect("http://$host/error")
    }

}