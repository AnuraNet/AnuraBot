package de.anura.bot.web

import org.http4k.core.cookie.Cookie
import java.util.*
import kotlin.streams.asSequence

class SessionManager {

    // Settings for the cookie
    val cookieName = "BotSession"
    // Settings for the token generation
    private val tokenChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private val tokenLength = 24
    private val expirationTime = 60 * 15
    // Session Identifier (String) <> Session
    private val sessions = mutableMapOf<String, Session>()

    /**
     * Generates a random token
     */
    fun startSession(): Session {
        // Generating the token
        val token = generateToken()

        // Setting up the session
        val session = Session(token, timestamp(), mutableMapOf(), true)

        // Storing it to the cache
        sessions[session.token] = session

        return session
    }

    /**
     * Generates a cookie object for the [session]
     */
    fun cookieForSession(session: Session): Cookie {

        session.fresh = false

        return Cookie(
                name = cookieName,
                value = session.token,
                maxAge = expirationTime.toLong(),
                httpOnly = true
        )
    }

    /**
     * Tries to find a session object for the given [token]
     */
    fun findSession(token: String): Session? {
        // Searching for the session in the cache
        val session = sessions[token] ?: return null

        // Checking if it's still valid
        if (session.lastAccess + expirationTime <= timestamp()) {
            // If not removing it from the cache
            sessions.remove(token)
            return null
        }

        return session
    }

    private fun generateToken(): String {
        // Generating a random sequenze of strings
        val token = Random().ints(tokenLength.toLong(), 0, tokenChars.length)
                .asSequence()
                .map { tokenChars[it] }
                .joinToString(separator = "")

        // If another token with same token string exists, we'll generate a new string
        if (findSession(token) != null) return generateToken()

        return token
    }

    private fun timestamp(): Long {
        return System.currentTimeMillis() / 1000
    }

    data class Session(
            val token: String,
            var lastAccess: Long,
            val data: MutableMap<String, String>,
            var fresh: Boolean
    ) {
        /**
         * Gets data for the [key]
         *
         * @throws WebException with the code [code], if there's no data for the [key]
         * @return The data for the [key]
         */
        public fun getData(key: String, code: Int): String {
            return this.data[key] ?: throw WebException(code, "There's no data for the key $key in this session")
        }
    }

}