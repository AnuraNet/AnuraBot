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
    public fun querySelectedGames(uniqueId: String): List<Int> {
        return Database.get().withHandleUnchecked {
            it.select("SELECT game_id FROM selected_game " +
                    "WHERE user_id = (SELECT id FROM ts_user WHERE uid = ?)",
                    uniqueId
            ).mapTo(Int::class.java).list()
        }
    }

    /**
     * Queries the database for the games which a user has selected.
     * And checks whether this selection is still valid.
     * If not it's updated and saved to the database.
     *
     * @return A list of [SteamGame] objects which the user wants to be shown
     */
    public fun queryAndUpdateGames(uniqueId: String, steamId: String): List<SteamGame> {
        // Getting the owned games via the Steam API
        val ownedGames = try {
            SteamAPI.getOwnedGames(steamId)
        } catch (ex: SteamException) {
            logger.warn("Couldn't get Steam games for {}", steamId)
            return emptyList()
        }
        val ownedIds = ownedGames.map { game -> game.appid }

        val baseSelected = querySelectedGames(uniqueId)
        val updatedSelected = baseSelected
                // The game must be stil present in the user's steam account, maybe he changed the connected account
                .filter { game -> ownedIds.contains(game) }
                .windowed(AppConfig.web.maxSteamGroups)
                .getOrElse(0) { emptyList() }

        if (baseSelected.size != updatedSelected.size) {
            saveSelectedGames(uniqueId, updatedSelected)
        }

        return ownedGames
                .filter { game -> updatedSelected.contains(game.appid) }
    }

    /**
     * Saves the selection of the user to the database.
     * To clear the selection just pass [emptyList] as [games]
     */
    public fun saveSelectedGames(uniqueId: String, games: List<Int>) {
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