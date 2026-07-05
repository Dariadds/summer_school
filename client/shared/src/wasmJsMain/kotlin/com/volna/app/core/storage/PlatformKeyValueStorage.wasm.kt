package com.volna.app.core.storage

import kotlinx.browser.localStorage

actual object PlatformKeyValueStorage {
    actual fun getString(key: String): String? = localStorage.getItem(key)

    actual fun putString(key: String, value: String) {
        localStorage.setItem(key, value)
    }

    actual fun remove(key: String) {
        localStorage.removeItem(key)
    }
}
