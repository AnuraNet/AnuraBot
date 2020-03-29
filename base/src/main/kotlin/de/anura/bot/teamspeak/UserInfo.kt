package de.anura.bot.teamspeak

import java.net.URLEncoder

class UserInfo(private val ts5: Boolean) {

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
     * @see http://yat.qa/ressourcen/definitionen-und-algorithmen/#int-links
     *
     */
    fun clientLink(clientId: Int, uniqueId: String, name: String): String {
        if (ts5) {
            return name
        }

        // Encoding only the name
        val encodedName = URLEncoder.encode(name, "UTF-8")
        return link(name, "client://$clientId/$uniqueId~$encodedName");
    }


}
