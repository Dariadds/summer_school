package com.volna.app.core.logging

expect object AppLogger {
    fun e(throwable: Throwable?, message: String)
}
