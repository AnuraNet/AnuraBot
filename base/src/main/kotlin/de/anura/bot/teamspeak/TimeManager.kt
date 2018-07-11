package de.anura.bot.teamspeak

import de.anura.bot.Scheduler
import de.anura.bot.database.Database
import org.jdbi.v3.core.kotlin.useHandleUnchecked
import org.jdbi.v3.core.kotlin.withHandleUnchecked
import java.time.Duration
import java.util.concurrent.TimeUnit

object TimeManager {

    private val clientTime: HashMap<String, Duration> = HashMap()
    private val listeners: HashSet<TimeListener> = HashSet()

    init {
        Scheduler.service.scheduleWithFixedDelay({ saveAll(false) }, 15, 15, TimeUnit.MINUTES)
        ActivityCounter
    }

    private fun select(uid: String): Duration? {
        val result = Database.get().withHandleUnchecked {
            it.select("SELECT time FROM ts_user WHERE uid = ?", uid)
                    .map { rs, _ -> Duration.ofSeconds(rs.getLong(1)) }
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
        clientTime[uid] = result ?: Duration.ZERO
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
    fun get(uid: String, db: Boolean = false): Duration {
        return when {
            clientTime.contains(uid) -> clientTime[uid] ?: Duration.ZERO
            db -> select(uid) ?: Duration.ZERO
            else -> Duration.ZERO
        }
    }

    /**
     * Adds [time] to the user ([uid]).
     *
     * @param uid The Teamspeak Uid of the user
     * @param time Time to add in seconds
     */
    fun add(uid: String, time: Duration) {
        val before = get(uid)
        val after = before + time

        clientTime[uid] = after
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

typealias TimeListener = (uid: String, old: Duration, new: Duration) -> Unit
