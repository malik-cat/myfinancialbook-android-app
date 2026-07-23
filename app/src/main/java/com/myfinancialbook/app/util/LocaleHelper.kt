package com.myfinancialbook.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun setLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun getLanguage(context: Context): String {
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("language", "en") ?: "en"
    }

    fun persistLanguage(context: Context, language: String) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit().putString("language", language).apply()
    }
}
