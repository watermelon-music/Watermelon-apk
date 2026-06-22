package com.watermelon.core.designsystem.theme

import android.content.Context

object ThemeManager {
    private const val PREFS = "watermelon_settings"
    private const val KEY_THEME = "theme_mode"

    fun save(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, mode).apply()
    }

    fun get(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, "system") ?: "system"
    }
}
