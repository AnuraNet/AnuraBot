package de.anura.bot.teamspeak

import de.anura.bot.Scheduler
import de.anura.bot.database.Database
import org.jdbi.v3.core.kotlin.useHandleUnchecked
import org.jdbi.v3.core.kotlin.withHandleUnchecked
import java.util.concurrent.TimeUnit

object TimeManager {

    private val clientTime: HashMap<String, Long> = HashMap()
    private val listeners: HashSet<TimeListener> = HashSet()

    init {
        Scheduler.service.scheduleWithFixedDelay({ saveAll(false) }, 15, 15, TimeUnit.MINUTES)
        ActivityCounter
    }

    private fun select(uid: String): Long? {
        val result = Database.get().withHandleUnchecked {
            it.select("SELECT time FROM ts_user WHERE uid = ?", uid)
                    .mapTo(Long::class.java)
                    .findFirst()
        }
        return result.orElse(null)
    }

    private fun insert(uid: String) {
        Database.get().useHandleUnchecked {
            it.execute("INSERT INTO ts_user (uid, `time`) VALUES (?, ?)", uid, 0)
        }
    }

    fun load(uid: String) {
        // Selecting from the database
        val result = select(uid)
        // If it doesn't exists there, we'll insert into
        if (result == null) insert(uid)
        // Adding it to the cache
        clientTime[uid] = result ?: 0
    }

    /**
     * Gets the time (in seconds) how long the Teamspeak with
     * the UID [uid] has been on this server.
     *
     * @param uid The Teamspeak Unique Id of the user
     * @param db Whether the database should be queried if nothing is loaded
     *
     * @return The time [uid] has been on this server in seconds
     */
    fun get(uid: String, db: Boolean = false): Long {
        return when {
            clientTime.contains(uid) -> clientTime[uid] ?: 0
            db -> select(uid) ?: 0
            else -> 0
        }
    }

    /**
     * Adds [time] to the user ([uid]).
     *
     * @param uid The Teamspeak Uid of the user
     * @param time Time to add in seconds
     */
    fun add(uid: String, time: Long) {
        val before = get(uid)
        val after = before + time

        clientTime.put(uid, after)
        listeners.forEach { it(uid, before, after) }
    }

    fun save(uid: String, remove: Boolean) {

        if (!clientTime.contains(uid)) return

        val time = clientTime[uid]

        Database.get().useHandleUnchecked {
            it.execute("UPDATE ts_user SET time = ? WHERE uid = ?", time, uid)
        }

        if (remove) clientTime.remove(uid)
    }

    fun saveAll(remove: Boolean) {
        clientTime.keys.forEach { uid -> save(uid, false) }
        if (remove) clientTime.clear()
    }

    /**
     * This listener will be called after the time for the
     * teamspeak user with the uid (1.) has been changed from
     * the old (2.) value to the new (3.) value.
     *
     */
    fun addListener(listener: TimeListener) {
        listeners.add(listener)
    }

}

typealias TimeListener = (uid: String, old: Long, new: Long) -> Unit
