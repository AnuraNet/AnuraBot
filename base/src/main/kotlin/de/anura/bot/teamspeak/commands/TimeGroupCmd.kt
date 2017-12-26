package de.anura.bot.teamspeak.commands

import de.anura.bot.teamspeak.TimeGroups
import de.anura.bot.teamspeak.TsBot
import java.math.BigDecimal
import java.math.RoundingMode

@CommandName("timegroup")
@CommandHelp("Manage groups users get for their online time")
class TimeGroupCmd : Command() {

    private val ts = TsBot.api
    private val groups = TimeGroups

    @CommandHelp("Adds a new time group. [Time] in seconds")
    fun add(tsGroup: Int, time: Long): String {
        try {
            groups.add(tsGroup, time)
        } catch (ex: IllegalStateException) {
            return "There's already an group with the same time!"
        }
        return "A new time group was added!"
    }

    @CommandHelp("Lists all time groups")
    fun list(): String {
        // Fetches Teamspeak Groups
        val tsGroups = ts.serverGroups.associateBy { it.id }

        // Convertes all to a String
        val list = groups.list()
                .joinToString(separator = "\n") {
                    val name = tsGroups[it.tsGroup]?.name ?: " --- "
                    val hours = BigDecimal(it.time / 60.0 / 60.0).setScale(2, RoundingMode.HALF_UP)
                    "$name (${it.tsGroup}) - $hours h"
                }

        return "Following time groups exists: \n$list"
    }

    @CommandHelp("Removes a time group")
    fun remove(groupId: Int): String {
        val removed = groups.remove(groupId, true)

        return if (removed)
            "Removed the time group and all clients from it"
        else
            "Couldn't find the time group"
    }

}