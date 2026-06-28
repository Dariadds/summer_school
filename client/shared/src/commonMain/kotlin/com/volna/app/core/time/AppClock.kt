package com.volna.app.core.time

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

fun interface AppClock {
    fun now(): Instant
}

object SystemAppClock : AppClock {
    override fun now(): Instant = Clock.System.now()
}
