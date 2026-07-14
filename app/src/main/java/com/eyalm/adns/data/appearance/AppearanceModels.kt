package com.eyalm.adns.data.appearance

enum class DarkModePreference {
    System,
    Light,
    Dark;

    companion object {
        fun fromStored(value: String?): DarkModePreference =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: System
    }
}

enum class ColorSchemePreference {
    Adns,
    SystemDynamic;

    companion object {
        fun fromStored(value: String?): ColorSchemePreference =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Adns
    }
}

data class AppearancePreferences(
    val darkMode: DarkModePreference = DarkModePreference.System,
    val colorScheme: ColorSchemePreference = ColorSchemePreference.Adns,
)

fun resolveDarkTheme(
    preference: DarkModePreference,
    systemDark: Boolean,
): Boolean = when (preference) {
    DarkModePreference.System -> systemDark
    DarkModePreference.Light -> false
    DarkModePreference.Dark -> true
}

fun supportsDynamicColor(sdkInt: Int): Boolean = sdkInt >= 31
