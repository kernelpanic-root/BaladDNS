package com.kernelpanic.baladdns.data.activation

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import com.kernelpanic.baladdns.data.TokenManager

object ActivationSnapshotCodec {
    private const val MIGRATION_VERSION = "activation_migration_version"
    private const val ONBOARDING_COMPLETE = "onboarding_complete"
    private const val MODE = "activation_mode"
    private const val NEEDS_MODE_CHOICE = "activation_needs_mode_choice"

    fun decode(values: Map<String, *>): StoredActivationSnapshot = StoredActivationSnapshot(
        migrationVersion = values[MIGRATION_VERSION] as? Int ?: 0,
        onboardingComplete = values[ONBOARDING_COMPLETE] as? Boolean ?: false,
        mode = values[MODE] as? String,
        needsModeChoice = values[NEEDS_MODE_CHOICE] as? Boolean ?: false,
    )

    fun encode(value: StoredActivationSnapshot): Map<String, Any?> = mapOf(
        MIGRATION_VERSION to value.migrationVersion,
        ONBOARDING_COMPLETE to value.onboardingComplete,
        MODE to value.mode,
        NEEDS_MODE_CHOICE to value.needsModeChoice,
    )
}

class AndroidPermissionObserver(
    private val context: Context,
) : PermissionObserver {
    override fun current(): PermissionState = if (
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        PermissionState.Granted
    } else {
        PermissionState.Missing
    }
}

class SharedPreferencesActivationStore(
    private val preferences: SharedPreferences,
) : ActivationStore {
    override fun read(): StoredActivationSnapshot =
        ActivationSnapshotCodec.decode(preferences.all)

    override fun write(value: StoredActivationSnapshot) {
        val editor = preferences.edit()
        ActivationSnapshotCodec.encode(value).forEach { (key, storedValue) ->
            when (storedValue) {
                null -> editor.remove(key)
                is Boolean -> editor.putBoolean(key, storedValue)
                is Int -> editor.putInt(key, storedValue)
                is String -> editor.putString(key, storedValue)
                else -> error("Unsupported activation preference value for $key")
            }
        }
        editor.apply()
    }
}

object ActivationRepositories {
    @Volatile
    private var instance: ActivationRepository? = null

    fun getInstance(context: Context): ActivationRepository {
        val appContext = context.applicationContext
        return instance ?: synchronized(this) {
            instance ?: run {
                val preferences = appContext.getSharedPreferences(
                    "adns_settings",
                    Context.MODE_PRIVATE,
                )
                ActivationRepository(
                    store = SharedPreferencesActivationStore(preferences),
                    permissionObserver = AndroidPermissionObserver(appContext),
                    migrationContext = {
                        ActivationMigrationContext(
                            hasNextDnsSession = TokenManager.getInstance(appContext).hasToken(),
                            hasSelectedNextDnsProfile = !preferences
                                .getString("enhanced_url", null)
                                .isNullOrBlank(),
                        )
                    },
                )
            }.also { instance = it }
        }
    }
}
