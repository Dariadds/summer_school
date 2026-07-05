package com.volna.app.favorites

import com.volna.app.core.storage.PlatformKeyValueStorage
import com.volna.app.domain.model.FavoriteRoute
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class LocalFavoritesRepository(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : FavoritesRepository {
    private val key = "volna_favorites_v1"

    private fun loadAll(): MutableList<FavoriteRoute> {
        val raw = PlatformKeyValueStorage.getString(key) ?: return mutableListOf()
        return try {
            json.decodeFromString(ListSerializer(FavoriteRoute.serializer()), raw).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveAll(list: List<FavoriteRoute>) {
        val raw = json.encodeToString(ListSerializer(FavoriteRoute.serializer()), list)
        PlatformKeyValueStorage.putString(key, raw)
    }

    override suspend fun addFavorite(route: FavoriteRoute): Result<Unit> {
        return try {
            val all = loadAll().toMutableList()
            if (all.none { it.slotId == route.slotId }) {
                all.add(route)
                saveAll(all)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFavorite(slotId: String): Result<Unit> {
        return try {
            val all = loadAll().filterNot { it.slotId == slotId }
            saveAll(all)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFavorites(): Result<List<FavoriteRoute>> {
        return try {
            Result.success(loadAll())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isFavorite(slotId: String): Result<Boolean> {
        return try {
            val all = loadAll()
            Result.success(all.any { it.slotId == slotId })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
