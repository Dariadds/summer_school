package com.volna.app.core.storage

expect object PlatformKeyValueStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}
