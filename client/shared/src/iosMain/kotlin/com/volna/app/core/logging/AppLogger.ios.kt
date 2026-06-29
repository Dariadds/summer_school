package com.volna.app.core.logging

actual object AppLogger {
    actual fun e(throwable: Throwable?, message: String) {
        println("E: $message ${throwable?.message.orEmpty()}")
    }
}
