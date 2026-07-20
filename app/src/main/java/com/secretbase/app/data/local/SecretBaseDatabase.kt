package com.secretbase.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cache_entries")
data class CacheEntryEntity(
    @PrimaryKey val cacheKey: String,
    val payload: String,
    val updatedAt: Long,
)

@Entity(
    tableName = "pending_operations",
    indices = [
        Index(value = ["scopeId", "createdAt"]),
        Index(value = ["dedupeKey"], unique = true),
    ],
)
data class PendingOperationEntity(
    @PrimaryKey val id: String,
    val scopeId: String,
    val module: String,
    val operation: String,
    val entityId: String,
    val payload: String,
    val dedupeKey: String,
    val createdAt: Long,
    val attemptCount: Int = 0,
    val lastError: String? = null,
)

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val scopeId: String,
    val isSyncing: Boolean = false,
    val realtimeConnected: Boolean = false,
    val lastSyncedAt: Long? = null,
    val lastError: String? = null,
)

@Dao
interface CacheEntryDao {
    @Query("SELECT * FROM cache_entries WHERE cacheKey = :cacheKey")
    fun observe(cacheKey: String): Flow<CacheEntryEntity?>

    @Query("SELECT * FROM cache_entries WHERE cacheKey = :cacheKey")
    suspend fun get(cacheKey: String): CacheEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CacheEntryEntity)

    @Query("DELETE FROM cache_entries WHERE cacheKey = :cacheKey")
    suspend fun delete(cacheKey: String)
}

@Dao
interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations WHERE scopeId = :scopeId ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPending(scopeId: String, limit: Int = 100): List<PendingOperationEntity>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE scopeId = :scopeId")
    fun observeCount(scopeId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE scopeId = :scopeId AND module = :module")
    suspend fun countForModule(scopeId: String, module: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(operation: PendingOperationEntity)

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun delete(id: String)

    @Query(
        "UPDATE pending_operations SET attemptCount = attemptCount + 1, lastError = :message " +
            "WHERE id = :id",
    )
    suspend fun recordFailure(id: String, message: String)
}

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE scopeId = :scopeId")
    fun observe(scopeId: String): Flow<SyncMetadataEntity?>

    @Query("SELECT * FROM sync_metadata WHERE scopeId = :scopeId")
    suspend fun get(scopeId: String): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadataEntity)
}

@Database(
    entities = [CacheEntryEntity::class, PendingOperationEntity::class, SyncMetadataEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SecretBaseDatabase : RoomDatabase() {
    abstract fun cacheEntries(): CacheEntryDao
    abstract fun pendingOperations(): PendingOperationDao
    abstract fun syncMetadata(): SyncMetadataDao

    companion object {
        @Volatile
        private var instance: SecretBaseDatabase? = null

        fun get(context: Context): SecretBaseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SecretBaseDatabase::class.java,
                    "secret-base-cache.db",
                ).build().also { instance = it }
            }
    }
}
