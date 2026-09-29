package com.example.traveljournal.data.repository

import com.example.traveljournal.data.local.SouvenirDao
import com.example.traveljournal.data.local.VoyageDao
import com.example.traveljournal.data.model.SouvenirEntity
import com.example.traveljournal.data.model.VoyageEntity
import kotlinx.coroutines.flow.Flow

class TravelRepository(
    private val voyageDao: VoyageDao,
    private val souvenirDao: SouvenirDao
) {
    fun getAllVoyages(): Flow<List<VoyageEntity>> = voyageDao.getAll()

    suspend fun getVoyageById(id: Long): VoyageEntity? = voyageDao.getById(id)

    suspend fun insertVoyage(v: VoyageEntity): Long = voyageDao.insert(v)

    suspend fun updateVoyage(v: VoyageEntity) = voyageDao.update(v)

    suspend fun deleteVoyage(v: VoyageEntity) = voyageDao.delete(v)

    suspend fun deleteVoyageWithSouvenirs(v: VoyageEntity) = voyageDao.deleteWithSouvenirs(v)

    fun getSouvenirsForVoyage(voyageId: Long): Flow<List<SouvenirEntity>> = souvenirDao.getSouvenirsForVoyage(voyageId)

    suspend fun getSouvenirById(id: Long): SouvenirEntity? = souvenirDao.getById(id)

    suspend fun insertSouvenir(s: SouvenirEntity): Long = souvenirDao.insertSouvenir(s)

    suspend fun updateSouvenir(s: SouvenirEntity) = souvenirDao.update(s)

    suspend fun deleteSouvenir(s: SouvenirEntity) = souvenirDao.delete(s)
}
