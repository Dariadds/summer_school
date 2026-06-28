package com.volna.app.notifications

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSUserDefaults
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

actual object PlatformPushPermissionGateway : PushPermissionGateway {
    actual override suspend fun requestPermission(): PushPermissionStatus =
        suspendCancellableCoroutine { continuation ->
            val options = UNAuthorizationOptionAlert or
                UNAuthorizationOptionSound or
                UNAuthorizationOptionBadge
            UNUserNotificationCenter.currentNotificationCenter()
                .requestAuthorizationWithOptions(options) { granted, _ ->
                    if (continuation.isActive) {
                        continuation.resume(
                            if (granted) {
                                PushPermissionStatus.Authorized
                            } else {
                                PushPermissionStatus.Denied
                            },
                        )
                    }
                }
        }
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
