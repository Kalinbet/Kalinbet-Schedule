package com.kalinbetschedule.data.backup
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.content.edit
import androidx.core.net.toUri
import java.security.MessageDigest
import java.security.SecureRandom
object YandexAuth {
    const val CLIENT_ID = "3a827a9711724ff298656b908a022b3d"
    const val REDIRECT_URI = "kalinbetschedule://oauth"
    private const val SCOPE = "cloud_api:disk.app_folder"
    private const val AUTHORIZE_URL = "https://oauth.yandex.ru/authorize"
    private const val TOKEN_URL = "https://oauth.yandex.ru/token"
    private const val PREFS = "backup"
    private const val KEY_TOKEN = "yandex_token"
    val configured: Boolean get() = CLIENT_ID.isNotBlank()
    private var verifier: String? = null
    fun token(context: Context): String? =
        prefs(context).getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }
    fun signOut(context: Context) {
        prefs(context).edit { remove(KEY_TOKEN) }
    }
    fun authorizeIntent(): Intent {
        val codeVerifier = randomVerifier().also { verifier = it }
        val url = AUTHORIZE_URL.toUri().buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPE)
            .appendQueryParameter("code_challenge", challengeOf(codeVerifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
        return Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    fun completeSignIn(context: Context, redirect: Uri): Boolean {
        val code = redirect.getQueryParameter("code") ?: return false
        val codeVerifier = verifier ?: return false
        verifier = null
        val response = runCatching {
            Http.postForm(
                url = TOKEN_URL,
                fields = mapOf(
                    "grant_type" to "authorization_code",
                    "code" to code,
                    "client_id" to CLIENT_ID,
                    "code_verifier" to codeVerifier,
                    "redirect_uri" to REDIRECT_URI
                )
            )
        }.getOrNull() ?: return false
        val token = response.optString("access_token").takeIf { it.isNotBlank() } ?: return false
        prefs(context).edit { putString(KEY_TOKEN, token) }
        return true
    }
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun randomVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, BASE64_URL)
    }
    private fun challengeOf(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray())
        return Base64.encodeToString(digest, BASE64_URL)
    }
    private const val BASE64_URL = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
}
