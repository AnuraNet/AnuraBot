package de.anura.bot.web.handlers

import de.anura.bot.database.Database
import de.anura.bot.teamspeak.SteamConnector
import de.anura.bot.web.AbstractRequestHandler
import de.anura.bot.web.NettyWebService.RequestInfo
import de.anura.bot.web.SteamAPI
import de.anura.bot.web.WebException
import org.http4k.core.Response
import org.http4k.core.queries
import org.jdbi.v3.core.kotlin.withHandleUnchecked
import org.openid4java.consumer.ConsumerManager
import org.openid4java.discovery.DiscoveryInformation
import org.openid4java.message.AuthSuccess
import org.openid4java.message.MessageException
import org.openid4java.message.ParameterList

class ConnectRequestHandler(requestInfo: RequestInfo) : AbstractRequestHandler(requestInfo) {

    /**
     * This page saves unique id of the user to the session
     */
    fun authenticate(): Response {
        verifyToken()
        return redirectToPath("redirectToSteam")
    }

    /**
     * This page redirectToUrl the user to the Steam login
     */
    fun redirect(idManager: ConsumerManager, discovered: DiscoveryInformation): Response {
        val returnUrl = "$redirect/accept"
        val authRequest = idManager.authenticate(discovered, returnUrl)

        return redirectToUrl(authRequest.getDestinationUrl(true))
    }

    /**
     * This page is the return_url for the Open Id process.
     * It accepts the datas given by Steam.
     */
    fun accept(idManager: ConsumerManager, discovered: DiscoveryInformation): Response {

        // Extracting the unique id from the session
        val uniqueId = session.getData("uniqueId", 0x300)

        // Listing the parameters and checking the result of the authentication
        val parameters = ParameterList(request.uri.queries().toMap())
        val receivingUrl = redirect + request.uri.path

        val verification = try {
            idManager.verify(receivingUrl, parameters, discovered)
        } catch (ex: MessageException) {
            throw WebException(0x301, ex)
        }

        // Checking whether the authentication was successful and contains and claimed username
        verification.verifiedId ?: throw WebException(0x302, "No verified id was present")

        // Extracting the Steam Id
        val authSuccess = verification.authResponse as AuthSuccess
        val steamid = authSuccess.claimed.replace("https://steamcommunity.com/openid/id/", "", true)

        // Requesting player data from Steam
        val player = SteamAPI.getPlayerSummaries(steamid)
                ?: throw WebException(0x303, "The SteamAPI doesn't show more information about the user $steamid")
        session.data["steamName"] = player.personaname
        session.data["steamUrl"] = "https://steamcommunity.com/profiles/$steamid"
        // Updating the steam id from the session, because it have changed
        // The steam id is used by the SelectRequestHandler
        session.data["steamId"] = steamid

        // Saving it to the database and getting the row
        val rowChanges = try {
            Database.get().withHandleUnchecked {
                it.execute(
                        "UPDATE ts_user SET steam_id = ? WHERE uid = ? AND (steam_id IS NULL OR steam_id NOT LIKE ?)",
                        steamid, uniqueId, steamid)
            }
        } catch (ex: Exception) {
            // We show the user an error too, if there was an database error
            throw WebException(0x304, "Can't save the steam id to the database")
        }

        // Only updating the games if the steam_id has changed
        if (rowChanges > 0) {
            // Updating the groups on the Teamspeak
            SteamConnector.setGroups(uniqueId)
        }

        return redirectToPath("success")
    }

    /**
     * A page which will be displayed if everything went fine
     */
    fun success(): Response {
        val name = session.getData("steamName", 0x400)
        val url = session.getData("steamUrl", 0x401)
        val uniqueId = session.getData("uniqueId", 0x402)

        return textOK("<h3>Success</h3>" +
                "Congratulations $name!<br/><br/>" +
                "Your <a href='$url'>Steam profile</a> is now connected with your Teamspeak Identity ($uniqueId).<br/>" +
                "<a href='/selectgames'>Select now which games should be shown</a> as Teamspeak groups.")
    }

}