package de.anura.bot.web.handlers

import de.anura.bot.async.Scheduler
import de.anura.bot.teamspeak.SelectedGames
import de.anura.bot.teamspeak.SteamConnector
import de.anura.bot.web.AbstractRequestHandler
import de.anura.bot.web.NettyWebService
import de.anura.bot.web.SteamAPI
import de.anura.bot.web.WebException
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.body.form
import org.intellij.lang.annotations.Language

class SelectRequestHandler(requestInfo: NettyWebService.RequestInfo) : AbstractRequestHandler(requestInfo) {

    /**
     * Routes the request to the '/selectgames' route to the right function depending on the method.
     */
    fun selectGames(): Response {
        // Routing to the right action
        return when (request.method) {
            Method.GET -> showSelectGames()
            Method.POST -> saveSelectedGames()
            else -> throw WebException(0x700, "Wrong request method ${request.method} while selecting games")
        }
    }

    /**
     * Shows a page to select the wanted games
     */
    private fun showSelectGames(): Response {
        @Language("HTML")
        val body = """
            <form method='post'>
                <fieldset>
                    <legend>
                        Choose which games should be shown as Teamspeak groups? (Max: ${config.maxSteamGroups})
                    </legend>
                    ${ownedGamesToHtml()}
                </fieldset>
                <br>
                ${appendSaveResult()}
                <button type='submit'>Save your settigns</button>
            </form>
            <script>
                window.maxGames = ${config.maxSteamGroups}
            </script>
            <script src='/assets/js/selectgames.js'></script>
        """.trimIndent()
        @Language("HTML")
        val head = """
            <style>
            fieldset {
                border: 1px #000 solid;
            }

            button {
                border-radius: 0;
                font-family: monospace;
                padding: 3px 10px;
                border: 3px #000 solid;
                background: #FFF;
            }

            button:hover {
                background: #000;
                color: #FFF;
                cursor: pointer;
            }

            input[type=checkbox] {
              vertical-align: middle;
              position: relative;
              bottom: 1px;
            }

            legend {
                background: #000;
                color: #FFF;
                padding: 3px 6px;
            }

            .box-divider {
                margin: 5px 0;
            }

            .alert {
                padding: 15px 20px;
                border: 1px #000 solid;
            }
            </style>
        """.trimIndent()
        return htmlConstruct(body, head)
    }

    private fun findSteamId(): String {
        val steamId = session.data["steamId"]
        if (steamId != null) {
            return steamId
        }

        val uniqueId: String = if (session.data.containsKey("uniqueId")) {
            session.getData("uniqueId", 0x701)
        } else {
            verifyToken()
        }

        val newSteamId = SteamConnector.getSteamId(uniqueId)
                ?: throw WebException(0x702, "The Steam api won't return the id for the user $uniqueId")

        session.data["steamId"] = newSteamId
        return newSteamId
    }

    private fun ownedGamesToHtml(): String {
        val steamId = findSteamId()
        val uniqueId = session.getData("uniqueId", 0x703)

        val availableIcons = SteamConnector.list().keys
        val selectedGames = SelectedGames.querySelectedGames(uniqueId)

        return SteamAPI.getOwnedGames(steamId)
                .filter { game -> availableIcons.contains(game.appid) }
                .sortedBy { game -> game.name }
                .joinToString(separator = "\n") { game ->

                    val elementId = "game-${game.appid}"
                    val checkedText = if (selectedGames.contains(game.appid)) "checked" else ""

                    //language=html
                    """
                    <div class='box-divider'>
                        <input type='checkbox' id='$elementId' name='game' value='$elementId' $checkedText>
                        <label for='$elementId'>${game.name}</label>
                    </div>
                    """.trimIndent()
                }
    }

    private fun appendSaveResult(): String {
        val resultBefore = session.data["saveSelectedResult"] ?: return ""

        val resultMessage = when (resultBefore) {
            "tooMany" -> "✖ You're only allowed to select up to ${config.maxSteamGroups} games!"
            "success" -> "✔ Your selection was saved!"
            else -> throw WebException(0x710, "Invalid save selected result")
        }

        return "<p class='alert'>$resultMessage</p><br/>"
    }

    /**
     * Saves the selection changed on the [showSelectGames] page
     */
    private fun saveSelectedGames(): Response {
        val games = request.form()
                .filter { pair -> pair.first == "game" }
                .mapNotNull { pair -> pair.second }
                .map { checkbox ->
                    try {
                        checkbox.replace("game-", "").toInt()
                    } catch (ex: NumberFormatException) {
                        throw WebException(0x800, "Couldn't a parse the string of a checkbox")
                    }
                }
                .toList()
        val uniqueId: String = session.getData("uniqueId", 0x704)

        if (games.size > config.maxSteamGroups) {
            session.data["saveSelectedResult"] = "tooMany"
        } else {
            SelectedGames.saveSelectedGames(uniqueId, games)
            Scheduler.execute { SteamConnector.setGroups(uniqueId) }
            session.data["saveSelectedResult"] = "success"
        }
        return redirectToPath("selectgames")
    }

}