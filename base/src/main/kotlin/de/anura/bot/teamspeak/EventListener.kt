package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.TS3Api
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode
import com.github.theholywaffle.teamspeak3.api.event.*
import com.github.theholywaffle.teamspeak3.api.wrapper.Client
import de.anura.bot.teamspeak.commands.CommandHandler
import de.anura.bot.web.WebServiceLoader
import org.slf4j.LoggerFactory

class EventListener(private val bot: TsBot, private val api: TS3Api) : TS3EventAdapter() {

    private val logger = LoggerFactory.getLogger(this::class.java)
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
        // We only handle direct messages to the bot as commands
        if (ev.targetMode == TextMessageTargetMode.CLIENT && ev.getInt("target") == bot.queryClientId) {

            if (!Permissions.has(ev.invokerUniqueId)) {
                api.sendPrivateMessage(ev.invokerId, "You don't have the permission to use commands!")
                return
            }

            val message = ev.message.trim()

            // Catching exceptions during the command execution
            val result = try {
                commands.handle(message)
            } catch (ex: Exception) {
                logger.warn("There was an error while executing the command '{}'", message, ex)
                api.sendPrivateMessage(ev.invokerId, "There was an error while running your command ):")
                return
            }
            // Sending the result to the user
            api.sendPrivateMessage(ev.invokerId, result)
        }
    }

    override fun onClientMoved(ev: ClientMovedEvent) {

        val client = clients[ev.clientId] ?: return

        val oldChannel = client.channelId
        val newChannel = ev.targetChannelId
        // Updating the channel id of the client
        client.channelId = ev.targetChannelId

        if (newChannel == bot.queryChannel) {
            // The client joined the channel defined in the configuration
            val url = WebServiceLoader.service.getLoginUrl(client.uniqueId)

            val message = """
                Hey,
                to get [b]icons for your Steam games[/b] you have to connect your Teamspeak account with Steam.
                Just [b]click on [URL=$url]this link[/URL][/b] and follow the instructions.
                """.trimIndent()

            api.sendPrivateMessage(ev.clientId, message)

            // Moving the client back to its old channel
            api.moveClient(ev.clientId, oldChannel)
        }

    }

    override fun onClientJoin(ev: ClientJoinEvent) {
        val client = api.getClientInfo(ev.clientId)
        clients[ev.clientId] = TeamspeakClient(ev.clientId, ev.uniqueClientIdentifier, client.channelId)

        steam.setGroups(ev)
        TimeManager.load(ev.uniqueClientIdentifier)
        TimeGroups.check(ev)
    }

    override fun onClientLeave(ev: ClientLeaveEvent) {
        val client = clients.remove(ev.clientId)

        if (client != null) {
            TimeManager.save(client.uniqueId, true)
        }
    }

    data class TeamspeakClient(var clientId: Int, val uniqueId: String, var channelId: Int)
}