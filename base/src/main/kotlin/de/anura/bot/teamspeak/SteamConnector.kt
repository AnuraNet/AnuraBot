package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent
import de.anura.bot.database.Database
import de.anura.bot.web.SteamAPI
import de.anura.bot.web.SteamException
import org.jdbi.v3.core.kotlin.useHandleUnchecked
import org.jdbi.v3.core.kotlin.withHandleUnchecked

object SteamConnector {

    // The API for the Teamspeak server query
    private val ts = TsBot.api
    // Steam Game Id <> Teamspeak Group Id
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
     * Lists all associations
     */
    fun list(): Map<Int, Int> {
        return icons.toMap()
    }

    /**
     * Removes an game icon association from the cache and the database
     *
     * @param removeClients Whether all clients in this group should be removed from it
     *
     * @return Whether a association was removed
     */
    fun removeIcon(gameId: Int, removeClients: Boolean): Boolean {
        val removed = icons.remove(gameId)

        Database.get().useHandleUnchecked {
            it.execute("DELETE FROM steam_game WHERE `id` = ?", gameId)
        }

        if (removed != null && removeClients) {
            // Removing all clients from the group
            val clients = ts.getServerGroupClients(removed)
            clients.forEach { ts.removeClientFromServerGroup(removed, it.clientDatabaseId) }
        }

        return removed != null
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

        // All groups the user should have
        val correctGroups = games.mapNotNull { icons[it.appid] }
        // All groups (include non-game) groups the user have
        val serverGroups = ts.getClientByUId(ev.uniqueClientIdentifier).serverGroups

        // Adding the missing groups for games to the user
        correctGroups
                .filter { !serverGroups.contains(it) }
                .forEach { ts.addClientToServerGroup(it, ev.clientDatabaseId) }

        // Removing the invalid groups from the user
        serverGroups
                .filter { icons.containsValue(it) }
                .filter { !correctGroups.contains(it) }
                .forEach { ts.removeClientFromServerGroup(it, ev.clientDatabaseId) }

        // Saving the current time for a delay
        delay.put(uid, System.currentTimeMillis())
    }

}