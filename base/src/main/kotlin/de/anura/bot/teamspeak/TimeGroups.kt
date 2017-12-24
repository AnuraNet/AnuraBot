package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent
import de.anura.bot.database.Database
import org.jdbi.v3.core.kotlin.useHandleUnchecked
import org.jdbi.v3.core.kotlin.withHandleUnchecked

object TimeGroups {

    private val database = Database.get()
    // Teamspeak Group Id <> Time Group
    private val groups = mutableMapOf<Int, TimeGroup>()
    private val ts = TsBot.api

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
        // Filtering for groups which time is greater than the old time and less than the new time
        // As a Java expression it would look like this: old < group.time && group.time < new
        // Returing if there's no change in groups
        val group = groups.filterValues { it.time in (old + 1)..(new - 1) }[0] ?: return

        // Getting the group which had the user before the new group
        val oldGroup = groups.values
                .filter { it.time < group.time }
                .minBy { group.time - it.time }

        val databaseId = ts.getClientByUId(uid).databaseId

        // Adding the new group to the user
        ts.addClientToServerGroup(group.tsGroup, databaseId)
        // Removing the old server group
        if (oldGroup != null) ts.removeClientFromServerGroup(oldGroup.tsGroup, databaseId)
    }

    /**
     * Checks the time groups of a user when he joins.
     * Invalid groups will be removed
     */
    fun check(ev: ClientJoinEvent) {
        // Getting the client groups from the string
        val clientGroups = ev.clientServerGroups.split(",").mapNotNull { it.toIntOrNull() }
        // Filtering out the time groups of the user
        val timeGroups = clientGroups.filter { groups.containsKey(it) }

        // Getting the correct teamspeak group for the online time of the user
        val time = TimeManager.get(ev.uniqueClientIdentifier)
        // Selecting with smalltest positive time difference => Searching for the correct group
        val correctGroup = groups.values
                .filter { time - it.time > 0 }
                .minBy { time - it.time } ?: return

        // Removing the user from the wrong groups
        timeGroups
                .filterNot { it == correctGroup.tsGroup }
                .forEach { ts.removeClientFromServerGroup(it, ev.clientDatabaseId) }

        // Adding the correct to the user if he hasn't got it
        if (!timeGroups.contains(correctGroup.tsGroup)) {
            ts.addClientToServerGroup(correctGroup.tsGroup, ev.clientDatabaseId)
        }
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