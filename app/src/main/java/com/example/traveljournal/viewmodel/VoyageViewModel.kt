package com.example.traveljournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveljournal.data.model.VoyageEntity
import com.example.traveljournal.data.repository.TravelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VoyageViewModel(private val repository: TravelRepository) : ViewModel() {
    val voyages: StateFlow<List<VoyageEntity>> = repository.getAllVoyages()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addVoyage(title: String, description: String? = null, thumbnailPath: String? = null) {
        viewModelScope.launch {
            repository.insertVoyage(VoyageEntity(title = title, description = description, thumbnailPath = thumbnailPath))
        }
    }

    fun updateVoyage(voyage: VoyageEntity) {
        viewModelScope.launch {
            repository.updateVoyage(voyage)
        }
    }

    fun deleteVoyage(voyage: VoyageEntity) {
        viewModelScope.launch {
            repository.deleteVoyageWithSouvenirs(voyage)
        }
    }
}
