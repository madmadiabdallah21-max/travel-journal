package com.example.traveljournal.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveljournal.data.model.SouvenirEntity
import com.example.traveljournal.data.repository.TravelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SouvenirViewModel(private val repository: TravelRepository) : ViewModel() {
    fun getSouvenirs(voyageId: Long): Flow<List<SouvenirEntity>> = repository.getSouvenirsForVoyage(voyageId)

    fun addSouvenir(voyageId: Long, mediaPath: String, mediaType: String, title: String?, description: String?) {
        viewModelScope.launch {
            val s = SouvenirEntity(mediaPath = mediaPath, mediaType = mediaType, title = title, description = description, voyageId = voyageId)
            repository.insertSouvenir(s)
        }
    }

    fun updateSouvenir(souvenir: SouvenirEntity) {
        viewModelScope.launch {
            repository.updateSouvenir(souvenir)
        }
    }

    fun deleteSouvenir(souvenir: SouvenirEntity) {
        viewModelScope.launch {
            repository.deleteSouvenir(souvenir)
        }
    }

    suspend fun getSouvenirById(id: Long): SouvenirEntity? = repository.getSouvenirById(id)
}
