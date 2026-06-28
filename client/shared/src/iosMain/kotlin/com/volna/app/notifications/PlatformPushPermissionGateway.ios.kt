package com.volna.app.notifications

actual object PlatformPushPermissionGateway : PushPermissionGateway {
    actual override suspend fun requestPermission(): PushPermissionStatus = PushPermissionStatus.NotDetermined
}
