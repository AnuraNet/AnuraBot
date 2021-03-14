package de.anura.bot.web

data class SteamPlayer(
    val steamid: String,
    val personaname: String,
    val profileurl: String,
    val avatar: SteamAvatar,
    val personastate: SteamPersonaState,
    val communityvisibilitystate: Boolean,
    val profilestate: Boolean,
    val lastlogoff: Int,
    val commentpermission: Boolean
)

data class SteamAvatar(
    val avatar: String,
    val avatarmedium: String,
    val avatarfull: String
)

enum class SteamPersonaState {
    OFFLINE,
    ONLINE,
    BUSY,
    AWAY,
    SNOOZE,
    LOOKING_TO_TRADE,
    LOOKING_TO_PLAYER

}

data class SteamGame(
    val appid: Int,
    val name: String
)