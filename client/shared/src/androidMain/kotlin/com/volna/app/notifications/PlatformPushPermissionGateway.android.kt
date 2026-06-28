package com.volna.app.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build

actual object PlatformPushPermissionGateway : PushPermissionGateway {
    private var activity: Activity? = null

    fun initialize(activity: Activity) {
        this.activity = activity
    }

    actual override suspend fun requestPermission(): PushPermissionStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return PushPermissionStatus.Authorized
        }

        val currentActivity = activity ?: return PushPermissionStatus.NotDetermined
        if (
            currentActivity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return PushPermissionStatus.Authorized
        }

        currentActivity.requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE_POST_NOTIFICATIONS,
        )
        return PushPermissionStatus.NotDetermined
    }

    private const val REQUEST_CODE_POST_NOTIFICATIONS = 7001
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
