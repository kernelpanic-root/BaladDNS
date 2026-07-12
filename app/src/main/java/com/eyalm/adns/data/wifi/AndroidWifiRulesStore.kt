package com.eyalm.adns.data.wifi

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.eyalm.adns.data.PrivateDnsObservation

class SharedPreferencesWifiRulesStore(
    private val preferences: SharedPreferences,
) : WifiRulesStore {
    private var lastSnapshot: StoredWifiRulesSnapshot? = null

    override fun read(): StoredWifiRulesSnapshot {
        val configuration = WifiRulesConfiguration(
            ssids = preferences.getStringSet(KEY_SSIDS, emptySet())
                .orEmpty()
                .mapNotNull(WifiSsid::fromUserInput)
                .toSet(),
        )
        return StoredWifiRulesSnapshot(
            configuration = configuration,
            suspension = readSuspension(),
            relinquishedSsid = WifiSsid.fromUserInput(
                preferences.getString(KEY_RELINQUISHED_SSID, null)
            ),
        ).also { lastSnapshot = it }
    }

    override fun write(snapshot: StoredWifiRulesSnapshot) {
        val durableOwnershipChange = lastSnapshot?.let { previous ->
            previous.suspension != snapshot.suspension ||
                previous.relinquishedSsid != snapshot.relinquishedSsid
        } ?: true
        preferences.edit(commit = durableOwnershipChange) {
            putStringSet(KEY_SSIDS, snapshot.configuration.ssids.mapTo(mutableSetOf()) { it.value })
            remove(LEGACY_KEY_DISABLE_BEHAVIOR)
            if (snapshot.relinquishedSsid == null) {
                remove(KEY_RELINQUISHED_SSID)
            } else {
                putString(KEY_RELINQUISHED_SSID, snapshot.relinquishedSsid.value)
            }
            val suspension = snapshot.suspension
            if (suspension == null) {
                SUSPENSION_KEYS.forEach(::remove)
            } else {
                putString(KEY_MATCHED_SSID, suspension.matchedSsid.value)
                writeObservation(RESTORE_PREFIX, suspension.restoreTarget)
                writeObservation(APPLIED_PREFIX, suspension.stateAppliedByAdns)
                val previousApplied = suspension.previousStateAppliedByAdns
                if (previousApplied == null) {
                    remove("$PREVIOUS_APPLIED_PREFIX.type")
                    remove("$PREVIOUS_APPLIED_PREFIX.value")
                } else {
                    writeObservation(PREVIOUS_APPLIED_PREFIX, previousApplied)
                }
                putString(KEY_PHASE, suspension.phase.name)
            }
        }
        lastSnapshot = snapshot
    }

    private fun readSuspension(): WifiRuleSuspension? {
        val matched = WifiSsid.fromUserInput(preferences.getString(KEY_MATCHED_SSID, null))
            ?: return null
        val restore = readObservation(RESTORE_PREFIX) ?: return null
        val applied = readObservation(APPLIED_PREFIX) ?: return null
        val previousApplied = readObservation(PREVIOUS_APPLIED_PREFIX)
        val phase = preferences.getString(KEY_PHASE, null)
            ?.let { stored -> WifiSuspensionPhase.entries.firstOrNull { it.name == stored } }
            ?: return null
        return WifiRuleSuspension(
            matchedSsid = matched,
            restoreTarget = restore,
            stateAppliedByAdns = applied,
            phase = phase,
            previousStateAppliedByAdns = previousApplied,
        )
    }

    private fun SharedPreferences.Editor.writeObservation(
        prefix: String,
        value: PrivateDnsObservation,
    ) {
        when (value) {
            PrivateDnsObservation.Automatic -> {
                putString("$prefix.type", TYPE_AUTOMATIC)
                remove("$prefix.value")
            }
            PrivateDnsObservation.Off -> {
                putString("$prefix.type", TYPE_OFF)
                remove("$prefix.value")
            }
            PrivateDnsObservation.PermissionMissing -> {
                putString("$prefix.type", TYPE_PERMISSION_MISSING)
                remove("$prefix.value")
            }
            is PrivateDnsObservation.Hostname -> {
                putString("$prefix.type", TYPE_HOSTNAME)
                putString("$prefix.value", value.value)
            }
        }
    }

    private fun readObservation(prefix: String): PrivateDnsObservation? =
        when (preferences.getString("$prefix.type", null)) {
            TYPE_AUTOMATIC -> PrivateDnsObservation.Automatic
            TYPE_OFF -> PrivateDnsObservation.Off
            TYPE_PERMISSION_MISSING -> PrivateDnsObservation.PermissionMissing
            TYPE_HOSTNAME -> preferences.getString("$prefix.value", null)
                ?.takeUnless(String::isBlank)
                ?.let(PrivateDnsObservation::Hostname)
            else -> null
        }

    companion object {
        private const val KEY_SSIDS = "wifi_rules.ssids"
        private const val LEGACY_KEY_DISABLE_BEHAVIOR = "wifi_rules.disable_behavior"
        private const val KEY_MATCHED_SSID = "wifi_rules.suspension.ssid"
        private const val KEY_RELINQUISHED_SSID = "wifi_rules.relinquished_ssid"
        private const val RESTORE_PREFIX = "wifi_rules.suspension.restore"
        private const val APPLIED_PREFIX = "wifi_rules.suspension.applied"
        private const val PREVIOUS_APPLIED_PREFIX =
            "wifi_rules.suspension.previous_applied"
        private const val KEY_PHASE = "wifi_rules.suspension.phase"
        private const val TYPE_AUTOMATIC = "automatic"
        private const val TYPE_OFF = "off"
        private const val TYPE_PERMISSION_MISSING = "permission_missing"
        private const val TYPE_HOSTNAME = "hostname"
        private val SUSPENSION_KEYS = setOf(
            KEY_MATCHED_SSID,
            "$RESTORE_PREFIX.type",
            "$RESTORE_PREFIX.value",
            "$APPLIED_PREFIX.type",
            "$APPLIED_PREFIX.value",
            "$PREVIOUS_APPLIED_PREFIX.type",
            "$PREVIOUS_APPLIED_PREFIX.value",
            KEY_PHASE,
        )
    }
}

object WifiRulesRepositories {
    @Volatile
    private var instance: WifiRulesRepository? = null

    fun getInstance(context: Context): WifiRulesRepository = instance ?: synchronized(this) {
        instance ?: WifiRulesRepository(
            SharedPreferencesWifiRulesStore(
                context.applicationContext.getSharedPreferences(
                    "adns_settings",
                    Context.MODE_PRIVATE,
                )
            )
        ).also { instance = it }
    }
}
