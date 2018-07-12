package de.anura.bot.async

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class ReportingFactory(private val name: String) : ThreadFactory {

    private val logger: Logger = LoggerFactory.getLogger(ReportingFactory::class.java)
    private val group: ThreadGroup = ThreadGroup("$name-Group")
    private val counter: AtomicInteger = AtomicInteger(0)

    override fun newThread(runnable: Runnable?): Thread {
        if (runnable == null) {
            throw IllegalArgumentException("The runnable as an parameter for the newThread method can't be null")
        }

        val id = counter.incrementAndGet()
        val threadName = "$name-$id"

        val thread = Thread(group, runnable, threadName)
        thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, ex ->
            reportError(threadName, ex)
        }

        return thread
    }

    fun reportError(name: String, ex: Throwable) {
        logger.error("Error in thread '{}': {}", name, ex.message, ex)
    }

}