package com.volna.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class InstructorRating(
    val bookingId: String,
    val instructorId: String,
    val rating: Int,
    val comment: String? = null,
    val createdAt: Long,
)
