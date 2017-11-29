package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.TS3Api
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent

class EventListener(private val api: TS3Api) : TS3EventAdapter() {

    override fun onTextMessage(e: TextMessageEvent?) {
        if (e == null) return

        api.sendPrivateMessage(e.invokerId, "Hey")
    }

    override fun onClientJoin(e: ClientJoinEvent?) {
        if (e == null) return
        // todo update game icons
        TimeManager.load(e.uniqueClientIdentifier)
    }

    override fun onClientLeave(e: ClientLeaveEvent?) {
        if (e == null) return

        TimeManager.save(e.invokerUniqueId, true) // todo test this
    }
}