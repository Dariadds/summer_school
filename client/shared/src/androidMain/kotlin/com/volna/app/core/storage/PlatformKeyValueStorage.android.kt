package com.volna.app.core.storage

import android.content.Context
import android.content.SharedPreferences

actual object PlatformKeyValueStorage {
    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    actual fun getString(key: String): String? = preferences?.getString(key, null)

    actual fun putString(key: String, value: String) {
        preferences?.edit()?.putString(key, value)?.apply()
    }

    actual fun remove(key: String) {
        preferences?.edit()?.remove(key)?.apply()
    }

    private const val PREFERENCES_NAME = "volna_preferences"
}
