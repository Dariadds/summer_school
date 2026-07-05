package com.volna.app.favorites

import com.volna.app.domain.model.FavoriteRoute

interface FavoritesRepository {
    suspend fun addFavorite(route: FavoriteRoute): Result<Unit>
    suspend fun removeFavorite(slotId: String): Result<Unit>
    suspend fun getFavorites(): Result<List<FavoriteRoute>>
    suspend fun isFavorite(slotId: String): Result<Boolean>
}
