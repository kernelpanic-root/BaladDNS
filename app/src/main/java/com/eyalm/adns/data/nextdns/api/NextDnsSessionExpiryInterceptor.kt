package com.eyalm.adns.data.nextdns.api

import com.google.gson.JsonParser
import okhttp3.Interceptor
import okhttp3.Response

internal class NextDnsSessionExpiryInterceptor(
    private val onUnauthorized: () -> Unit,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.requiresAuthentication()) onUnauthorized()
        return response
    }

    private fun Response.requiresAuthentication(): Boolean {
        if (code == 401) return true
        if (code != 403) return false

        val root = runCatching {
            JsonParser.parseString(peekBody(MAX_ERROR_BODY_BYTES).string()).asJsonObject
        }.getOrNull() ?: return false

        return NextDnsErrorParser.parse(root).any { problem ->
            problem.code.equals("authRequired", ignoreCase = true)
        }
    }

    private companion object {
        const val MAX_ERROR_BODY_BYTES = 8_192L
    }
}
