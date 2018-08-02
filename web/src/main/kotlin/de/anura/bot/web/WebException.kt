package de.anura.bot.web

/**
 * A Exception which can be thrown in an function called by an [AbstractRequestHandler]
 */
class WebException : Exception {

    public val code: Int

    constructor(code: Int, message: String) : super(message) {
        this.code = code
    }

    constructor(code: Int, throwable: Throwable) : super(throwable) {
        this.code = code
    }

}