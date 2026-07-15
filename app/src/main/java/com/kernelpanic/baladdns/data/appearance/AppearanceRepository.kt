package com.kernelpanic.baladdns.data.appearance

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppearanceRepository private constructor(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    private val mutableState = MutableStateFlow(readPreferences())
    val state: StateFlow<AppearancePreferences> = mutableState.asStateFlow()

    fun setDarkMode(value: DarkModePreference) {
        preferences.edit { putString(KEY_DARK_MODE, value.name) }
        mutableState.value = mutableState.value.copy(darkMode = value)
    }

    fun setColorScheme(value: ColorSchemePreference) {
        preferences.edit { putString(KEY_COLOR_SCHEME, value.name) }
        mutableState.value = mutableState.value.copy(colorScheme = value)
    }

    private fun readPreferences(): AppearancePreferences = AppearancePreferences(
        darkMode = DarkModePreference.fromStored(preferences.getString(KEY_DARK_MODE, null)),
        colorScheme = ColorSchemePreference.fromStored(
            preferences.getString(KEY_COLOR_SCHEME, null),
        ),
    )

    companion object {
        private const val PREFERENCES_NAME = "appearance_preferences"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_COLOR_SCHEME = "color_scheme"

        @Volatile
        private var instance: AppearanceRepository? = null

        fun getInstance(context: Context): AppearanceRepository = instance ?: synchronized(this) {
            instance ?: AppearanceRepository(context).also { instance = it }
        }
    }
}
