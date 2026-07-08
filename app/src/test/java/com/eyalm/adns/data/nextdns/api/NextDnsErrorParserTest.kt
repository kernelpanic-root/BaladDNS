package com.eyalm.adns.data.nextdns.api

import com.eyalm.adns.domain.nextdns.ApiProblem
import com.eyalm.adns.data.nextdns.nextDnsFixture
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NextDnsErrorParserTest {

    @Test
    fun `parses array errors and source pointer`() {
        val body = JsonParser.parseString(
            nextDnsFixture("errors/array.json")
        ).asJsonObject

        assertEquals(
            listOf(
                ApiProblem(code = "invalid", field = "name"),
                ApiProblem(code = "conflict"),
            ),
            NextDnsErrorParser.parse(body),
        )
    }

    @Test
    fun `parses field map errors`() {
        val body = JsonParser.parseString(
            nextDnsFixture("errors/field-map.json")
        ).asJsonObject

        assertEquals(
            listOf(
                ApiProblem(code = "taken", field = "email"),
                ApiProblem(code = "incorrect", field = "currentPassword"),
            ),
            NextDnsErrorParser.parse(body),
        )
    }

    @Test
    fun `returns empty list when errors are absent or malformed`() {
        val success = JsonParser.parseString("{}").asJsonObject
        val malformed = JsonParser.parseString("""{ "errors": true }""").asJsonObject

        assertTrue(NextDnsErrorParser.parse(success).isEmpty())
        assertTrue(NextDnsErrorParser.parse(malformed).isEmpty())
    }
}
