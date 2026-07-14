package com.eyalm.adns.data.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleCatalogTest {
    @Test
    fun `legacy and region tags normalize to canonical language tags`() {
        assertEquals("he", canonicalLocaleTag("iw"))
        assertEquals("he", canonicalLocaleTag("he"))
        assertEquals("pt-BR", canonicalLocaleTag("pt_br"))
    }

    @Test
    fun `catalog merge recursively overlays selected strings and keeps english fallback`() {
        val english = mapOf(
            "global" to mapOf("save" to "Save", "cancel" to "Cancel"),
            "title" to "English title",
        )
        val selected = mapOf(
            "global" to mapOf("save" to "Kaydet"),
        )

        val merged = mergeCatalog(english, selected)

        @Suppress("UNCHECKED_CAST")
        val global = merged.getValue("global") as Map<String, Any>
        assertEquals("Kaydet", global["save"])
        assertEquals("Cancel", global["cancel"])
        assertEquals("English title", merged["title"])
    }

    @Test
    fun `locale descriptors are sorted and describe independent catalog coverage`() {
        val locales = buildLocaleDescriptors(
            androidLocaleTags = setOf("en", "iw"),
            nextDnsAssetNames = listOf("tr.json", "en.json", "README.txt"),
        )

        assertEquals(listOf("en", "he", "tr"), locales.map { it.tag })
        assertTrue(locales.first { it.tag == "he" }.hasAndroidResources)
        assertFalse(locales.first { it.tag == "he" }.hasNextDnsCatalog)
        assertFalse(locales.first { it.tag == "tr" }.hasAndroidResources)
        assertTrue(locales.first { it.tag == "tr" }.hasNextDnsCatalog)
    }

    @Test
    fun `unsupported system locale falls back to english when no app locale is selected`() {
        assertEquals(
            "en",
            resolveSelectedLocaleTag(
                requestedTag = null,
                configurationTag = "de-DE",
                supportedAndroidTags = setOf("en", "he"),
            ),
        )
        assertEquals(
            "he",
            resolveSelectedLocaleTag(
                requestedTag = "iw",
                configurationTag = "en",
                supportedAndroidTags = setOf("en", "he"),
            ),
        )
    }

    @Test
    fun `generated locale config entries support arbitrary languages and regions`() {
        assertEquals(
            setOf("de", "en", "fr", "pt-BR", "zh-Hant"),
            normalizeSupportedLocaleTags(listOf("fr", "en", "pt_br", "de", "zh-Hant", "fr")),
        )
    }

    @Test
    fun `only explicitly released locales with both catalogs are user visible`() {
        val discovered = buildLocaleDescriptors(
            androidLocaleTags = setOf("en", "he", "de"),
            nextDnsAssetNames = listOf("en.json", "de.json", "tr.json"),
        )

        val visible = releaseReadyLocaleDescriptors(
            discovered = discovered,
            releasedTags = setOf("en", "he", "tr"),
        )

        assertEquals(listOf("en"), visible.map { it.tag })
    }
}
