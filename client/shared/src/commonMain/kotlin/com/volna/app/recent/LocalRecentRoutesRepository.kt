package com.volna.app.recent

import com.volna.app.core.storage.PlatformKeyValueStorage
import com.volna.app.domain.model.RecentRoute
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class LocalRecentRoutesRepository(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : RecentRoutesRepository {
    private val key = "volna_recent_routes_v1"
    private val maxSize = 5

    private fun loadAll(): MutableList<RecentRoute> {
        val raw = PlatformKeyValueStorage.getString(key) ?: return mutableListOf()
        return try {
            json.decodeFromString(ListSerializer(RecentRoute.serializer()), raw).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveAll(list: List<RecentRoute>) {
        val raw = json.encodeToString(ListSerializer(RecentRoute.serializer()), list)
        PlatformKeyValueStorage.putString(key, raw)
    }

    override suspend fun addRecent(route: RecentRoute): Result<Unit> {
        return try {
            val all = loadAll()
            all.removeAll { it.slotId == route.slotId }
            all.add(0, route)
            saveAll(all.take(maxSize))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentRoutes(limit: Int): Result<List<RecentRoute>> {
        return try {
            val all = loadAll()
            Result.success(all.take(limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearRecent(): Result<Unit> {
        return try {
            PlatformKeyValueStorage.remove(key)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
