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

/**
 * LOGIC-007 local flag boundary.
 *
 * Current implementation mirrors existing session storage placeholders and is
 * process-local until platform persistent storage is wired.
 */
object InMemoryPushPermissionFlagStore : PushPermissionFlagStore {
    private var requested: Boolean = false

    override suspend fun wasRequested(): Boolean = requested

    override suspend fun markRequested() {
        requested = true
    }
}
