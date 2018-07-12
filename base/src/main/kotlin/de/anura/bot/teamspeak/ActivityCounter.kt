package de.anura.bot.teamspeak

import de.anura.bot.async.Scheduler
import java.time.Duration
import java.util.concurrent.TimeUnit

object ActivityCounter {

    private val maxIdleTime = Duration.ofMinutes(5)
    private const val delay = 30L
    private val ts = TsBot.api

    init {
        Scheduler.scheduleAtFixedRate({ run() }, delay, delay, TimeUnit.SECONDS)
    }

    private fun run() {
        ts.clients.stream()
                .filter { client -> Duration.ofMillis(client.idleTime) < maxIdleTime }
                .forEach { client -> TimeManager.add(client.uniqueIdentifier, Duration.ofSeconds(delay)) }
        TimeManager.saveAll(false)
    }
}