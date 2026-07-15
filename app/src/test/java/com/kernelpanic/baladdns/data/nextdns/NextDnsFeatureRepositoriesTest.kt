package com.kernelpanic.baladdns.data.nextdns

import com.kernelpanic.baladdns.data.nextdns.access.AccessRole
import com.kernelpanic.baladdns.data.nextdns.access.NextDnsAccessRepository
import com.kernelpanic.baladdns.data.nextdns.api.NextDnsApi
import com.kernelpanic.baladdns.data.nextdns.recreation.NextDnsRecreationRepository
import com.kernelpanic.baladdns.data.nextdns.recreation.RecreationScheduleDto
import com.kernelpanic.baladdns.data.nextdns.recreation.RecreationWindowDto
import com.kernelpanic.baladdns.data.nextdns.rewrites.NextDnsRewritesRepository
import com.kernelpanic.baladdns.data.nextdns.settings.AddCustomDomainResult
import com.kernelpanic.baladdns.data.nextdns.settings.ApiBinding
import com.kernelpanic.baladdns.data.nextdns.settings.NextDnsSettingsRepository
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

class NextDnsFeatureRepositoriesTest {
    private lateinit var server: MockWebServer
    private lateinit var api: NextDnsApi

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NextDnsApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `scalar patch uses one canonical nested API path`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val repository = NextDnsSettingsRepository(api)

        val result = repository.patchScalarSetting(
            profileId = "profile-id",
            binding = ApiBinding(
                page = "settings",
                path = listOf("logs", "retention"),
            ),
            encodedValue = 63_072_000,
        )

        assertTrue(result is ApiResult.Success)
        val request = server.takeRequest()
        assertEquals("/profiles/profile-id/settings", request.requestUrl!!.encodedPath)
        assertEquals(
            "{\"logs\":{\"retention\":63072000}}",
            request.body.readUtf8(),
        )
    }

    @Test
    fun `duplicate custom domain remains a typed domain result`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"errors":[{"code":"duplicate"}]}""")
        )
        val repository = NextDnsSettingsRepository(api)

        val result = repository.addCustomDomain(
            profileId = "profile-id",
            page = "allowlist",
            domain = "example.com",
        )

        assertEquals(AddCustomDomainResult.AlreadyExists, result)
    }

    @Test
    fun `successful custom domain add accepts empty response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val repository = NextDnsSettingsRepository(api)

        val result = repository.addCustomDomain(
            profileId = "profile-id",
            page = "allowlist",
            domain = "example.com",
        )

        assertEquals(AddCustomDomainResult.Added, result)
    }

    @Test
    fun `rewrite create maps one item and delete accepts empty success`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"data":{"id":"rewrite-id","name":"example.com","type":"A","content":"1.1.1.1"}}"""
                )
        )
        server.enqueue(MockResponse().setResponseCode(204))
        val repository = NextDnsRewritesRepository(api)

        val created = repository.create(
            "profile-id",
            "example.com",
            "1.1.1.1",
        )
        assertTrue(created is ApiResult.Success)
        assertEquals("rewrite-id", (created as ApiResult.Success).value.id)
        assertEquals("POST", server.takeRequest().method)

        val deleted = repository.delete("profile-id", "rewrite-id")
        assertTrue(deleted is ApiResult.Success)
        val deleteRequest = server.takeRequest()
        assertEquals(
            "/profiles/profile-id/rewrites/rewrite-id",
            deleteRequest.requestUrl!!.encodedPath,
        )
        assertEquals("DELETE", deleteRequest.method)
    }

    @Test
    fun `access email is encoded exactly once`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val repository = NextDnsAccessRepository(api)

        val result = repository.updateRole(
            profileId = "profile-id",
            email = "test+shared@example.com",
            role = AccessRole.Viewer,
        )

        assertTrue(result is ApiResult.Success)
        assertEquals(
            "/profiles/profile-id/access/test%2Bshared%40example.com",
            server.takeRequest().requestUrl!!.encodedPath,
        )
    }

    @Test
    fun `recreation schedule uses the parental control endpoint`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val repository = NextDnsRecreationRepository(api)

        val result = repository.updateSchedule(
            profileId = "profile-id",
            schedule = RecreationScheduleDto(
                times = mapOf(
                    "monday" to RecreationWindowDto(
                        start = "18:00:00",
                        end = "20:30:00",
                    )
                ),
                timezone = "Asia/Jerusalem",
            ),
        )

        assertTrue(result is ApiResult.Success)
        val request = server.takeRequest()
        assertEquals("/profiles/profile-id/parentalControl", request.requestUrl!!.encodedPath)
        assertTrue(request.body.readUtf8().contains("\"monday\""))
    }
}
