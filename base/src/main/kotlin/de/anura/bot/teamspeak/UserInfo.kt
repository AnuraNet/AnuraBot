package de.anura.bot.teamspeak

class UserInfo(private val ts5: Boolean) {

    // Teamspeak 3 format: https://www.teamspeak-info.de/ts_bb_codes.htm
    // Teamspeak 5 format: https://ts-n.net/index.php?artid=97

    fun bold(text: String): String {
        return when {
            ts5 -> "**$text**"
            else -> "[b]$text[/b]"
        }
    }

    fun italic(text: String): String {
        return when {
            ts5 -> "__${text}__"
            else -> "[i]$text[/i]"
        }
    }

    fun link(text: String, url: String): String {
        return if (ts5) {
            "[$text]($url)"
        } else {
            "[url=$url]$text[/url]"
        }
    }


}
