package com.eyalm.adns.data.network

import android.content.Context
import com.eyalm.adns.data.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://api.nextdns.io/"

    private lateinit var tokenManager: TokenManager
    private lateinit var nextDnsApiInternal: NextDnsApi

    /**
     * Initialize the API client. Call this once from Application.onCreate() or similar with an app Context.
     */
    @Synchronized
    fun init(context: Context) {
        if (::nextDnsApiInternal.isInitialized) return

        tokenManager = TokenManager(context.applicationContext)

        val dispatcher = okhttp3.Dispatcher().apply {
            maxRequests = 30
            maxRequestsPerHost = 15
        }

        val okHttpClient = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val path = originalRequest.url.encodedPath
                val builder = originalRequest.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")

                val isLoginOrApiKeyFlow = path.contains("accounts/@login") || path.contains("account/apiKeys")

                if (isLoginOrApiKeyFlow) {
                    builder.header("Origin", "https://my.nextdns.io")
                    builder.header("Referer", "https://my.nextdns.io/")
                } else {
                    val key = tokenManager.getApiKey()
                    if (key != null) {
                        builder.addHeader("X-Api-Key", key)
                    }
                }

                chain.proceed(builder.build())
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        nextDnsApiInternal = retrofit.create(NextDnsApi::class.java)
    }


    val nextDnsApi: NextDnsApi
        get() = if (::nextDnsApiInternal.isInitialized) nextDnsApiInternal
        else throw IllegalStateException("ApiClient not initialized. Call ApiClient.init(context) before using it.")
}