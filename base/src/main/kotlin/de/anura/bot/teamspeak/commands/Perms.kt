package de.anura.bot.teamspeak.commands

import de.anura.bot.teamspeak.Permissions
import de.anura.bot.teamspeak.TsBot

@CommandName("perms")
@CommandHelp("Manage permissions for this command interface")
class Perms : Command() {

    private val perms = Permissions
    private val ts = TsBot.api

    @CommandHelp("Allows a user to change the bots settings")
    fun add(uniqueId: String): String {
        if (perms.has(uniqueId)) {
            return "The user has already permissions"
        }

        val client = ts.getDatabaseClientByUId(uniqueId)
            ?: return "A user with the unique id $uniqueId never visited this Teamspeak!"

        perms.add(uniqueId)
        return "Given permission to ${client.nickname}"
    }

    @CommandHelp("List all users with the permission")
    fun list(): String {
        val clients = ts.databaseClients
            .filter { perms.has(it.uniqueIdentifier) }
            .joinToString(separator = "\n") { "${it.nickname} - ${it.uniqueIdentifier}" }

        return "All users with permission: \n$clients"
    }

    @CommandHelp("Takes away a users right to edit the settings")
    fun remove(uniqueId: String): String {

        perms.remove(uniqueId)
        val client = ts.getDatabaseClientByUId(uniqueId)

        return if (client != null)
            "Revoked the permission from ${client.nickname}"
        else
            "Revoked the permission from a user without a name"
    }

}
