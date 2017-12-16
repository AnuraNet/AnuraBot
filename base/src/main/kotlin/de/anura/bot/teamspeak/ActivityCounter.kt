package de.anura.bot.teamspeak

import de.anura.bot.Scheduler
import java.util.concurrent.TimeUnit

class ActivityCounter(private val bot: TsBot) {

    private val maxIdleTime = 5 * 60
    private val delay = 60L

    init {
        Scheduler.service.scheduleAtFixedRate({ run() }, delay, delay, TimeUnit.SECONDS)
    }


    private fun run() {
        // todo fix this
        /* val api = bot.getApi() ?: return

        api.clients.stream()
                .filter { client -> client.idleTime > maxIdleTime }
                .forEach { client -> TimeManager.add(client.uniqueIdentifier, delay) } */
    }
}