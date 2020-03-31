package de.anura.bot.teamspeak.commands

import de.anura.bot.teamspeak.TimeGroups
import de.anura.bot.teamspeak.TsBot
import de.anura.bot.teamspeak.UserInfo
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

@CommandName("timegroup")
@CommandHelp("Manage groups users get for their online time")
class TimeGroupCmd : Command() {

    private val ts = TsBot.api
    private val groups = TimeGroups

    @CommandHelp("Adds a new time group ([time] in seconds)")
    fun add(tsGroup: Int, time: Long, userInfo: UserInfo): String {
        return userInfo.canAddGroupWithMessages(tsGroup) {
            try {
                groups.add(tsGroup, Duration.ofSeconds(time), true)
            } catch (ex: IllegalStateException) {
                return@canAddGroupWithMessages "There's already an group with the same time!"
            }

            return@canAddGroupWithMessages "A new time group was added!"
        }
    }

    @CommandHelp("Lists all time groups")
    fun list(): String {
        // Fetches Teamspeak Groups
        val tsGroups = ts.serverGroups.associateBy { it.id }

        // Converts all time groups to a single string
        val list = groups.list()
                .joinToString(separator = "\n") {
                    val name = tsGroups[it.tsGroup]?.name ?: " --- "
                    val hours = BigDecimal(it.time.seconds / 60.0 / 60.0).setScale(2, RoundingMode.HALF_UP)
                    "$name (${it.tsGroup}) - $hours h"
                }

        if (list.isEmpty()) {
            return "There are no time groups registered."
        } else {
            return "Here are all registered time groups: \n$list"
        }
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
