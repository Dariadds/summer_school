package com.volna.app.ratings

import com.volna.app.core.storage.PlatformKeyValueStorage
import com.volna.app.domain.model.InstructorRating
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class LocalRatingsRepository(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : RatingsRepository {
    private val key = "volna_ratings_v1"

    private fun loadAll(): MutableList<InstructorRating> {
        val raw = PlatformKeyValueStorage.getString(key) ?: return mutableListOf()
        return try {
            json.decodeFromString(ListSerializer(InstructorRating.serializer()), raw).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveAll(list: List<InstructorRating>) {
        val raw = json.encodeToString(ListSerializer(InstructorRating.serializer()), list)
        PlatformKeyValueStorage.putString(key, raw)
    }

    override suspend fun submitRating(rating: InstructorRating): Result<Unit> {
        return try {
            val all = loadAll()
            if (all.none { it.bookingId == rating.bookingId }) {
                all.add(0, rating)
                saveAll(all.take(100))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRatingForBooking(bookingId: String): InstructorRating? {
        return loadAll().firstOrNull { it.bookingId == bookingId }
    }
}
