package de.anura.bot.teamspeak.commands

import de.anura.bot.teamspeak.Permissions
import de.anura.bot.teamspeak.TsBot

@CommandName("perms")
@CommandHelp("Permissions for these commands")
class Perms : Command() {

    private val perms = Permissions
    private val ts = TsBot.api

    @CommandHelp("Allow a user to interact with the bot")
    fun add(uniqueId: String): String {
        val client = ts.getDatabaseClientByUId(uniqueId)
                ?: return "A user with the unique id $uniqueId never visted this Teamspeak!"

        perms.add(uniqueId)
        return "Given the permission to ${client.nickname}"
    }

    @CommandHelp("List all users with the permission")
    fun list(): String {
        val clients = ts.databaseClients
                .filter { perms.has(it.uniqueIdentifier) }
                .joinToString(separator = "\n") { "${it.nickname} - ${it.uniqueIdentifier}" }

        return "All users with permission: \n$clients"
    }

    @CommandHelp("Disallow a user to interact with the bot")
    fun remove(uniqueId: String): String {

        perms.remove(uniqueId)
        val client = ts.getDatabaseClientByUId(uniqueId)

        return if (client != null)
            "Revoked the permission from $client"
        else
            "Revoked the permission from a user without a name"
    }

}