package de.anura.bot.teamspeak.commands

import de.anura.bot.teamspeak.SteamConnector
import de.anura.bot.teamspeak.UserInfo

@CommandName("games")
@CommandHelp("Edit the icons for Steam games")
class Games : Command() {

    private val steam = SteamConnector

    @CommandHelp("Associates a Steam game with a Teamspeak group")
    fun add(gameId: Int, groupId: Int, userInfo: UserInfo): String {
        return userInfo.canAddGroupWithMessages(groupId) {
            steam.addIcon(gameId, groupId)
            return@canAddGroupWithMessages "Associated the game $gameId with the teamspeak group $groupId"
        }
    }

    @CommandHelp("Lists all associations")
    fun list(userInfo: UserInfo): String {
        val all = SteamConnector.list().entries
            .joinToString(separator = "\n") {
                userInfo.link(it.key.toString(), "https://steamdb.info/app/${it.key}/") + " - ${it.value}"
            }

        return "The following associations were found: \nFormat: Steam Game - Teamspeak Group \n$all"
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
