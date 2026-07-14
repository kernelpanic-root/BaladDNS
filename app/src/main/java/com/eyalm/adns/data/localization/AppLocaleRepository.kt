package com.eyalm.adns.data.localization

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.eyalm.adns.R
import com.eyalm.adns.data.Locales
import java.util.Locale
import org.xmlpull.v1.XmlPullParser

class AppLocaleRepository(private val context: Context) {
    fun selectedTag(): String {
        val selected = AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag()
        val releasedTags = supportedLocales().mapTo(mutableSetOf()) { it.tag }
        return resolveSelectedLocaleTag(
            requestedTag = selected,
            configurationTag = context.resources.configuration.locales[0]?.toLanguageTag(),
            supportedAndroidTags = releasedTags,
        )
    }

    fun supportedLocales(): List<LocaleDescriptor> {
        val releasedTags = context.releasedAndroidLocaleTags()
        return releaseReadyLocaleDescriptors(
            discovered = buildLocaleDescriptors(
                androidLocaleTags = releasedTags,
                nextDnsAssetNames = context.assets
                    .list(NEXTDNS_LOCALE_DIRECTORY)
                    ?.toList()
                    .orEmpty(),
            ),
            releasedTags = releasedTags,
        )
    }

    fun select(tag: String) {
        val canonicalTag = canonicalLocaleTag(tag)
        if (supportedLocales().none { it.tag == canonicalTag }) return
        Locales.select(context, canonicalTag)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(canonicalTag),
        )
    }

    companion object {
        private const val NEXTDNS_LOCALE_DIRECTORY = "locales/nextdns"
    }
}

private fun Context.releasedAndroidLocaleTags(): Set<String> {
    val parser = resources.getXml(R.xml.locales_config)
    return try {
        buildList {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "locale") {
                    parser.getAttributeValue(ANDROID_NAMESPACE, "name")?.let(::add)
                }
                event = parser.next()
            }
        }.let(::normalizeSupportedLocaleTags)
    } finally {
        parser.close()
    }
}

fun localizedContext(base: Context): Context {
    val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: return base
    val configuration = Configuration(base.resources.configuration).apply {
        val javaLocale = Locale.forLanguageTag(locale.toLanguageTag())
        setLocale(javaLocale)
        setLayoutDirection(javaLocale)
    }
    return base.createConfigurationContext(configuration)
}

private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
