package de.anura.bot.teamspeak

import de.anura.bot.database.Database
import org.jdbi.v3.core.kotlin.useHandleUnchecked
import org.jdbi.v3.core.kotlin.withHandleUnchecked

object TimeGroups {

    private val database = Database.get()
    // Teamspeak Group Id <> Time Group
    private val groups = mutableMapOf<Int, TimeGroup>()

    init {
        load()
    }

    private fun load() {
        // Loading from the database
        database.withHandleUnchecked {
            it.select("SELECT * FROM time_group")
                    .map { rs, _ -> TimeGroup(rs.getInt("ts_group"), rs.getLong("required_time")) }
                    .list()
        }.forEach { groups[it.tsGroup] = it }
        // Adding the listener
        TimeManager.addListener(::listen)
    }

    /**
     * The listener function for the TimeManager
     */
    private fun listen(uid: String, old: Long, new: Long) {
        // todo also check on join
    }

    /**
     * Adds a group to the cache and the database and returns it with the id
     *
     * @throws IllegalStateException If there's already a group with the same [time]
     */
    fun add(tsGroup: Int, time: Long): TimeGroup {
        // Checking for other groups with the same time
        if (groups.any { it.value.time == time }) {
            throw IllegalStateException("There's already a group with this time")
        }

        // Inserting it into the cache
        val group = TimeGroup(tsGroup, time)
        groups[group.tsGroup] = group

        // And inserting it into the database
        database.useHandleUnchecked {
            it.execute("INSERT INTO time_group (ts_group, required_time) VALUES (?, ?)", tsGroup, time)
        }

        return group
    }

    /**
     * Gets a list of all groups
     */
    fun list(): List<TimeGroup> {
        return groups.values.toList()
    }

    /**
     * Removes the group with the [id] from the cache and the database
     */
    fun remove(id: Int): Boolean {
        // Removing from the cache
        val remove = groups.remove(id)
        // Removing it from the database
        database.useHandleUnchecked { it.execute("DELETE FROM time_group WHERE id = ?", id) }
        // Returns whether something was deleted from the cache
        return remove != null
    }

    data class TimeGroup(val tsGroup: Int, val time: Long)

}