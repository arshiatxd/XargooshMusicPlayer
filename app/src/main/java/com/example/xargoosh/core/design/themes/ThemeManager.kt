package com.example.xargoosh.core.design.themes

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.annotation.StringRes
import com.example.xargoosh.R

enum class AppTheme(@StringRes val displayNameRes: Int) {
    AERO(R.string.theme_black_aero),
    GREEN_VISTA(R.string.theme_green_vista),
    BLUE_WINDOWS_7(R.string.theme_windows_7),
    CRYSTAL_SKY_AERO(R.string.theme_crystal_sky),
    PEARL_AERO(R.string.theme_pearl),
    MEADOW_AERO(R.string.theme_meadow),
    FROSTED(R.string.theme_frosted),
    MINIMAL(R.string.theme_minimal),
    SUNLIT_PAPER(R.string.theme_sunlit_paper),
    LAVENDER_DAY(R.string.theme_lavender_day),
    AMOLED(R.string.theme_amoled),
    MODERN_DARK(R.string.theme_modern_dark),
    RETRO(R.string.theme_retro),
    PASTEL(R.string.theme_pastel),
    HIGH_CONTRAST(R.string.theme_high_contrast),
    DYNAMIC_SYSTEM(R.string.theme_dynamic_system);

    val isAero: Boolean
        get() = this == AERO || this == GREEN_VISTA || this == BLUE_WINDOWS_7 ||
            this == CRYSTAL_SKY_AERO || this == PEARL_AERO || this == MEADOW_AERO

    val usesGlass: Boolean
        get() = isAero || this == RETRO

    val isLight: Boolean
        get() = this == FROSTED || this == MINIMAL || this == PASTEL ||
            this == CRYSTAL_SKY_AERO || this == PEARL_AERO || this == MEADOW_AERO ||
            this == SUNLIT_PAPER || this == LAVENDER_DAY
}

object ThemeManager {
    private val _currentTheme = MutableStateFlow(AppTheme.AERO)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val savedTheme = prefs.getString("theme", AppTheme.AERO.name) ?: AppTheme.AERO.name
        _currentTheme.value = try { AppTheme.valueOf(savedTheme) } catch (e: Exception) { AppTheme.AERO }
    }

    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
    }

    fun setThemeAndSave(context: Context, theme: AppTheme) {
        _currentTheme.value = theme
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE).edit()
            .putString("theme", theme.name).apply()
    }
}
