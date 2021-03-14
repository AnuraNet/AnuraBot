package de.anura.bot.web

import org.json.JSONException
import org.json.JSONObject

/**
 * @see <a href="https://steamcommunity.com/dev">https://steamcommunity.com/dev</a>
 */
object SteamAPI {
    private var key = ""
    private const val base = "http://api.steampowered.com"

    /**
     * Gets basic profile information for the player
     *
     * @param steamid The 64-bit Steam Id of the player
     *
     * @see <a href="https://developer.valvesoftware.com/wiki/Steam_Web_API#GetPlayerSummaries_.28v0002.29">
     *     https://developer.valvesoftware.com/wiki/Steam_Web_API#GetPlayerSummaries_.28v0002.29</a>
     *
     * @throws SteamException When something goes wrong during the request to Steam
     * @throws IllegalArgumentException When no Steam API key is set
     *
     * @return The Steam player or if he doesn't null
     */
    fun getPlayerSummaries(steamid: String): SteamPlayer? {
        val url = "ISteamUser/GetPlayerSummaries/v0002/?steamids=$steamid"
        val json = requestSteam(url)
        val player = json.optJSONObject("response")?.optJSONArray("players")?.optJSONObject(0) ?: return null

        return SteamPlayer(
            player.getString("steamid"),
            player.getString("personaname"),
            player.getString("profileurl"),
            SteamAvatar(
                player.getString("avatar"),
                player.getString("avatarmedium"),
                player.getString("avatarfull")
            ),
            SteamPersonaState.values()[player.getInt("personastate")],
            player.getInt("communityvisibilitystate") == 3,
            player.has("profilestate"),
            player.getInt("lastlogoff"),
            player.has("commentpermission")
        )
    }

    /**
     * Gets a list of games the player owns
     *
     * @param steamid The 64-bit Steam Id of the player
     *
     * @see <a href="https://developer.valvesoftware.com/wiki/Steam_Web_API#GetOwnedGames_.28v0001.29">
     *     https://developer.valvesoftware.com/wiki/Steam_Web_API#GetOwnedGames_.28v0001.29</a>
     *
     * @throws SteamException When something goes wrong during the request to Steam
     * @throws IllegalArgumentException When no Steam API key is set
     */
    fun getOwnedGames(steamid: String, appinfo: Boolean = true): List<SteamGame> {
        val appinfoUrl = if (appinfo) 1 else 0

        val url = "IPlayerService/GetOwnedGames/v0001/?steamid=$steamid&include_appinfo=$appinfoUrl"
        val json = requestSteam(url)
        val gamesJson = json.optJSONObject("response")?.optJSONArray("games") ?: return emptyList()

        val games = mutableListOf<SteamGame>()

        gamesJson.forEach { gameJson ->
            if (gameJson !is JSONObject) return@forEach
            val game = SteamGame(
                gameJson.getInt("appid"),
                gameJson.getString("name")
            )
            games.add(game)
        }

        return games
    }

    /**
     * Checks whether the API key works. If this is sucessful the key will be stored and used for all requests.
     *
     * @throws SteamException When something goes wrong during the request to Steam (includes an wrong API key)
     */
    fun setKey(key: String) {
        // Steam API Request for the game Dota 2. We're testing whether the API key works
        val testUrl = "ISteamUserStats/GetSchemaForGame/v2/?appid=570"
        requestSteam(testUrl, key)
        this.key = key
    }

    /**
     * Sends a request to the Steam API.
     *
     * @param url The realtive url to the [base] url
     * @param key The api key that should be used
     *
     * @throws SteamHttpException When there's an error while requesting the url via http
     * @throws SteamJsonException When the answer from Steam couldn't be parsed to a JSONObject
     * @throws IllegalArgumentException When no api key is set
     */
    private fun requestSteam(url: String, key: String = getKey()): JSONObject {
        val fullUrl = "$base/$url&key=$key"

        val (status, text) = HttpClient.request(fullUrl, "GET")

        if (status != 200) {
            throw SteamHttpException(url, status)
        }

        return try {
            JSONObject(text)
        } catch (ex: JSONException) {
            throw SteamJsonException(ex)
        }
    }

    private fun getKey(): String {
        if (key.isEmpty()) throw IllegalArgumentException("No Steam API key set!")
        return key
    }

}
