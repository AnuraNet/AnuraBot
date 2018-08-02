package de.anura.bot.web

import java.util.*
import kotlin.streams.asSequence

class TokenManager {

    // Settings for the token generation
    private val tokenChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private val tokenLength = 20
    private val expirationTime = 60 * 15
    // Token (String) <> Token
    private val tokens = mutableMapOf<String, Token>()

    /**
     * Generates and saves a token for the [uniqueId] with a custom [tag]
     * Tokens duplications with the same [uniqueId] and [tag] will be removed when creating a new one
     */
    fun tokenFor(uniqueId: String, tag: String): String {
        // Searching for other tokens with the same unique id and removing them
        tokens.values.removeIf { it.tag == tag && it.uniqueId == uniqueId }

        // Generating the token and adding it to the cache
        val token = generateToken(uniqueId, tag)
        tokens[token.token] = token

        // Returing the token string
        return token.token
    }

    /**
     * Deletes a token from the cache and returns its unique id
     */
    fun destroyToken(token: String): String? {
        // Searching for the token in the cache
        val foundToken = findToken(token) ?: return null

        // Removing the token from the cache
        tokens.remove(foundToken.token)

        // Returing its unique id
        return foundToken.uniqueId
    }

    /**
     * Tries to find a token object with the given [token] string
     */
    private fun findToken(token: String): Token? {
        // Searching for a token in the cache
        val foundToken = tokens[token] ?: return null

        // Checking if it's expired and if so removing it
        if (foundToken.created + expirationTime <= timestamp()) {
            tokens.remove(token)
            return null
        }

        // Returing the valid token
        return foundToken
    }

    private fun generateToken(uniqueId: String, tag: String): Token {
        // Generating a random sequenze of strings
        val tokenString = Random().ints(tokenLength.toLong(), 0, tokenChars.length)
                .asSequence()
                .map { tokenChars[it] }
                .joinToString(separator = "")

        // If another token with same token string exists, we'll generate a new string
        if (findToken(tokenString) != null) return generateToken(uniqueId, tag)

        return Token(tokenString, timestamp(), tag, uniqueId)
    }

    private fun timestamp(): Long {
        return System.currentTimeMillis() / 1000
    }

    data class Token(val token: String, val created: Long, val tag: String, val uniqueId: String)

}