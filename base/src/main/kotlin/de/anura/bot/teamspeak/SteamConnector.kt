package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent
import de.anura.bot.database.Database
import de.anura.bot.web.SteamAPI
import de.anura.bot.web.SteamException
import org.jdbi.v3.core.kotlin.useHandleUnchecked
import org.jdbi.v3.core.kotlin.withHandleUnchecked
import org.slf4j.LoggerFactory
import java.time.Instant

object SteamConnector {

    // If the user join more than once in 2 minutes we don't check his games again
    private const val delayTime = 2

    private val ts = TsBot.api
    // Steam Game Id <> Teamspeak Group Id
    private val icons = mutableMapOf<Int, Int>()
    // Teamspeak UID <> Instant
    private val delay = mutableMapOf<String, Instant>()
    private val logger = LoggerFactory.getLogger(SteamConnector.javaClass)

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
                    .forEach { pair -> icons[pair.first] = pair.second }
        }
    }

    /**
     * Adds an game icon association to the cache and the database
     */
    fun addIcon(gameId: Int, iconId: Int) {
        icons[gameId] = iconId
        Database.get().useHandleUnchecked {
            it.execute("INSERT INTO steam_game (`id`, icon_id) VALUES (?, ?)", gameId, iconId)
        }
        // Updating the Steam groups of all online clients
        setAllClientsGroups()
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
        // If the user has permissions there should be no delay
        // The timestamp of the last check. If it isn't set there's no delay
        if (Permissions.has(uid)) return false

        val lastTime = delay[uid] ?: return false
        // The last time plus the delay
        val allowedTime = lastTime.plusSeconds((delayTime * 60).toLong())

        return allowedTime.isAfter(Instant.now())
    }

    /**
     * Updates all games icons for the user with the [ev]
     */
    fun setGroups(ev: ClientJoinEvent) {
        val uniqueId = ev.uniqueClientIdentifier
        val databaseId = ev.clientDatabaseId

        // If the user joined too often, we skip the check
        if (hasDelay(uniqueId)) {
            logger.info("Not setting groups for ${ev.clientNickname}, because of the delay")
            return
        }

        // Setting the groups
        setGroups(uniqueId, databaseId)

        // Saving the current time for a delay
        delay[uniqueId] = Instant.now()
    }

    /**
     * Updates all games icons for the user with the [uniqueId]
     */
    fun setGroups(uniqueId: String) {
        // Searching for the client, but if he isn't online, we can't set groups
        val client = ts.getClientByUId(uniqueId) ?: return

        // Setting the groups
        setGroups(uniqueId, client.databaseId)
    }

    private fun setGroups(uniqueId: String, databaseId: Int) {
        // Getting the Steam id of the user from the database. If it isn't set we don't continue
        val steamId = Database.get().withHandleUnchecked { handle ->
            handle.select("SELECT steam_id FROM ts_user WHERE steam_id IS NOT NULL AND uid = ?", uniqueId)
                    .mapTo(String::class.java)
                    .findFirst()
        }

        val games = if (steamId.isPresent) {
            // Getting the owned games via the Steam API
            try {
                SteamAPI.getOwnedGames(steamId.get())
            } catch (ex: SteamException) {
                return
            }
        } else {
            // Returning an empty list of games
            listOf()
        }

        // All groups the user should have
        val correctGroups = games.mapNotNull { icons[it.appid] }
        // All groups (include non-game) groups the user have
        val serverGroups = ts.getClientByUId(uniqueId).serverGroups

        // D! This is just a debug message
        val mappedGames = games.joinToString(separator = ", ") { it.appid.toString() }
        val mappedGroups = correctGroups.joinToString(separator = ", ") { it.toString() }
        logger.info("Games of $uniqueId: $mappedGames\nCorrect Groups: $mappedGroups")
        // D! End of the debug message part

        // Adding the missing groups for games to the user
        correctGroups
                .filter { !serverGroups.contains(it) }
                .forEach { ts.addClientToServerGroup(it, databaseId) }

        // Removing the invalid groups from the user
        serverGroups
                .filter { icons.containsValue(it) }
                .filter { !correctGroups.contains(it) }
                .forEach { ts.removeClientFromServerGroup(it, databaseId) }
    }

    fun setAllClientsGroups() {
        ts.clients.forEach { client -> setGroups(client.uniqueIdentifier, client.databaseId) }
    }

    fun isConnected(uniqueId: String): Boolean {
        val steamId = Database.get().withHandleUnchecked {
            it.createQuery("SELECT steam_id FROM ts_user WHERE uid = ?")
                    .bind(0, uniqueId)
                    .mapTo(String::class.java)
                    .findFirst()
        }
        return steamId.isPresent
    }

    /**
     * Disconnects a [uniqueId] from its Steam account.
     * Returns whether it was connected with a Steam account.
     */
    fun disconnect(uniqueId: String): Boolean {
        val rows = Database.get().withHandleUnchecked {
            it.execute("UPDATE ts_user SET steam_id = NULL WHERE uid = ? AND (steam_id IS NOT NULL)", uniqueId)
        }

        if (rows == 0) {
            // Nothing changed
            return false
        }

        setGroups(uniqueId)

        // Disconnects a user and returns whether
        return true
    }

}