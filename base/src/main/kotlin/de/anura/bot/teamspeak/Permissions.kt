package de.anura.bot.teamspeak

import de.anura.bot.database.Database
import org.jdbi.v3.core.kotlin.useHandleUnchecked
import org.jdbi.v3.core.kotlin.withHandleUnchecked

object Permissions {

    // Teamspeak Unique Ids
    private val allowed = mutableListOf<String>()
    private val database = Database.get()

    init {
        load()
    }

    /**
     * Loads all users with permissions from the database
     */
    private fun load() {
        database.withHandleUnchecked {
            it.select("SELECT uid FROM ts_user WHERE permission = 1")
                .mapTo(String::class.java)
                .list()
        }.forEach { allowed.add(it) }
    }

    /**
     * Gives the Teamspeak user with the [uniqueId] the permission to use commands
     */
    fun add(uniqueId: String) {
        allowed.add(uniqueId)
        databaseUpdate(uniqueId, true)
    }

    /**
     * Checks whether [uniqueId] has permission to use the commands
     */
    fun has(uniqueId: String): Boolean {
        return allowed.contains(uniqueId)
    }

    /**
     * Returns a list of a users with permissions
     */
    fun all(): List<String> {
        return allowed.toList()
    }

    /**
     * Revokes the permission for the user with [uniqueId]
     */
    fun remove(uniqueId: String) {
        allowed.remove(uniqueId)
        databaseUpdate(uniqueId, false)
    }

    /**
     * Saves the changes to the database
     */
    private fun databaseUpdate(uniqueId: String, permission: Boolean) {
        val permBit = if (permission) 1 else 0

        database.useHandleUnchecked {
            it.execute("UPDATE ts_user SET permission = ? WHERE uid = ?", permBit, uniqueId)
        }
    }
}