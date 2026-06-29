package com.volna.app.core.config

/**
 * SCR-007: app-level values that should come from build/runtime configuration.
 */
data class AppConfig(
    val rulesUrl: String? = null,
    val supportUrl: String? = null,
    val appVersion: String = "0.1.0",
    val reminderHours: Int? = null,
)
