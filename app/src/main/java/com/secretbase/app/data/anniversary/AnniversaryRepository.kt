package com.secretbase.app.data.anniversary

import kotlinx.coroutines.flow.Flow

interface AnniversaryRepository {
    fun observeAnniversaries(): Flow<List<Anniversary>>

    suspend fun addAnniversary(item: Anniversary): Result<Unit>

    suspend fun updateAnniversary(item: Anniversary): Result<Unit>

    suspend fun deleteAnniversary(id: String): Result<Unit>
}
