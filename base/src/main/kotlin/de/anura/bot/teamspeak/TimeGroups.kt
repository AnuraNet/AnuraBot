package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent
import de.anura.bot.database.Database
import org.jdbi.v3.core.kotlin.useHandleUnchecked
import org.jdbi.v3.core.kotlin.withHandleUnchecked
import java.time.Duration

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
                    .map { rs, _ -> TimeGroup(rs.getInt("ts_group"), Duration.ofSeconds(rs.getLong("required_time"))) }
                    .list()
        }.forEach { groups[it.tsGroup] = it }
        // Adding the listener
        TimeManager.addListener(::listen)
    }

    /**
     * The listener function for the TimeManager
     */
    private fun listen(uid: String, old: Duration, new: Duration) {
        // We don't add groups to the serveradmin account
        if (uid.equals("serveradmin", true)) {
            return
        }

        // Filtering for groups which time is greater than the old time and less than the new time
        // As a Java expression it would look like this: old < group.time && group.time < new
        // Returing if there's no change in groups
        val group = groups.values.firstOrNull { it.time in old..new } ?: return

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
        val uniqueId = ev.uniqueClientIdentifier
        val databaseId = ev.clientDatabaseId

        // Getting the client groups from the string
        val clientGroups = ev.clientServerGroups.split(",").mapNotNull { it.toIntOrNull() }

        check(uniqueId, databaseId, clientGroups)
    }

    /**
     * Checks the time groups of a user when he joins.
     * Invalid groups will be removed
     */
    private fun check(uniqueId: String, databaseId: Int, clientGroups: List<Int>) {
        // We don't add groups to the serveradmin account
        if (uniqueId.equals("serveradmin", true)) {
            return
        }

        // Filtering out the time groups of the user
        val timeGroups = clientGroups.filter { groups.containsKey(it) }

        // Getting the correct teamspeak group for the online time of the user
        val time = TimeManager.get(uniqueId)
        // Selecting with smalltest positive time difference => Searching for the correct group
        val correctGroup = groups.values
                .filter { !(time - it.time).isNegative }
                .minBy { time - it.time } ?: return

        // Removing the user from the wrong groups
        timeGroups
                .filterNot { it == correctGroup.tsGroup }
                .forEach { ts.removeClientFromServerGroup(it, databaseId) }

        // Adding the correct to the user if he hasn't got it
        if (!timeGroups.contains(correctGroup.tsGroup)) {
            ts.addClientToServerGroup(correctGroup.tsGroup, databaseId)
        }
    }

    fun checkAllClients() {
        ts.clients.forEach { client -> check(client.uniqueIdentifier, client.databaseId, client.serverGroups.asList()) }
    }

    /**
     * Adds a group to the cache and the database and returns it with the id
     *
     * @throws IllegalStateException If there's already a group with the same [time]
     */
    fun add(tsGroup: Int, time: Duration, addClients: Boolean): TimeGroup {
        // Checking for other groups with the same time
        if (groups.any { it.value.time == time }) {
            throw IllegalStateException("There's already a group with this time")
        }

        // Inserting it into the cache
        val group = TimeGroup(tsGroup, time)
        groups[group.tsGroup] = group

        // And inserting it into the database
        database.useHandleUnchecked {
            it.execute("INSERT INTO time_group (ts_group, required_time) VALUES (?, ?)", tsGroup, time.seconds)
        }

        if (addClients) {
            // Getting the next group with a little higher time requirement
            val nextGroupTime: Duration = groups.values
                    .filter { it.time > time }
                    .minBy { it.time.minus(time) }
                    ?.time ?: Duration.ofSeconds(Long.MAX_VALUE)

            // Adding all online clients with enough and not too much time to this group
            ts.clients
                    // We don't add groups to the serveradmin account
                    .filter { client -> !client.uniqueIdentifier.equals("serveradmin", true) }
                    .filter { client -> TimeManager.get(client.uniqueIdentifier) in time..nextGroupTime }
                    .forEach { client ->
                        check(client.uniqueIdentifier, client.databaseId, client.serverGroups.asList())
                    }
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
     *
     * @param removeClients Whether all clients in this group should be removed from it
     */
    fun remove(id: Int, removeClients: Boolean): Boolean {
        // Removing from the cache
        val removed = groups.remove(id)
        // Removing it from the database
        database.useHandleUnchecked { it.execute("DELETE FROM time_group WHERE ts_group = ?", id) }

        if (removed != null && removeClients) {
            // Removes all clients from this group
            val clients = ts.getServerGroupClients(removed.tsGroup)
            clients.forEach { ts.removeClientFromServerGroup(removed.tsGroup, it.clientDatabaseId) }
        }

        // Returns whether something was deleted from the cache
        return removed != null
    }

    data class TimeGroup(val tsGroup: Int, val time: Duration)

}