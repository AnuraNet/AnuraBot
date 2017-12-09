package de.anura.bot.web

abstract class SteamException : RuntimeException {

    constructor(message: String) : super(message)

    constructor(message: String, throwable: Throwable?) : super(message, throwable)

}

class SteamHttpException(public val url: String, public val status: Int) :
        SteamException("Couldn't execute Steam request to $url. Http Status: $status")

class SteamJsonException(throwable: Throwable?) :
        SteamException("Couldn't parse the answer from Steam as JSON!", throwable)