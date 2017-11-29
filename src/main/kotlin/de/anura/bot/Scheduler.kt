package de.anura.bot

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object Scheduler {

    val service: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

    fun stop() {
        service.shutdown()
        service.awaitTermination(20, TimeUnit.SECONDS)
        service.shutdownNow()
    }
}