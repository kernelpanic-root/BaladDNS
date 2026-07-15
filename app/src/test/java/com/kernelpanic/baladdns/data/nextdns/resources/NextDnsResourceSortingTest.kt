package com.kernelpanic.baladdns.data.nextdns.resources

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NextDnsResourceSortingTest {
    private val blocklists = listOf(
        NextDnsResourceItem(
            id = "z-list",
            name = "Zulu",
            entries = null,
            updatedOn = null,
            sourceIndex = 0,
        ),
        NextDnsResourceItem(
            id = "a-list",
            name = "Alpha",
            entries = 10,
            updatedOn = "2026-01-01T00:00:00Z",
            sourceIndex = 1,
        ),
        NextDnsResourceItem(
            id = "b-list",
            name = "Beta",
            entries = 30,
            updatedOn = "2026-02-01T00:00:00Z",
            sourceIndex = 2,
        ),
    )

    @Test
    fun `popularity preserves server order`() {
        assertEquals(
            listOf("z-list", "a-list", "b-list"),
            orderResourceItems(blocklists, "blocklists").map { it.id },
        )
    }

    @Test
    fun `entry and recent sorts keep missing metadata last`() {
        assertEquals(
            listOf("b-list", "a-list", "z-list"),
            orderResourceItems(
                blocklists,
                "blocklists",
                BlocklistSort.Entries,
            ).map { it.id },
        )
        assertEquals(
            listOf("b-list", "a-list", "z-list"),
            orderResourceItems(
                blocklists,
                "blocklists",
                BlocklistSort.Recent,
            ).map { it.id },
        )
    }

    @Test
    fun `tlds group abused names first and sort each group`() {
        val ordered = orderResourceItems(
            listOf(
                NextDnsResourceItem("zip", ".zip", spamhausRank = 2),
                NextDnsResourceItem("aaa", ".aaa"),
                NextDnsResourceItem("bar", ".bar", spamhausRank = 10),
            ),
            "tlds",
        )

        assertEquals(listOf("bar", "zip", "aaa"), ordered.map { it.id })
        assertTrue(ordered.take(2).all { it.spamhausRank > 0 })
        assertEquals(0, ordered.last().spamhausRank)
    }

    @Test
    fun `server metadata is retained for tlds and blocklists`() {
        val tlds = mapServerResourceItems(
            "tlds",
            JsonParser.parseString(
                """[{"id":"work","spamhaus":10},{"id":"xn--nqv7f","spamhaus":1},{"id":"aaa","spamhaus":0}]"""
            ).asJsonArray,
        )
        assertEquals(10, tlds.first().spamhausRank)
        assertEquals(".机构", tlds[1].name)
        assertEquals(0, tlds.last().spamhausRank)

        val blocklist = mapServerResourceItems(
            "blocklists",
            JsonParser.parseString(
                """[{"id":"sample","name":"Sample","website":"https://example.com","description":"Description","entries":42,"updatedOn":"2026-01-01T00:00:00Z"}]"""
            ).asJsonArray,
        ).single()
        assertEquals("https://example.com", blocklist.website)
        assertEquals("Description", blocklist.description)
        assertEquals(42, blocklist.entries)
        assertEquals("2026-01-01T00:00:00Z", blocklist.updatedOn)
        assertEquals(0, blocklist.sourceIndex)
    }

    @Test
    fun `resource filtering combines search with enabled-only state`() {
        val items = listOf(
            NextDnsResourceItem("alpha", "Alpha list", description = "Tracking"),
            NextDnsResourceItem("beta", "Beta list", description = "Security"),
            NextDnsResourceItem("gamma", "Gamma list", description = "Tracking"),
        )

        assertEquals(
            listOf("alpha"),
            filterResourceItems(
                items = items,
                query = "tracking",
                enabledOnly = true,
                activeIds = setOf("alpha", "beta"),
            ).map { it.id },
        )
        assertEquals(
            listOf("beta"),
            filterResourceItems(
                items = items,
                query = "beta",
                enabledOnly = false,
                activeIds = emptySet(),
            ).map { it.id },
        )
    }
}
