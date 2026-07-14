package com.eyalm.adns.data.localization

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory

class StringResourceContractTest {
    @Test
    fun `android catalogs use typographic ellipses`() {
        androidStringCatalogs().forEach { catalog ->
            assertFalse(
                "${catalog.path} contains three periods instead of an ellipsis",
                catalog.readText().contains("..."),
            )
        }
    }

    @Test
    fun `recreation active count is a plural resource`() {
        val english = androidStringCatalogs().first { it.path.replace('\\', '/').contains("/values/") }

        assertTrue(
            english.readText().contains("<plurals name=\"recreation_time_active_count\">")
        )
    }

    @Test
    fun `localized catalogs do not redefine non-translatable strings`() {
        val catalogs = androidStringCatalogs()
        val english = catalogs.first { it.path.replace('\\', '/').contains("/values/") }
        val nonTranslatableNames = stringElements(english)
            .filter { it.getAttribute("translatable") == "false" }
            .mapTo(mutableSetOf()) { it.getAttribute("name") }

        catalogs.filterNot { it == english }.forEach { catalog ->
            val localizedNames = stringElements(catalog)
                .mapTo(mutableSetOf()) { it.getAttribute("name") }
            assertTrue(
                "${catalog.path} redefines non-translatable strings: " +
                    nonTranslatableNames.intersect(localizedNames).sorted().joinToString(),
                nonTranslatableNames.intersect(localizedNames).isEmpty(),
            )
        }
    }

    private fun androidStringCatalogs(): List<File> {
        val sourceRoot = sequenceOf(File("src/main/res"), File("app/src/main/res"))
            .first(File::isDirectory)
        return sourceRoot
            .listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values") }
            .map { File(it, "strings.xml") }
            .filter(File::isFile)
    }

    private fun stringElements(catalog: File) =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(catalog)
            .getElementsByTagName("string")
            .let { nodes ->
                (0 until nodes.length).map { nodes.item(it) as org.w3c.dom.Element }
            }
}
