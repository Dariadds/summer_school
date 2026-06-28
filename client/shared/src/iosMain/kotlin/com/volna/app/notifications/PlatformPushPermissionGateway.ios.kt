package com.volna.app.notifications

import platform.Foundation.NSUserDefaults

actual object PlatformPushPermissionGateway : PushPermissionGateway {
    actual override suspend fun requestPermission(): PushPermissionStatus = PushPermissionStatus.NotDetermined
}

actual object PlatformPushPermissionFlagStore : PushPermissionFlagStore {
    private val defaults: NSUserDefaults
        get() = NSUserDefaults.standardUserDefaults

    actual override suspend fun wasRequested(): Boolean =
        defaults.boolForKey(KEY_REQUESTED)

    actual override suspend fun markRequested() {
        defaults.setBool(true, forKey = KEY_REQUESTED)
    }

    private const val KEY_REQUESTED = "volna_push_permission_requested"
}
