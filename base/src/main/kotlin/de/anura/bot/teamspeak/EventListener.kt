package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.TS3Query
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode
import com.github.theholywaffle.teamspeak3.api.event.*
import com.github.theholywaffle.teamspeak3.api.wrapper.Client
import de.anura.bot.async.Scheduler
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
        clients[client.id] = TeamspeakClient(client.id, client.uniqueIdentifier, client.channelId,
                findTsVersion(client.version))
    }

    fun clearCache() {
        clients.clear()
    }

    private fun anyClientUid(uniqueId: String): Boolean {
        return clients.values.any { client -> client.uniqueId == uniqueId }
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


            val userInfo = clients[ev.invokerId]?.toUserInfo()
            if (userInfo == null) {
                asyncApi.sendPrivateMessage(ev.invokerId,
                        "There's no information about you. Please wait 5 seconds and try again.")
                return
            }

            // Catching exceptions during the command execution
            val result = try {
                commands.handle(message, userInfo)
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
            val loginUrl = WebServiceLoader.service.getLoginUrl(client.uniqueId)
            val gamesUrl = WebServiceLoader.service.getSelectGamesUrl(client.uniqueId)

            val info = client.toUserInfo()
            val message = if (SteamConnector.isConnected(client.uniqueId)) {
                """
                Hey,
                you're ${info.bold("already connected")} with a Steam account.

                To ${info.bold("connect")} your Teamspeak identity with a
                new Steam account click on ${info.link("this link", loginUrl)} and follow the instructions.

                To ${info.bold("change the game icon")} which are shown on Teamspeak
                click on ${info.link("this link", gamesUrl)} and follow the instructions.

                To ${info.bold("disconnect")} your Steam account
                send this bot a message with the content ${info.bold("disconnect")}.
                """.trimIndent()
            } else {
                """
                Hey,
                to get ${info.bold("icons for your Steam games")} you have to connect your Teamspeak account with Steam.
                Just ${info.bold("click on " + info.link("this link", loginUrl))} and follow the instructions.
                """.trimIndent()
            }

            asyncApi.sendPrivateMessage(ev.clientId, message)

            // Moving the client back to its old channel
            asyncApi.moveClient(ev.clientId, oldChannel)
        }

    }

    override fun onClientJoin(ev: ClientJoinEvent) {
        asyncApi.getClientInfo(ev.clientId).onSuccess { info ->

            clients[ev.clientId] = TeamspeakClient(ev.clientId, ev.uniqueClientIdentifier, info.channelId,
                    findTsVersion(info.version))

            steam.setGroups(ev)
            TimeManager.load(ev.uniqueClientIdentifier)
            TimeGroups.check(ev)

        }.onFailure { ex ->
            logger.warn("Couldn't get client info on join for {}", ev.clientId, ex)
        }
    }

    override fun onClientLeave(ev: ClientLeaveEvent) {
        val client = clients.remove(ev.clientId) ?: return

        if (anyClientUid(client.uniqueId)) {
            // We just save the data, but don't delete it from the cache,
            // because another user with the same uid is online.
            // When we would delete it and add time (the next time), the time
            // would start from 0 and so an invalid would be later saved to the database.
            Scheduler.execute { TimeManager.save(client.uniqueId, false) }
            logger.info("Only soft saving the time of {}, because he/she was with multiple accounts online",
                    client.uniqueId)
        } else {
            // We save the data of the user (& delete it), because no other user with the same uid is online
            Scheduler.execute { TimeManager.save(client.uniqueId, true) }
        }

    }

    private fun findTsVersion(version: String): Int {
        // The client_version property has the format of "3.5.1", "5.0.0-beta.24" or "ServerQuery"
        return when {
            version.startsWith("3") -> 3
            version.startsWith("5") -> 5
            else -> 1
        }
    }

    data class TeamspeakClient(var clientId: Int, val uniqueId: String, var channelId: Int, val tsVersion: Int) {
        fun isServerQuery(): Boolean {
            return tsVersion == 1;
        }

        fun isTeamspeak5(): Boolean {
            return tsVersion == 5;
        }

        fun toUserInfo(): UserInfo {
            return UserInfo(isTeamspeak5());
        }

    }
}
