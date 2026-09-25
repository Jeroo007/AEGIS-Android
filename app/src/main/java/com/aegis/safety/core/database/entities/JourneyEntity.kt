package com.aegis.safety.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journeys")
data class JourneyEntity(
    @PrimaryKey val id: String,
    val contactIds: String,
    val synced: Boolean
)
