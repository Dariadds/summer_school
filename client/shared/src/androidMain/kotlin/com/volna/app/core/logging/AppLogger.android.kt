package com.volna.app.core.logging

import timber.log.Timber

actual object AppLogger {
    actual fun e(throwable: Throwable?, message: String) {
        Timber.e(throwable, message)
    }
}
