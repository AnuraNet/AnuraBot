package de.anura.bot.teamspeak.commands

@CommandName("perms")
@CommandHelp("Permissions for these commands")
class Perms : Command() {

    @CommandHelp("Allow a user to interact with the bot")
    fun add(): String {
        return ""
    }


    @CommandHelp("Disallow a user to interact with the bot")
    fun remove(): String {
        return ""
    }

}