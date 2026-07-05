package com.volna.app.ratings

import com.volna.app.domain.model.InstructorRating

interface RatingsRepository {
    suspend fun submitRating(rating: InstructorRating): Result<Unit>
    suspend fun getRatingForBooking(bookingId: String): InstructorRating?
}
