package com.kernelpanic.baladdns.data.provider

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesProviderSelectionStore(
    private val preferences: SharedPreferences,
) : ProviderSelectionStore {
    override fun read(): LegacyProviderSnapshot =
        ProviderSelectionSnapshotCodec.decode(preferences.all)

    override fun write(result: ProviderSelectionMigrationResult) {
        val editor = preferences.edit()
        ProviderSelectionSnapshotCodec.encode(result).forEach { (key, value) ->
            when (value) {
                null -> editor.remove(key)
                is Int -> editor.putInt(key, value)
                is String -> editor.putString(key, value)
                else -> error("Unsupported provider preference value for $key")
            }
        }
        editor.apply()
    }
}

object ProviderSelectionRepositories {
    @Volatile
    private var instance: ProviderSelectionRepository? = null

    fun getInstance(context: Context): ProviderSelectionRepository =
        instance ?: synchronized(this) {
            instance ?: ProviderSelectionRepository(
                store = SharedPreferencesProviderSelectionStore(
                    context.applicationContext.getSharedPreferences(
                        "adns_settings",
                        Context.MODE_PRIVATE,
                    )
                )
            ).also { instance = it }
        }
}
