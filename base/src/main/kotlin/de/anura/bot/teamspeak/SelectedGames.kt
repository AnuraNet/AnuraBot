package de.anura.bot.teamspeak

import de.anura.bot.config.AppConfig
import de.anura.bot.database.Database
import de.anura.bot.web.SteamAPI
import de.anura.bot.web.SteamException
import de.anura.bot.web.SteamGame
import org.jdbi.v3.core.kotlin.useHandleUnchecked
import org.jdbi.v3.core.kotlin.withHandleUnchecked
import org.slf4j.LoggerFactory

object SelectedGames {

    private val logger = LoggerFactory.getLogger(SelectedGames::class.java)

    /**
     * Queries the database for the games which a user has selected.
     * @return A list of the steam appids of the selected games
     */
    fun querySelectedGames(uniqueId: String): List<Int> {
        return Database.get().withHandleUnchecked {
            it.select("SELECT game_id FROM selected_game " +
                    "WHERE user_id = (SELECT id FROM ts_user WHERE uid = ?)",
                    uniqueId
            ).mapTo(Int::class.java).list()
        }
    }

    /**
     * Returns all games a user with the [steamId] owns on Steam
     */
    private fun getOwnedGames(steamId: String): List<SteamGame> {
        return try {
            SteamAPI.getOwnedGames(steamId)
        } catch (ex: SteamException) {
            logger.warn("Couldn't get Steam games for {}", steamId)
            emptyList()
        }
    }

    /**
     * Queries the database for the games which a user has selected.
     * And checks whether this selection is still valid.
     * If not it's updated and saved to the database.
     *
     * @return A list of [SteamGame] objects which the user wants to be shown
     */
    fun queryAndUpdateGames(uniqueId: String, steamId: String): List<SteamGame> {
        // Getting the owned games via the Steam API
        val ownedGames = getOwnedGames(steamId)
        val ownedIds = ownedGames.map { game -> game.appid }

        val baseSelected = querySelectedGames(uniqueId)
        val updatedSelected = if (baseSelected.isNotEmpty()) {
            // The game must be still present in the user's steam account, maybe he changed the connected account
            val owned = baseSelected.filter { game -> ownedIds.contains(game) }
            shortenSelection(owned)
        } else {
            // The user selected no games, so we don't display a game
            baseSelected
        }

        if (baseSelected != updatedSelected) {
            saveSelectedGames(uniqueId, updatedSelected)
        }

        return ownedGames
                .filter { game -> updatedSelected.contains(game.appid) }
    }

    /**
     * Removes the last part of the selection if there are more selected games than are allowed.
     *
     * @return A list of [SteamGame] which is equal to or shorter than the maximum number of games.
     */
    private fun shortenSelection(games: List<Int>): List<Int> {
        if (games.size <= AppConfig.web.maxSteamGroups) {
            return games
        }

        // If the number of maxSteamGroups is reduced, the selection of the user also shrinks
        return games.subList(0, AppConfig.web.maxSteamGroups)
    }

    /**
     * Sets a number of games as default and saves them to the database.
     * The number of default games is equal to or smaller than the limit.
     */
    fun setDefaultGames(uniqueId: String, steamId: String) {
        // Getting the owned games via the Steam API
        val ownedGames = getOwnedGames(steamId)
        val ownedIds = ownedGames.map { game -> game.appid }

        val iconsAvailable = SteamConnector.list()
        val gamesAvailable = iconsAvailable.keys.filter { gameId -> ownedIds.contains(gameId) }

        // If the user has selected too many groups, we cut his groups down to the limit
        val diffCount = gamesAvailable.size - AppConfig.web.maxSteamGroups
        val selectedGames = if (diffCount > 0) {
            gamesAvailable.dropLast(diffCount)
        } else {
            gamesAvailable
        }

        saveSelectedGames(uniqueId, selectedGames)
    }

    /**
     * Saves the selection of the user to the database.
     * To clear the selection just pass [emptyList] as [games]
     */
    fun saveSelectedGames(uniqueId: String, games: List<Int>) {
        Database.get().useHandleUnchecked {
            val optionalId = it.select("SELECT id FROM ts_user WHERE uid = ?", uniqueId)
                    .mapTo(Int::class.java)
                    .findFirst()

            if (!optionalId.isPresent) {
                logger.warn("Can't save the selected games for user {}, because he's got no id", uniqueId)
                return@useHandleUnchecked
            }

            val userId = optionalId.get()
            it.execute("DELETE FROM selected_game WHERE user_id = ?", userId)

            if (games.isEmpty()) {
                return@useHandleUnchecked
            }

            val batch = it.prepareBatch("INSERT INTO selected_game (user_id, game_id) VALUES (?, ?)")
            games.forEach { gameId -> batch.add(userId, gameId) }
            if (batch.size() > 0) {
                batch.execute()
            }
            batch.close()
        }
    }

}