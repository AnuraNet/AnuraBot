package de.anura.bot.teamspeak

import de.anura.bot.Scheduler
import java.util.concurrent.TimeUnit

object ActivityCounter {

    private val maxIdleTime = 5 * 60
    private val delay = 60L
    private val ts = TsBot.api

    init {
        Scheduler.service.scheduleAtFixedRate({ run() }, delay, delay, TimeUnit.SECONDS)
    }

    private fun run() {
        ts.clients.stream()
                .filter { client -> client.idleTime > maxIdleTime }
                .forEach { client -> TimeManager.add(client.uniqueIdentifier, delay) }
    }
}