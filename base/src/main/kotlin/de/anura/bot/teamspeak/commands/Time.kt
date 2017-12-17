package de.anura.bot.teamspeak.commands

import de.anura.bot.teamspeak.TimeManager
import de.anura.bot.teamspeak.TsBot

@CommandName("time")
@CommandHelp("Shows how long users were on this Teamspeak")
class Time : Command() {

    private val ts = TsBot.api

    @CommandHelp("Shows how long the user was active on this Teamspeak")
    fun show(uniqueId: String): String {
        // Converting seconds to hours
        val time = TimeManager.get(uniqueId) / 60.0 / 60.0
        // The last name of the client
        val nickname = ts.getDatabaseClientByUId(uniqueId)?.nickname

        return if (nickname != null)
            "$nickname was $time hours online"
        else
            "Nickname of $uniqueId wasn't found. He/She was $time hours online"
    }

}
