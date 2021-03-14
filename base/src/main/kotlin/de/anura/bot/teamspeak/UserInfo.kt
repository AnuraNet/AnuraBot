package de.anura.bot.teamspeak

import com.github.theholywaffle.teamspeak3.api.PermissionGroupType
import java.net.URLEncoder

class UserInfo(private val ts5: Boolean, private val databaseId: Int) {

    // Teamspeak 3 format: https://www.teamspeak-info.de/ts_bb_codes.htm
    // Teamspeak 5 format: https://ts-n.net/index.php?artid=97

    /**
     * Creates bold text
     */
    fun bold(text: String): String {
        return when {
            ts5 -> "**$text**"
            else -> "[b]$text[/b]"
        }
    }

    /**
     * Creates italic text
     */
    fun italic(text: String): String {
        return when {
            ts5 -> "__${text}__"
            else -> "[i]$text[/i]"
        }
    }

    /**
     * Formats the [text] as code, this only works in teamspeak 5.
     * For users using teamspeak 3 this will just be text.
     */
    fun code(text: String): String {
        return when {
            ts5 -> "`$text`"
            else -> text
        }
    }

    /**
     * Creates a link which links to [url] and shows the [text].
     */
    fun link(text: String, url: String): String {
        return if (ts5) {
            "[$text]($url)"
        } else {
            "[url=$url]$text[/url]"
        }
    }

    /**
     * Creates a clickable chat link to the client if the user is using teamspeak 3,
     * otherwise it's just the name.
     *
     * @param clientId - The client id of the user or if he's offline 0
     * @param uniqueId - The unique id of the user
     * @param name - The name of the user with which the link should be displayed
     * @see <a href="http://yat.qa/ressourcen/definitionen-und-algorithmen/#int-links">yat.qa</a>
     *
     */
    fun clientLink(clientId: Int, uniqueId: String, name: String): String {
        if (ts5) {
            return name
        }

        // Encoding only the name
        val encodedName = URLEncoder.encode(name, "UTF-8")
        return link(name, "client://$clientId/$uniqueId~$encodedName")
    }

    /**
     * Runs [canAddGroup] and sends messages if it's not allowed
     */
    fun canAddGroupWithMessages(groupId: Int, allowed: () -> String): String {
        return when (canAddGroup(groupId)) {
            AddGroupPermission.NO_PERMISSION ->
                "There's an error, the id for the permission 'i_group_member_add_power' couldn't be found."
            AddGroupPermission.NO_GROUP ->
                "There's no teamspeak server group with the id $groupId"
            AddGroupPermission.FORBIDDEN ->
                "Your 'i_group_member_add_power' permission is lower than the groups 'i_group_needed_member_add_power'.\n" +
                        "You aren't allowed to add users to this group."
            AddGroupPermission.ALLOWED ->
                allowed.invoke()
        }
    }

    /**
     * Checks whether this user has teamspeak permissions to add users to the group [groupId]
     */
    private fun canAddGroup(groupId: Int): AddGroupPermission {
        val permAddPower = "i_group_member_add_power"
        val permAddNeeded = "i_group_needed_member_add_power"
        val tsApi = TsBot.api

        val permAddPowerId = tsApi.permissions.find { it.name == permAddPower }?.id
            ?: return AddGroupPermission.NO_PERMISSION

        if (!tsApi.serverGroups.any { it.id == groupId }) {
            return AddGroupPermission.NO_GROUP
        }

        val serverGroup = tsApi.getServerGroupPermissions(groupId)
        val permission = serverGroup.find { it.name == permAddNeeded }
        val minimumPower = permission?.value ?: 0

        // Getting the permission of the user and we don't care about the channel
        val userPermissions = tsApi.getPermissionOverview(0, databaseId) ?: return AddGroupPermission.FORBIDDEN
        val userAddPower = userPermissions.find {
            it.type == PermissionGroupType.SERVER_GROUP && it.id == permAddPowerId
        }?.value ?: return AddGroupPermission.FORBIDDEN

        return if (userAddPower >= minimumPower) {
            AddGroupPermission.ALLOWED
        } else {
            AddGroupPermission.FORBIDDEN
        }
    }

    enum class AddGroupPermission {
        NO_GROUP,
        NO_PERMISSION,
        FORBIDDEN,
        ALLOWED
    }
}
