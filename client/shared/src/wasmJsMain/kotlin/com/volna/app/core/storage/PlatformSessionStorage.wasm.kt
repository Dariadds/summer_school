package com.volna.app.core.storage

actual object PlatformSessionStorage : SessionStorage {
    private var token: String? = null

    actual override suspend fun readToken(): String? = token

    actual override suspend fun writeToken(token: String) {
        this.token = token
    }

    actual override suspend fun clearToken() {
        token = null
    }
}
