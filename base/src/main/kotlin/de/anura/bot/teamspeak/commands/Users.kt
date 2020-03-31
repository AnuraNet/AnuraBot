package de.anura.bot.teamspeak.commands

import com.github.theholywaffle.teamspeak3.TS3Api
import de.anura.bot.database.Database
import de.anura.bot.teamspeak.TsBot
import de.anura.bot.teamspeak.UserInfo
import org.jdbi.v3.core.kotlin.withHandleUnchecked

@CommandName("users")
@CommandHelp("Manage the users of your Teamspeak")
class Users : Command() {

    private val ts: TS3Api = TsBot.api

    @CommandHelp("List of all users who connected with Steam")
    fun listConnected(userInfo: UserInfo): String {

        // Getting all users who connected their Steam account from the database
        val users = Database.get().withHandleUnchecked {
            it.select("SELECT uid, steam_id FROM ts_user WHERE steam_id IS NOT NULL")
                    .map { rs, _ -> Pair(rs.getString("uid"), rs.getLong("steam_id")) }
                    .toMap()
        }

        val userStrings = mutableListOf<String>()

        // Getting their names from the Teamspeak database & printing it
        users.forEach { (uid, steamId) ->
            val client = ts.getDatabaseClientByUId(uid)

            // Getting the user's name
            val name = if (client != null) {
                client.nickname
            } else {
                "Not in the Teamspeak database (UID: $uid)"
            }

            val link = userInfo.link(steamId.toString(), "https://steamcommunity.com/profiles/$steamId")
            userStrings.add("$name - $link")
        }

        // Returing the strings with the users
        return if (userStrings.isNotEmpty()) {
            "The following users have connected their Steam account: \n " +
                    userStrings.joinToString(separator = "\n")
        } else {
            "No users have connected their Steam acccount yet"
        }
    }

}
