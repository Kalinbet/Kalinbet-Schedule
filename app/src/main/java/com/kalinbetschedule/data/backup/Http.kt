package com.kalinbetschedule.data.backup
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
internal object Http {
    private const val TIMEOUT_MS = 20_000
    fun postForm(url: String, fields: Map<String, String>): JSONObject {
        val body = fields.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val connection = open(url, "POST")
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.outputStream.use { it.write(body.toByteArray()) }
        return JSONObject(connection.readText())
    }
    fun getJson(url: String, token: String): JSONObject {
        val connection = open(url, "GET")
        connection.setRequestProperty("Authorization", "OAuth $token")
        return JSONObject(connection.readText())
    }
    fun getText(url: String): String = open(url, "GET").readText()
    fun put(url: String, body: String) {
        val connection = open(url, "PUT")
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(body.toByteArray()) }
        connection.check()
        connection.disconnect()
    }
    private fun open(url: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
        }
    private fun HttpURLConnection.readText(): String {
        check()
        return inputStream.use { it.readBytes().toString(Charsets.UTF_8) }.also { disconnect() }
    }
    private fun HttpURLConnection.check() {
        if (responseCode in 200..299) return
        val detail = runCatching {
            errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull()
        val message = detail
            ?.let { runCatching { JSONObject(it).optString("message") }.getOrNull() }
            ?.takeIf { it.isNotBlank() }
        disconnect()
        throw IOException(message ?: "Ошибка $responseCode")
    }
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
