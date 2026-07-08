package com.eyalm.adns.data.nextdns.api

import okhttp3.Interceptor
import okhttp3.Response

internal class NextDnsApiKeyInterceptor(
    private val apiKeyProvider: () -> String?,
    private val allowedHosts: Set<String>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()
            .removeHeader(API_KEY_HEADER)

        apiKeyForHost(originalRequest.url.host)?.let { key ->
            builder.header(API_KEY_HEADER, key)
        }

        return chain.proceed(builder.build())
    }

    internal fun apiKeyForHost(host: String): String? {
        if (host !in allowedHosts) return null
        return apiKeyProvider()
    }

    private companion object {
        const val API_KEY_HEADER = "X-Api-Key"
    }
}
