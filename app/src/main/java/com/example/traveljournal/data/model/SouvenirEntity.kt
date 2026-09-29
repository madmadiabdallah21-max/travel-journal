package com.example.traveljournal.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "souvenirs")
data class SouvenirEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaPath: String,
    val mediaType: String, // "PHOTO" or "VIDEO"
    val title: String? = null,
    val description: String? = null,
    val voyageId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
