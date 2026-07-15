package com.kernelpanic.baladdns.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kernelpanic.baladdns.data.appearance.AppearanceRepository
import com.kernelpanic.baladdns.data.appearance.ColorSchemePreference
import com.kernelpanic.baladdns.data.appearance.resolveDarkTheme

@Composable
fun AdnsTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) {
        AppearanceRepository.getInstance(context.applicationContext)
    }
    val preferences by repository.state.collectAsState()
    val darkTheme = resolveDarkTheme(
        preference = preferences.darkMode,
        systemDark = isSystemInDarkTheme(),
    )
    val colorScheme = if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        preferences.colorScheme == ColorSchemePreference.SystemDynamic
    ) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) {
        AdnsDarkColorScheme
    } else {
        AdnsLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
