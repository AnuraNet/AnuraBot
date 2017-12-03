package de.anura.bot.config

class ConfigException : Throwable {

    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(message: String?) : super(message)
}