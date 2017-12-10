package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.TS3Api
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent
import de.anura.bot.database.Database
import de.anura.bot.web.SteamAPI
import de.anura.bot.web.SteamException
import org.jdbi.v3.core.kotlin.useHandleUnchecked
import org.jdbi.v3.core.kotlin.withHandleUnchecked

class SteamConnector(private val ts: TS3Api) {

    // Steam Game Id <> Teamspeak Icon Id
    private val icons = mutableMapOf<Int, Int>()
    // Teamspeak UID <> Timestamp
    private val delay = mutableMapOf<String, Long>()
    // If the user join more than once in 5 minutes we don't check his games
    private val delayTime = 5

    init {
        loadIcons()
    }

    /**
     * Loads all game icon association from the database
     */
    private fun loadIcons() {
        Database.get().withHandleUnchecked {
            it.select("SELECT * FROM steam_game")
                    .map { rs, _ -> Pair(rs.getInt("id"), rs.getInt("icon_id")) }
                    .stream()
                    .forEach { pair -> icons.put(pair.first, pair.second) }
        }
    }

    /**
     * Adds an game icon association to the cache and the database
     */
    fun addIcon(gameId: Int, iconId: Int) {
        icons.put(gameId, iconId)
        Database.get().useHandleUnchecked {
            it.execute("INSERT INTO steam_game (`id`, icon_id) VALUES (?, ?)", gameId, iconId)
        }
    }

    /**
     * Returns whether the game of the user should be checked
     */
    private fun hasDelay(uid: String): Boolean {
        // The timestamp of the last check may not be grater than the allowed time
        val allowedTime = System.currentTimeMillis() - delayTime * 60 * 1000
        // The timestamp of the last check
        val lastTime = delay.getOrDefault(uid, 0)

        return lastTime > allowedTime
    }

    /**
     * Checks whether the user of [ev] has all icons for his games
     */
    fun setGroups(ev: ClientJoinEvent) {
        val uid = ev.uniqueClientIdentifier
        // If the user joined too often, we skip the check
        if (hasDelay(uid)) return

        // Getting the Steam id of the user from the database. If it isn't set we don't continue
        val steamId = Database.get().withHandleUnchecked { handle ->
            handle.select("SELECT steam_id FROM ts_user WHERE steam_id IS NOT NULL AND uid = ?", uid)
                    .mapTo(String::class.java)
                    .findFirst()
        }
        if (!steamId.isPresent) return

        // Getting the owned games via the Steam API
        val games = try {
            SteamAPI.getOwnedGames(steamId.get())
        } catch (ex: SteamException) {
            return
        }

        // Adding the missing groups for games to the user
        val groups = ts.getClientByUId(ev.uniqueClientIdentifier).serverGroups
        games.mapNotNull { icons[it.appid] }
                // We don't try add existing groups
                .filter { !groups.contains(it) }
                .forEach { ts.addClientToServerGroup(it, ev.clientDatabaseId) }

        // Saving the current time for a delay
        delay.put(uid, System.currentTimeMillis())
    }

}