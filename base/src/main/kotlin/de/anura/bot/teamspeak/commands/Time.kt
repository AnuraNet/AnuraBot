package de.anura.bot.teamspeak.commands

import de.anura.bot.teamspeak.TimeManager
import de.anura.bot.teamspeak.TsBot
import de.anura.bot.teamspeak.UserInfo
import java.math.BigDecimal
import java.math.RoundingMode

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
            val clientUrl = userInfo.clientLink(0, client.uniqueIdentifier, client.nickname)
            "$clientUrl has been online for $time hours"
        } else {
            "Nickname of $uniqueId wasn't found. He/She was $time hours online"
        }
    }

}
