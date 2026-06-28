package com.volna.app.notifications

enum class PushPermissionStatus {
    NotDetermined,
    Authorized,
    Denied,
}

interface PushPermissionGateway {
    suspend fun requestPermission(): PushPermissionStatus
}

interface PushPermissionFlagStore {
    suspend fun wasRequested(): Boolean
    suspend fun markRequested()
}

expect object PlatformPushPermissionGateway : PushPermissionGateway {
    override suspend fun requestPermission(): PushPermissionStatus
}

expect object PlatformPushPermissionFlagStore : PushPermissionFlagStore {
    override suspend fun wasRequested(): Boolean
    override suspend fun markRequested()
}
