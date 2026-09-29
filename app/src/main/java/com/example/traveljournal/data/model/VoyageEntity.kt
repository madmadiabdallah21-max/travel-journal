package com.example.traveljournal.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voyages")
data class VoyageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null
)
