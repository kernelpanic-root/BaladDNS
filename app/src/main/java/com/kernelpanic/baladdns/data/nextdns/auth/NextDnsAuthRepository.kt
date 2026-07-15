package com.kernelpanic.baladdns.data.nextdns.auth

import android.content.Context
import com.kernelpanic.baladdns.R
import com.kernelpanic.baladdns.data.TokenManager
import com.kernelpanic.baladdns.data.network.ApiClient
import com.kernelpanic.baladdns.data.nextdns.api.NextDnsLoginRequest
import com.kernelpanic.baladdns.data.nextdns.api.NextDnsErrorParser
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class NextDnsAuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val tokenManager = TokenManager.getInstance(context.applicationContext)
    private val sessionManager = NextDnsSessionManager.getInstance(context.applicationContext)

    private var pendingKey: PendingKey? = null

    suspend fun login(state: NextDnsLoginUiState): NextDnsLoginOutcome =
        when (state.mode) {
            NextDnsLoginMode.Password -> loginWithPassword(
                email = state.email.trim(),
                password = state.password,
                code = state.code.takeIf { state.requiresTwoFactor },
            )

            NextDnsLoginMode.ApiKey -> loginWithApiKey(state.apiKey.trim())
        }

    fun discardPendingKey() {
        pendingKey = null
    }

    private suspend fun loginWithPassword(
        email: String,
        password: String,
        code: String?,
    ): NextDnsLoginOutcome = withContext(Dispatchers.IO) {
        pendingKey?.takeIf { it.email == email }?.let { pending ->
            return@withContext verifyAndPersist(pending.key, pending.email)
        }

        try {
            val response = ApiClient.nextDnsCookieAuthApi.login(
                NextDnsLoginRequest(email, password, code)
            )
            val raw = response.body()?.string()
                ?: response.errorBody()?.string()
                ?: ""

            when (
                val loginResponse = classifyPasswordLoginResponse(
                    status = response.code(),
                    raw = raw,
                    submittedCode = code != null,
                )
            ) {
                PasswordLoginResponse.Accepted -> Unit
                is PasswordLoginResponse.Outcome -> {
                    return@withContext loginResponse.value
                }
            }

            val cookie = response.headers()
                .values("Set-Cookie")
                .map { it.substringBefore(';') }
                .filter(String::isNotBlank)
                .joinToString("; ")

            if (cookie.isBlank()) {
                return@withContext NextDnsLoginOutcome.Failure(
                    NextDnsLoginFailure.Unknown
                )
            }

            val keyResponse = ApiClient.nextDnsCookieAuthApi
                .exchangeCookieForApiKey(cookie)
            if (!keyResponse.isSuccessful) {
                return@withContext failureForProvisioningStatus(keyResponse.code())
            }

            val apiKey = keyResponse.body()?.key
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: return@withContext NextDnsLoginOutcome.Failure(
                    NextDnsLoginFailure.Unknown
                )

            pendingKey = PendingKey(apiKey, email)
            verifyAndPersist(apiKey, email)
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            NextDnsLoginOutcome.Failure(NextDnsLoginFailure.Offline)
        } catch (_: Exception) {
            NextDnsLoginOutcome.Failure(NextDnsLoginFailure.Unknown)
        }
    }

    private suspend fun loginWithApiKey(apiKey: String): NextDnsLoginOutcome =
        withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.nextDnsApiKeyAuthApi.verifyApiKey(apiKey)
                if (!response.isSuccessful) {
                    return@withContext NextDnsLoginOutcome.Failure(
                        if (response.code() == 401 || response.code() == 403) {
                            NextDnsLoginFailure.InvalidApiKey
                        } else if (response.code() >= 500) {
                            NextDnsLoginFailure.ServerUnavailable
                        } else {
                            NextDnsLoginFailure.Unknown
                        },
                        field = NextDnsLoginField.ApiKey,
                    )
                }

                val profiles = response.body()?.data
                    ?: return@withContext NextDnsLoginOutcome.Failure(
                        NextDnsLoginFailure.Unknown
                    )

                persistSession(apiKey, email = null)
                NextDnsLoginOutcome.Authenticated(profiles)
            } catch (error: CancellationException) {
                throw error
            } catch (_: IOException) {
                NextDnsLoginOutcome.Failure(NextDnsLoginFailure.Offline)
            } catch (_: Exception) {
                NextDnsLoginOutcome.Failure(NextDnsLoginFailure.Unknown)
            }
        }

    private suspend fun verifyAndPersist(
        apiKey: String,
        email: String,
    ): NextDnsLoginOutcome {
        val response = try {
            ApiClient.nextDnsApiKeyAuthApi.verifyApiKey(apiKey)
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            return NextDnsLoginOutcome.Failure(NextDnsLoginFailure.Offline)
        } catch (_: Exception) {
            return NextDnsLoginOutcome.Failure(NextDnsLoginFailure.Unknown)
        }

        if (!response.isSuccessful) {
            if (response.code() == 401 || response.code() == 403) pendingKey = null
            return failureForProvisioningStatus(response.code())
        }

        val profiles = response.body()?.data
            ?: return NextDnsLoginOutcome.Failure(NextDnsLoginFailure.Unknown)

        persistSession(apiKey, email)
        pendingKey = null
        return NextDnsLoginOutcome.Authenticated(profiles)
    }

    private fun persistSession(apiKey: String, email: String?) {
        tokenManager.saveApiKey(apiKey)
        tokenManager.saveEmail(email ?: appContext.getString(R.string.api_key_account))
        sessionManager.authenticated()
    }

    private fun failureForProvisioningStatus(status: Int): NextDnsLoginOutcome.Failure =
        NextDnsLoginOutcome.Failure(
            when {
                status >= 500 -> NextDnsLoginFailure.ServerUnavailable
                else -> NextDnsLoginFailure.Unknown
            }
        )

    private data class PendingKey(
        val key: String,
        val email: String,
    )
}

internal sealed interface PasswordLoginResponse {
    data object Accepted : PasswordLoginResponse

    data class Outcome(
        val value: NextDnsLoginOutcome,
    ) : PasswordLoginResponse
}


internal fun classifyPasswordLoginResponse(
    status: Int,
    raw: String,
    submittedCode: Boolean,
): PasswordLoginResponse {
    val root = runCatching {
        JsonParser.parseString(raw).asJsonObject
    }.getOrNull()

    if (root != null) {
        if (root.has("errors")) {
            val problems = NextDnsErrorParser.parse(root)
            return PasswordLoginResponse.Outcome(
                if (problems.isNotEmpty()) {
                    mapLoginFailure(status, raw, submittedCode)
                } else {
                    NextDnsLoginOutcome.Failure(NextDnsLoginFailure.Unknown)
                }
            )
        }
        val requiresCode = root.get("requiresCode")
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }
            ?.asBoolean == true
        if (requiresCode) {
            return PasswordLoginResponse.Outcome(
                NextDnsLoginOutcome.RequiresTwoFactor
            )
        }
    }

    if (status !in 200..299) {
        return PasswordLoginResponse.Outcome(
            mapLoginFailure(status, raw, submittedCode)
        )
    }

    return if (root != null || raw.trim() == "OK") {
        PasswordLoginResponse.Accepted
    } else {
        PasswordLoginResponse.Outcome(
            NextDnsLoginOutcome.Failure(NextDnsLoginFailure.Unknown)
        )
    }
}

internal fun mapLoginFailure(
    status: Int,
    raw: String,
    submittedCode: Boolean,
): NextDnsLoginOutcome.Failure {
    val root = runCatching {
        JsonParser.parseString(raw).asJsonObject
    }.getOrNull()
    val problems = root?.let(NextDnsErrorParser::parse).orEmpty()
    val codeProblem = problems.firstOrNull { problem ->
        problem.field?.contains("code", ignoreCase = true) == true ||
            problem.code.contains("code", ignoreCase = true)
    }

    if (codeProblem != null) {
        return NextDnsLoginOutcome.Failure(
            NextDnsLoginFailure.InvalidTwoFactorCode,
            NextDnsLoginField.Code,
        )
    }

    val invalidCredentials = problems.any { problem ->
        problem.code.equals("invalidCombination", ignoreCase = true) ||
            problem.code.equals("invalidCredentials", ignoreCase = true) ||
            problem.field?.contains("password", ignoreCase = true) == true
    }
    if (invalidCredentials || status == 401) {
        return NextDnsLoginOutcome.Failure(
            if (submittedCode && !invalidCredentials) {
                NextDnsLoginFailure.InvalidTwoFactorCode
            } else {
                NextDnsLoginFailure.InvalidCredentials
            },
            if (submittedCode && !invalidCredentials) {
                NextDnsLoginField.Code
            } else {
                NextDnsLoginField.Password
            },
        )
    }

    return NextDnsLoginOutcome.Failure(
        if (status >= 500) {
            NextDnsLoginFailure.ServerUnavailable
        } else {
            NextDnsLoginFailure.Unknown
        }
    )
}
