package com.volna.app.notifications

import android.content.Context
import android.content.SharedPreferences

actual object PlatformPushPermissionGateway : PushPermissionGateway {
    actual override suspend fun requestPermission(): PushPermissionStatus = PushPermissionStatus.NotDetermined
}

actual object PlatformPushPermissionFlagStore : PushPermissionFlagStore {
    private var preferences: SharedPreferences? = null
    private var fallbackRequested: Boolean = false

    fun initialize(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (fallbackRequested) {
            preferences?.edit()?.putBoolean(KEY_REQUESTED, true)?.apply()
            fallbackRequested = false
        }
    }

    actual override suspend fun wasRequested(): Boolean =
        preferences?.getBoolean(KEY_REQUESTED, false) ?: fallbackRequested

    actual override suspend fun markRequested() {
        val currentPreferences = preferences
        if (currentPreferences == null) {
            fallbackRequested = true
        } else {
            currentPreferences.edit().putBoolean(KEY_REQUESTED, true).apply()
        }
    }

    private const val PREFERENCES_NAME = "volna_notifications"
    private const val KEY_REQUESTED = "push_permission_requested"
}
