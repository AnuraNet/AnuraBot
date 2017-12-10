package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.TS3Api
import com.github.theholywaffle.teamspeak3.api.event.*
import com.github.theholywaffle.teamspeak3.api.wrapper.Client

class EventListener(private val api: TS3Api) : TS3EventAdapter() {

    private val steam = SteamConnector(api)
    // Teamspeak Client ID <> Teamspeak Unique ID
    private val clientUids = mutableMapOf<Int, String>()

    init {
        api.clients.forEach { populateCache(it) }
    }

    fun populateCache(client: Client) {
        clientUids.put(client.id, client.uniqueIdentifier)
    }

    fun clearCache() {
        clientUids.clear()
    }

    override fun onTextMessage(ev: TextMessageEvent) {
        api.sendPrivateMessage(ev.invokerId, "Hey")
        // todo add command interface
    }

    override fun onClientMoved(ev: ClientMovedEvent) {

        api.sendPrivateMessage(ev.clientId, "${ev.clientId}")
        // todo when the client joins the Steam channel, send the client a message

    }

    override fun onClientJoin(ev: ClientJoinEvent) {
        clientUids[ev.clientId] = ev.uniqueClientIdentifier

        steam.setGroups(ev)
        TimeManager.load(ev.uniqueClientIdentifier)
    }

    override fun onClientLeave(ev: ClientLeaveEvent) {
        val uniqueId = clientUids.remove(ev.clientId)

        if (uniqueId != null) {
            TimeManager.save(uniqueId, true)
        }
    }
}