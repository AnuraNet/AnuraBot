package de.anura.bot.teamspeak

class UserInfo(private val ts5: Boolean) {

    fun boldStart(): String {
        return if (ts5) "**" else "[b]"
    }

    fun boldEnd(): String {
        return if (ts5) "**" else "[/b]"
    }

    fun bold(text: String): String {
        return boldStart() + text + boldEnd()
    }

    fun link(text: String, url: String): String {
        return if (ts5) {
            "[$text]($url)"
        } else {
            "[url=$url]$text[/url]"
        }
    }


}
