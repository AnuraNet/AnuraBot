package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.TS3Api
import com.github.theholywaffle.teamspeak3.api.event.*
import com.github.theholywaffle.teamspeak3.api.wrapper.Client
import de.anura.bot.teamspeak.commands.CommandHandler

class EventListener(private val bot: TsBot, private val api: TS3Api) : TS3EventAdapter() {

    private val steam = SteamConnector
    private val commands = CommandHandler()
    private val clients = mutableMapOf<Int, TeamspeakClient>()

    fun populateCache(client: Client) {
        clients.put(client.id, TeamspeakClient(client.id, client.uniqueIdentifier, client.channelId))
    }

    fun clearCache() {
        clients.clear()
    }

    override fun onTextMessage(ev: TextMessageEvent) {
        val result = commands.handle(ev.message)
        println(ev)
        api.sendPrivateMessage(ev.invokerId, result) // todo check invoker
    }

    override fun onClientMoved(ev: ClientMovedEvent) {

        val client = clients[ev.clientId] ?: return

        val oldChannel = client.channelId
        val newChannel = ev.targetChannelId
        client.channelId = ev.targetChannelId // todo check whether we need to reinsert this

        if (newChannel == bot.queryChannel) {
            api.sendPrivateMessage(ev.clientId, "Hey") // todo change message content
            api.moveClient(ev.clientId, oldChannel)
        }

    }


    override fun onClientJoin(ev: ClientJoinEvent) {
        clients[ev.clientId] = TeamspeakClient(ev.clientId, ev.uniqueClientIdentifier, 0) // todo check channel

        steam.setGroups(ev)
        TimeManager.load(ev.uniqueClientIdentifier)
    }

    override fun onClientLeave(ev: ClientLeaveEvent) {
        val client = clients.remove(ev.clientId)

        if (client != null) {
            TimeManager.save(client.uniqueId, true)
        }
    }

    data class TeamspeakClient(var clientId: Int, val uniqueId: String, var channelId: Int)
}