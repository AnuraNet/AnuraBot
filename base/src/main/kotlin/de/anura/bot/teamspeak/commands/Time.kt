package de.anura.bot.teamspeak.commands

@CommandName("time")
@CommandHelp("Shows how long users were on this Teamspeak")
class Time : Command() {

    @CommandHelp("Shows how long the user was active on this Teamspeak")
    fun show(uniqueId: String): String {
        return ""
    }

}
