package com.volna.app.core.storage

/**
 * LOGIC-001: platform boundary for bearer token persistence.
 *
 * Current actual implementations are intentionally minimal placeholders until
 * Android Keystore, iOS Keychain, and Web storage policies are wired.
 */
expect object PlatformSessionStorage : SessionStorage {
    override suspend fun readToken(): String?
    override suspend fun writeToken(token: String)
    override suspend fun clearToken()
}

interface SessionStorage {
    suspend fun readToken(): String?
    suspend fun writeToken(token: String)
    suspend fun clearToken()
}
