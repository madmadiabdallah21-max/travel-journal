package com.example.traveljournal.data.local

import androidx.room.*
import com.example.traveljournal.data.model.VoyageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoyageDao {
    @Query("SELECT * FROM voyages ORDER BY createdAt DESC")
    fun getAll(): Flow<List<VoyageEntity>>

    @Query("SELECT * FROM voyages WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VoyageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(voyage: VoyageEntity): Long

    @Update
    suspend fun update(voyage: VoyageEntity)

    @Delete
    suspend fun delete(voyage: VoyageEntity)

    @Query("DELETE FROM souvenirs WHERE voyageId = :voyageId")
    suspend fun deleteSouvenirsForVoyage(voyageId: Long)

    @Transaction
    suspend fun deleteWithSouvenirs(voyage: VoyageEntity) {
        deleteSouvenirsForVoyage(voyage.id)
        delete(voyage)
    }
}
