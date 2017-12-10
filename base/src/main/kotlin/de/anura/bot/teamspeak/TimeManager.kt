package de.anura.bot.teamspeak

import de.anura.bot.Scheduler
import de.anura.bot.database.Database
import org.jdbi.v3.core.kotlin.useHandleUnchecked
import org.jdbi.v3.core.kotlin.withHandleUnchecked
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

object TimeManager {

    private val clientTime: HashMap<String, Long> = HashMap()
    private val listeners: HashSet<TimeChangedListener> = HashSet()

    init {
        Scheduler.service.scheduleWithFixedDelay({ saveAll(false) }, 15, 15, TimeUnit.MINUTES)
    }

    fun load(uid: String) {
        val optional = Database.get().withHandleUnchecked { handle ->
            handle.select("SELECT time FROM ts_user WHERE uid = ?")
                    .bind(0, uid)
                    .mapTo(Long::class.java)
                    .findFirst()
        }

        if (!optional.isPresent) {
            insert(uid)
        }

        clientTime[uid] = optional.orElse(0)

    }

    private fun insert(uid: String) {
        Database.get().useHandleUnchecked { handle ->
            handle.createUpdate("INSERT INTO ts_user (uid, `time`) VALUES (?, ?)")
                    .bind(0, uid)
                    .bind(1, 0)
                    .execute()
        }
    }

    fun loadReadOnly(uid: String, consumer: Consumer<Long?>) {

        if (clientTime.contains(uid)) {
            consumer.accept(clientTime[uid])
        } else {
            load(uid)
            consumer.accept(clientTime[uid])
            clientTime.remove(uid) // todo improve
        }

    }

    /**
     * Gets the time (in seconds) how long the Teamspeak with
     * the UID [uid] has been on this server.
     *
     * @return The time [uid] has been on this server in seconds
     */
    fun get(uid: String): Long {
        return when {
            clientTime.contains(uid) -> clientTime[uid] ?: 0
            else -> 0
        }
    }

    fun add(uid: String, time: Long) {
        val before = get(uid)
        val after = before + time

        clientTime.put(uid, after)
        listeners.forEach { listener -> listener.changed(uid, before, after) }
    }

    fun save(uid: String, remove: Boolean) {

        if (!clientTime.contains(uid)) return

        val time = clientTime[uid]

        Database.get().useHandleUnchecked { handle ->
            handle.createUpdate("UPDATE ts_user SET time = ? WHERE uid = ?")
                    .bind(0, time)
                    .bind(1, uid)
                    .execute()
        }

        if (remove) clientTime.remove(uid)
    }

    fun saveAll(remove: Boolean) {
        clientTime.keys.forEach { uid -> save(uid, false) }
        if (remove) clientTime.clear()
    }

    fun addListener(listener: TimeChangedListener) {
        listeners.add(listener) // todo add listener
    }

}