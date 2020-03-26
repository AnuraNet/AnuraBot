package de.anura.bot.teamspeak.commands

import de.anura.bot.teamspeak.TimeManager
import de.anura.bot.teamspeak.TsBot
import de.anura.bot.teamspeak.UserInfo
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder

@CommandName("time")
@CommandHelp("Shows how long users were on this Teamspeak")
class Time : Command() {

    private val ts = TsBot.api

    @CommandHelp("Shows how long the user was active on this Teamspeak")
    fun show(uniqueId: String, userInfo: UserInfo): String {
        // Converting from seconds to hours and rounding it to 2 decimals
        val time = BigDecimal.valueOf(TimeManager.get(uniqueId).seconds / 60.0 / 60.0).setScale(2, RoundingMode.HALF_UP)
        // User info from the database
        val client = ts.getDatabaseClientByUId(uniqueId)

        return if (client != null) {
            val clientUrl = buildClientUrl(0, client.uniqueIdentifier, client.nickname, userInfo)

            "$clientUrl has been online for $time hours"
        } else {
            "Nickname of $uniqueId wasn't found. He/She was $time hours online"
        }
    }

    /**
     * Builds a BBCode which makes the name of client clickable
     *
     * @param clientId - The client id of the user or if he's offline 0
     * @param uniqueId - The unique id of the user
     * @param name - The name of the user with which the link should be displayed
     */
    private fun buildClientUrl(clientId: Int, uniqueId: String, name: String, userInfo: UserInfo): String {
        // Encoding only the name
        val encodedName = URLEncoder.encode(name, "UTF-8")
        // Building the BBCode for the href to the client
        // Read more here: http://yat.qa/ressourcen/definitionen-und-algorithmen/#int-links
        // TODO: Test this url in teamspeak 5, I don't if it works
        return userInfo.link(name, "client://$clientId/$uniqueId~$encodedName");
    }

}
