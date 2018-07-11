package de.anura.bot.teamspeak

import de.anura.bot.Scheduler
import java.time.Duration
import java.util.concurrent.TimeUnit

object ActivityCounter {

    private const val maxIdleTime = 5 * 60 * 1000
    private const val delay = 30L
    private val ts = TsBot.api

    init {
        Scheduler.service.scheduleAtFixedRate({ run() }, delay, delay, TimeUnit.SECONDS)
    }

    private fun run() {
        ts.clients.stream()
                .filter { client -> client.idleTime < maxIdleTime }
                .forEach { client -> TimeManager.add(client.uniqueIdentifier, Duration.ofSeconds(delay)) }
    }
}