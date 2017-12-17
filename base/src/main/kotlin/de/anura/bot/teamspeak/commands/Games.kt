package de.anura.bot.teamspeak.commands

import de.anura.bot.teamspeak.SteamConnector
import de.anura.bot.teamspeak.TsBot

@CommandName("games")
@CommandHelp("Edit the icons for Steam games")
class Games : Command() {

    private val steam = SteamConnector
    private val ts = TsBot.api

    @CommandHelp("Associates a Steam game with a Teamspeak icon")
    fun add(gameId: Int, iconId: Int): String {
        val anyIcon = ts.iconList.any { icon -> icon.iconId == iconId.toLong() }

        if (!anyIcon) {
            return "There's no icon with the icon id $iconId"
        }

        steam.addIcon(gameId, iconId)
        return "Associated the game $gameId with the $iconId"
    }

    @CommandHelp("Lists all associations")
    fun list(): String {

        val all = SteamConnector.list()
                .map { "${it.key}  - ${it.value}" }
                .joinToString { "\n" }

        return "The following associations were found: \nFormat: Steam Game Id - Teamspeak Icon Id \n$all"
    }

    @CommandHelp("Removes a association")
    fun remove(gameId: Int): String {
        val removed = SteamConnector.removeIcon(gameId)

        return if (removed)
            "The association of the game $gameId were removed"
        else
            "No association was found for the $gameId, so noting was removed"
    }
}