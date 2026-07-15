package com.kernelpanic.baladdns.data.dns

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

class AndroidPrivateDnsSettings(
    private val resolver: ContentResolver,
) : PrivateDnsSettings {
    override fun readMode(): String? =
        Settings.Global.getString(resolver, MODE_KEY)

    override fun readSpecifier(): String? =
        Settings.Global.getString(resolver, SPECIFIER_KEY)

    override fun writeMode(value: String): Boolean =
        Settings.Global.putString(resolver, MODE_KEY, value)

    override fun writeSpecifier(value: String): Boolean =
        Settings.Global.putString(resolver, SPECIFIER_KEY, value)

    companion object {
        const val MODE_KEY = "private_dns_mode"
        const val SPECIFIER_KEY = "private_dns_specifier"
    }
}

class SharedPreferencesDnsDisableBehaviorStore(
    private val preferences: SharedPreferences,
) : DnsDisableBehaviorStore {
    override fun read(): String? = preferences.getString(KEY, null)

    override fun write(value: String) {
        preferences.edit().putString(KEY, value).apply()
    }

    companion object {
        const val KEY = "dns_disable_behavior"
    }
}

object DnsDisableBehaviorRepositories {
    @Volatile
    private var instance: DnsDisableBehaviorRepository? = null

    fun getInstance(context: Context): DnsDisableBehaviorRepository =
        instance ?: synchronized(this) {
            instance ?: DnsDisableBehaviorRepository(
                SharedPreferencesDnsDisableBehaviorStore(
                    context.applicationContext.getSharedPreferences(
                        "adns_settings",
                        Context.MODE_PRIVATE,
                    )
                )
            ).also { instance = it }
        }
}
