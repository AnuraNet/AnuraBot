package de.anura.bot.teamspeak.commands

@CommandName("games")
@CommandHelp("Edit the icons for Steam games")
class Games : Command() {

    @CommandHelp("Associates a Steam game with a Teamspeak icon")
    fun add(gameId: Int, iconId: Int): String {
        return ""
    }

    @CommandHelp("Lists all associations")
    fun list(): String {
        return ""
    }

    @CommandHelp("Removes a association")
    fun remove(gameId: Int): String {
        return ""
    }
}