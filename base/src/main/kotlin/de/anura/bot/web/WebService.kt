package de.anura.bot.web

interface WebService {

    fun getLoginUrl(uid: String): String

    fun getSelectGamesUrl(uid: String): String

    fun stop()

}