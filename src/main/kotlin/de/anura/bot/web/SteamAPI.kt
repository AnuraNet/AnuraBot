package de.anura.bot.web

/**
 * @see <a href="https://steamcommunity.com/dev">https://steamcommunity.com/dev</a>
 */
object SteamAPI {
    val key = ""
    val base = "http://api.steampowered.com"

    /**
     * @see <a href="https://developer.valvesoftware.com/wiki/Steam_Web_API#GetPlayerSummaries_.28v0002.29">
     *     https://developer.valvesoftware.com/wiki/Steam_Web_API#GetPlayerSummaries_.28v0002.29</a>
     */
    fun getPlayerSummaries(steamid: String) {
        val url = "$base/ISteamUser/GetPlayerSummaries/v0002/?key=$key&steamids=$steamid"
        // https://github.com/jkcclemens/khttp
    }

    /**
     * @see <a href="https://developer.valvesoftware.com/wiki/Steam_Web_API#GetOwnedGames_.28v0001.29">
     *     https://developer.valvesoftware.com/wiki/Steam_Web_API#GetOwnedGames_.28v0001.29</a>
     */
    fun getOwnedGames(steamid: String, appinfo: Boolean = true) {
        val url = "$base/IPlayerService/GetOwnedGames/v0001/?key=$key&steamid=$steamid&include_appinfo=$appinfo"
    }

}