package com.example.traveljournal.data.local

import androidx.room.*
import com.example.traveljournal.data.model.SouvenirEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SouvenirDao {
    @Query("SELECT * FROM souvenirs WHERE voyageId = :voyageId ORDER BY createdAt DESC")
    fun getSouvenirsForVoyage(voyageId: Long): Flow<List<SouvenirEntity>>

    @Query("SELECT * FROM souvenirs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SouvenirEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSouvenir(souvenir: SouvenirEntity): Long

    @Update
    suspend fun update(souvenir: SouvenirEntity)

    @Delete
    suspend fun delete(souvenir: SouvenirEntity)

    @Query("DELETE FROM souvenirs WHERE voyageId = :voyageId")
    suspend fun deleteByVoyageId(voyageId: Long)
}
