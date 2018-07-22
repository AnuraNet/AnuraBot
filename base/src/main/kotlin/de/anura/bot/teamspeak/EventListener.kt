package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.TS3Query
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode
import com.github.theholywaffle.teamspeak3.api.event.*
import com.github.theholywaffle.teamspeak3.api.wrapper.Client
import de.anura.bot.teamspeak.commands.CommandHandler
import de.anura.bot.web.WebServiceLoader
import org.slf4j.LoggerFactory

class EventListener(private val bot: TsBot, query: TS3Query) : TS3EventAdapter() {

    private val asyncApi = query.asyncApi
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val steam = SteamConnector
    private val commands = CommandHandler()
    private val clients = mutableMapOf<Int, TeamspeakClient>()

    fun populateCache(client: Client) {
        clients[client.id] = TeamspeakClient(client.id, client.uniqueIdentifier, client.channelId)
    }

    fun clearCache() {
        clients.clear()
    }

    override fun onTextMessage(ev: TextMessageEvent) {
        // We only handle direct messages to the bot as commands
        if (ev.targetMode == TextMessageTargetMode.CLIENT && ev.getInt("target") == bot.queryClientId) {

            val message = ev.message.trim()

            // Let the user disconnect its Steam connection
            if (message.equals("disconnect", true)) {
                val disconnected = SteamConnector.disconnect(ev.invokerUniqueId)

                val answerMessage = if (disconnected) {
                    "Your Steam account has been disconnected"
                } else {
                    "A Steam account isn't connected to this Teamspeak identity"
                }
                asyncApi.sendPrivateMessage(ev.invokerId, answerMessage)

                return
            }

            if (!Permissions.has(ev.invokerUniqueId)) {
                asyncApi.sendPrivateMessage(ev.invokerId, "You don't have the permission to use commands!")
                return
            }

            // Catching exceptions during the command execution
            val result = try {
                commands.handle(message)
            } catch (ex: Exception) {
                logger.warn("There was an error while executing the command '{}'", message, ex)
                asyncApi.sendPrivateMessage(ev.invokerId, "There was an error while running your command ):")
                return
            }
            // Sending the result to the user
            asyncApi.sendPrivateMessage(ev.invokerId, result)
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

            val message = if (SteamConnector.isConnected(client.uniqueId)) {
                """
                Hey,
                you're [b]already conncted[/b] with a Steam account.

                To [b]connect[/b] your Teamspeak identity with a
                new Steam account click on [URL=$url]this link[/URL] and follow the instructions.

                To [b]disconnect[/b] your Steam account
                send this bot a message with the content [b]disconnect[/b].
                """.trimIndent()
            } else {
                """
                Hey,
                to get [b]icons for your Steam games[/b] you have to connect your Teamspeak account with Steam.
                Just [b]click on [URL=$url]this link[/URL][/b] and follow the instructions.
                """.trimIndent()
            }

            asyncApi.sendPrivateMessage(ev.clientId, message)

            // Moving the client back to its old channel
            asyncApi.moveClient(ev.clientId, oldChannel)
        }

    }

    override fun onClientJoin(ev: ClientJoinEvent) {
        asyncApi.getClientInfo(ev.clientId).onSuccess { info ->

            clients[ev.clientId] = TeamspeakClient(ev.clientId, ev.uniqueClientIdentifier, info.channelId)

            steam.setGroups(ev)
            TimeManager.load(ev.uniqueClientIdentifier)
            TimeGroups.check(ev)

        }.onFailure { ex ->
            logger.warn("Couldn't get client info on join for {}", ev.clientId, ex)
        }
    }

    override fun onClientLeave(ev: ClientLeaveEvent) {
        val client = clients.remove(ev.clientId) ?: return

        asyncApi.getClientByUId(client.uniqueId).onSuccess { _ ->
            // We just save the data, but don't delete it from the cache,
            // because another user with the same uid is online.
            // When we would delete it and add time (the next time), the time
            // would start from 0 and so an invalid would be later saved to the database.
            TimeManager.save(client.uniqueId, false)
            logger.info("Only soft saving the time of {}, because he/she was with multiple accounts online",
                    client.uniqueId)
        }.onFailure { _ ->
            // We save the data of the user (& delete it), because no other user with the same uid is online
            TimeManager.save(client.uniqueId, true)
        }

    }

    data class TeamspeakClient(var clientId: Int, val uniqueId: String, var channelId: Int)
}