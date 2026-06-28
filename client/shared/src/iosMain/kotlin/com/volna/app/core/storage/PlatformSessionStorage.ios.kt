package com.volna.app.core.storage

actual object PlatformSessionStorage : SessionStorage {
    private var token: String? = null

    override suspend fun readToken(): String? = token

    override suspend fun writeToken(token: String) {
        this.token = token
    }

    override suspend fun clearToken() {
        token = null
    }
}
