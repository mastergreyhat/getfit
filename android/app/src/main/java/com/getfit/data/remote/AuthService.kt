package com.getfit.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AuthUser(
    val id: String,
    val email: String,
    val name: String,
    val authProvider: String
)

data class AuthResult(
    val token: String,
    val user: AuthUser
)

data class SyncResult(
    val backupJson: String?,
    val updatedAt: Long?
)

/**
 * Communicates with GetFit Cloud Backend.
 * Lightweight HTTP client using native HttpURLConnection - zero external dependencies.
 */
object AuthService {

    private const val TAG = "AuthService"
    private const val BASE_URL = "https://getfit-nutrition-proxy.aswinvijay4686.workers.dev"
    private const val TIMEOUT_MS = 15000

    private val DISPOSABLE_DOMAINS = setOf(
        "tempmail.com", "temp-mail.org", "10minutemail.com", "mailinator.com",
        "guerrillamail.com", "guerrillamail.net", "guerrillamail.org",
        "throwawaymail.com", "trashmail.com", "trashmail.net", "trashmail.me",
        "yopmail.com", "yopmail.net", "yopmail.fr", "sharklasers.com", "dispostable.com",
        "getairmail.com", "crazymailing.com", "fakeinbox.com", "generator.email",
        "inboxbear.com", "mohmal.com", "mytemp.email", "tempail.com", "tempm.com",
        "tmpmail.net", "tmpmail.org", "nada.ltd", "dropmail.me", "fakemailgenerator.com",
        "burnermail.io", "maildrop.cc", "emailondeck.com", "trash-mail.com",
        "temp-mail.io", "disposablemail.com", "fakemail.net", "mytempemail.com",
        "tempemailgen.com", "minuteinbox.com", "getnada.com", "inboxkitten.com",
        "throwawaymail.net", "trashmail.io", "byom.de", "mailcatch.com"
    )

    fun isDisposableEmail(email: String): Boolean {
        val domain = email.substringAfter("@", "").trim().lowercase()
        if (domain.isBlank()) return true
        if (DISPOSABLE_DOMAINS.contains(domain)) return true
        if (domain.contains("tempmail") || domain.contains("throwaway") || domain.contains("trashmail") || domain.contains("guerrillamail") || domain.contains("10minute")) {
            return true
        }
        return false
    }

    suspend fun sendOtp(email: String, purpose: String = "register"): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = email.trim().lowercase()
        if (isDisposableEmail(trimmed)) {
            return@withContext Result.failure(Exception("Temporary and disposable email addresses are not allowed. Please enter your real email address."))
        }

        try {
            val body = JSONObject().apply {
                put("email", trimmed)
                put("purpose", purpose)
            }
            val (code, responseStr) = executeHttp("$BASE_URL/api/auth/send-otp", "POST", body.toString(), null)
            if (code in 200..299) {
                val json = JSONObject(responseStr)
                val msg = json.optString("message", "Verification code sent to your email.")
                Result.success(msg)
            } else {
                val err = parseError(responseStr, "Failed to send verification code ($code)")
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Send OTP error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, name: String, otp: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        val trimmed = email.trim().lowercase()
        if (isDisposableEmail(trimmed)) {
            return@withContext Result.failure(Exception("Temporary and disposable email addresses are not allowed."))
        }

        try {
            val body = JSONObject().apply {
                put("email", trimmed)
                put("password", password)
                put("name", name.trim())
                put("otp", otp.trim())
            }
            val response = postJson("$BASE_URL/api/auth/register", body.toString())
            parseAuthResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Register error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", password)
            }
            val response = postJson("$BASE_URL/api/auth/login", body.toString())
            parseAuthResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String, otp: String, newPassword: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("otp", otp.trim())
                put("newPassword", newPassword)
            }
            val (code, responseStr) = postJson("$BASE_URL/api/auth/reset-password", body.toString())
            if (code in 200..299) {
                val json = JSONObject(responseStr)
                Result.success(json.optString("message", "Password reset successfully. You can now log in."))
            } else {
                Result.failure(Exception(parseError(responseStr, "Password reset failed ($code)")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reset password error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(token: String, confirmEmail: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", confirmEmail.trim().lowercase())
            }
            val (code, responseStr) = executeHttp(
                urlStr = "$BASE_URL/api/auth/delete-account",
                method = "POST",
                payload = body.toString(),
                token = token
            )
            if (code in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseError(responseStr, "Delete account failed ($code)")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete account error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadSync(token: String, backupJson: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("backupJson", backupJson)
            }
            val (code, responseStr) = executeHttp(
                urlStr = "$BASE_URL/api/sync",
                method = "POST",
                payload = body.toString(),
                token = token
            )
            if (code in 200..299) {
                val json = JSONObject(responseStr)
                val syncedAt = json.optLong("syncedAt", System.currentTimeMillis())
                Result.success(syncedAt)
            } else {
                val err = parseError(responseStr, "Upload sync failed ($code)")
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload sync error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun downloadSync(token: String): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            val (code, responseStr) = executeHttp(
                urlStr = "$BASE_URL/api/sync",
                method = "GET",
                payload = null,
                token = token
            )
            if (code in 200..299) {
                val json = JSONObject(responseStr)
                val backupJson = if (json.isNull("backupJson")) null else json.optString("backupJson").takeIf { it.isNotBlank() && it != "null" }
                val updatedAt = if (json.isNull("updatedAt")) null else json.optLong("updatedAt", 0L).takeIf { it > 0 }
                Result.success(SyncResult(backupJson = backupJson, updatedAt = updatedAt))
            } else {
                val err = parseError(responseStr, "Download sync failed ($code)")
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download sync error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun postJson(urlStr: String, payload: String): Pair<Int, String> {
        return executeHttp(urlStr, "POST", payload, token = null)
    }

    private fun parseAuthResponse(resp: Pair<Int, String>): Result<AuthResult> {
        val (code, body) = resp
        return try {
            if (code in 200..299) {
                val root = JSONObject(body)
                val token = root.getString("token")
                val u = root.getJSONObject("user")
                val user = AuthUser(
                    id = u.getString("id"),
                    email = u.getString("email"),
                    name = u.optString("name", "User"),
                    authProvider = u.optString("authProvider", "email")
                )
                Result.success(AuthResult(token = token, user = user))
            } else {
                Result.failure(Exception(parseError(body, "Request failed with code $code")))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Invalid server response: ${e.message}"))
        }
    }

    private fun parseError(body: String, fallback: String): String {
        return try {
            val obj = JSONObject(body)
            obj.optString("error", obj.optString("message", fallback))
        } catch (_: Exception) {
            fallback
        }
    }

    private fun executeHttp(urlStr: String, method: String, payload: String?, token: String?): Pair<Int, String> {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                if (payload != null) {
                    doOutput = true
                    OutputStreamWriter(outputStream, "UTF-8").use { it.write(payload) }
                }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) {
                try { conn.inputStream } catch (_: Exception) { null }
            } else {
                conn.errorStream ?: try { conn.inputStream } catch (_: Exception) { null }
            }
            val response = try {
                stream?.let {
                    BufferedReader(InputStreamReader(it, "UTF-8")).use { reader ->
                        reader.readText()
                    }
                } ?: ""
            } catch (_: Exception) {
                ""
            }
            Pair(code, response)
        } finally {
            conn?.disconnect()
        }
    }
}
