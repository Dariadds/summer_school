package com.volna.app.recent

import com.volna.app.domain.model.RecentRoute

interface RecentRoutesRepository {
    suspend fun addRecent(route: RecentRoute): Result<Unit>
    suspend fun getRecentRoutes(limit: Int): Result<List<RecentRoute>>
    suspend fun clearRecent(): Result<Unit>
}
