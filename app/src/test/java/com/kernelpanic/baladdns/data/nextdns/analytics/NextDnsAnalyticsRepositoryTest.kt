package com.kernelpanic.baladdns.data.nextdns.analytics

import com.kernelpanic.baladdns.data.nextdns.api.NextDnsApi
import com.kernelpanic.baladdns.domain.nextdns.ApiResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NextDnsAnalyticsRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: NextDnsAnalyticsRepository

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NextDnsApi::class.java)
        repository = NextDnsAnalyticsRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `device and selected period are sent to graph and card endpoints`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":[],"meta":{}}""")
        )
        val scope = AnalyticsScope(AnalyticsPeriod.Hours6, "device-id")

        val graph = repository.getGraph("profile-id", scope)

        assertTrue(graph is ApiResult.Success)
        val graphRequest = server.takeRequest().requestUrl!!
        assertEquals("-6h", graphRequest.queryParameter("from"))
        assertEquals("device-id", graphRequest.queryParameter("device"))

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":[]}""")
        )
        val card = repository.getCardData(
            profileId = "profile-id",
            feature = "domains",
            baseParams = mapOf("limit" to "6"),
            scope = scope,
        )

        assertTrue(card is ApiResult.Success)
        val cardRequest = server.takeRequest().requestUrl!!
        assertEquals("-6h", cardRequest.queryParameter("from"))
        assertEquals("device-id", cardRequest.queryParameter("device"))
        assertEquals("6", cardRequest.queryParameter("limit"))
    }
}
