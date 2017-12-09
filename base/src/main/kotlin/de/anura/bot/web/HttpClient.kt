package de.anura.bot.web

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object HttpClient {

    /**
     * Sends a basic HTTP request to the [url] using the defined HTTP [method]
     */
    fun request(url: String, method: String): HttpAnswer {
        val obj = URL(url)

        return with(obj.openConnection() as HttpURLConnection) {
            requestMethod = method

            val byteAnswer = try {
                inputStream.use { it.readBytes() }
            } catch (ex: IOException) {
                errorStream.use { it.readBytes() }
            }
            val answer = byteAnswer.toString(charset("UTF-8"))
            HttpAnswer(responseCode, answer)
        }
    }

    /**
     * The answer returned by an HTTP request
     *
     * @property status The HTTP status code
     * @property text The HTTP body
     */
    data class HttpAnswer(val status: Int, val text: String)

}
