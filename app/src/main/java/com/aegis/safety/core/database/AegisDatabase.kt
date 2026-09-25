package com.aegis.safety.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aegis.safety.core.database.entities.*

@Database(
    entities = [
        IncidentEntity::class,
        ContactEntity::class,
        PendingEventEntity::class,
        LocationEntity::class,
        SafetyTimerEntity::class,
        NotificationEntity::class,
        JourneyEntity::class,
        ChatMessageEntity::class    // NEW
    ],
    version = 3,                     // bumped from 2
    exportSchema = false
)
abstract class AegisDatabase : RoomDatabase() {
    abstract fun aegisDao(): AegisDao
}