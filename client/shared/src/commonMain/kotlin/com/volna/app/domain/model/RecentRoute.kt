package com.volna.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class RecentRoute(
    val slotId: String,
    val routeName: String,
    val routeDescription: String,
    val price: Int,
    val instructorName: String,
    val viewedAt: Instant,
)
