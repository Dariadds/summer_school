package com.volna.app.notifications

import kotlinx.browser.localStorage

actual object PlatformPushPermissionGateway : PushPermissionGateway {
    actual override suspend fun requestPermission(): PushPermissionStatus = PushPermissionStatus.NotDetermined
}

actual object PlatformPushPermissionFlagStore : PushPermissionFlagStore {
    actual override suspend fun wasRequested(): Boolean =
        localStorage.getItem(KEY_REQUESTED) == "true"

    actual override suspend fun markRequested() {
        localStorage.setItem(KEY_REQUESTED, "true")
    }

    private const val KEY_REQUESTED = "volna_push_permission_requested"
}
