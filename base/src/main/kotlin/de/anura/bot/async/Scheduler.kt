package de.anura.bot.async

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object Scheduler {

    private val factory: ReportingFactory = ReportingFactory("Scheduler")
    private val service: ScheduledExecutorService = Executors.newScheduledThreadPool(2, factory)

    fun scheduleAtFixedRate(runnable: () -> Unit, initialDelay: Long, period: Long, timeUnit: TimeUnit) {
        service.scheduleAtFixedRate(transformRunnable(runnable), initialDelay, period, timeUnit)
    }

    fun execute(runnable: () -> Unit) {
        service.execute(transformRunnable(runnable))
    }

    private fun transformRunnable(runnable: () -> Unit): () -> Unit {
        return {
            try {
                runnable.invoke()
            } catch (ex: Throwable) {
                factory.reportError(Thread.currentThread().name, ex)
            }
        }
    }

    fun stop() {
        service.shutdown()
        service.awaitTermination(20, TimeUnit.SECONDS)
        service.shutdownNow()
    }
}