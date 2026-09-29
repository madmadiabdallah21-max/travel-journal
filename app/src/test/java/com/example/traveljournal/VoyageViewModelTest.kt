package com.example.traveljournal

import com.example.traveljournal.data.local.SouvenirDao
import com.example.traveljournal.data.local.VoyageDao
import com.example.traveljournal.data.model.SouvenirEntity
import com.example.traveljournal.data.model.VoyageEntity
import com.example.traveljournal.data.repository.TravelRepository
import com.example.traveljournal.viewmodel.VoyageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoyageViewModelTest {
    @Test
    fun addVoyage_publishesInsertedVoyage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = VoyageViewModel(
                TravelRepository(FakeVoyageDao(), FakeSouvenirDao())
            )

            viewModel.addVoyage("Test Voyage", "Description")
            advanceUntilIdle()

            assertEquals(1, viewModel.voyages.value.size)
            assertEquals("Test Voyage", viewModel.voyages.value.single().title)
            assertEquals("Description", viewModel.voyages.value.single().description)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class FakeVoyageDao : VoyageDao {
    private val voyages = MutableStateFlow<List<VoyageEntity>>(emptyList())

    override fun getAll(): Flow<List<VoyageEntity>> = voyages

    override suspend fun getById(id: Long): VoyageEntity? =
        voyages.value.find { it.id == id }

    override suspend fun insert(voyage: VoyageEntity): Long {
        val id = voyage.id.takeIf { it != 0L }
            ?: ((voyages.value.maxOfOrNull { it.id } ?: 0L) + 1L)
        voyages.value = voyages.value + voyage.copy(id = id)
        return id
    }

    override suspend fun update(voyage: VoyageEntity) {
        voyages.value = voyages.value.map { if (it.id == voyage.id) voyage else it }
    }

    override suspend fun delete(voyage: VoyageEntity) {
        voyages.value = voyages.value.filterNot { it.id == voyage.id }
    }

    override suspend fun deleteSouvenirsForVoyage(voyageId: Long) = Unit
}

private class FakeSouvenirDao : SouvenirDao {
    override fun getSouvenirsForVoyage(voyageId: Long): Flow<List<SouvenirEntity>> =
        emptyFlow()

    override suspend fun getById(id: Long): SouvenirEntity? = null

    override suspend fun insertSouvenir(souvenir: SouvenirEntity): Long = souvenir.id

    override suspend fun update(souvenir: SouvenirEntity) = Unit

    override suspend fun delete(souvenir: SouvenirEntity) = Unit

    override suspend fun deleteByVoyageId(voyageId: Long) = Unit
}
