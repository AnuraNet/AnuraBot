package de.anura.bot.teamspeak.commands

import de.anura.bot.teamspeak.SteamConnector
import de.anura.bot.teamspeak.TsBot

@CommandName("games")
@CommandHelp("Edit the icons for Steam games")
class Games : Command() {

    private val steam = SteamConnector
    private val ts = TsBot.api

    @CommandHelp("Associates a Steam game with a Teamspeak group")
    fun add(gameId: Int, groupId: Int): String {
        val anyGroup = ts.serverGroups.any { it.id == groupId }

        if (!anyGroup) {
            return "There's no group with the id $groupId"
        }

        steam.addIcon(gameId, groupId)
        return "Associated the game $gameId with the $groupId"
    }

    @CommandHelp("Lists all associations")
    fun list(): String {
        val all = SteamConnector.list().entries
                .joinToString(separator = "\n") {
                    "[url=https://steamdb.info/app/${it.key}/]${it.key}[/url]  - ${it.value}"
                }

        return "The following associations were found: \nFormat: Steam Game Id - Teamspeak Icon Id \n$all"
    }

    @CommandHelp("Removes a association")
    fun remove(gameId: Int): String {
        val removed = SteamConnector.removeIcon(gameId, true)

        return if (removed)
            "The association of the game $gameId was removed. All users were kicked out of this group."
        else
            "No association was found for the $gameId, so noting was removed"
    }
}