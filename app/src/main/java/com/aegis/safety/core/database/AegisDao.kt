package com.aegis.safety.core.database

import androidx.room.*
import com.aegis.safety.core.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AegisDao {
    @Query("SELECT * FROM incidents ORDER BY createdAt DESC")
    fun observeIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE id = :id LIMIT 1")
    suspend fun getIncident(id: String): IncidentEntity?

    @Upsert suspend fun upsertIncident(entity: IncidentEntity)
    @Query("DELETE FROM incidents WHERE id = :id") suspend fun deleteIncident(id: String)

    @Query("SELECT * FROM contacts ORDER BY priority ASC, name ASC")
    fun observeContacts(): Flow<List<ContactEntity>>

    /** One-shot synchronous read — used by SOS to reliably fetch contacts. */
    @Query("SELECT * FROM contacts ORDER BY priority ASC, name ASC")
    suspend fun getContactsSnapshot(): List<ContactEntity>

    @Upsert suspend fun upsertContact(entity: ContactEntity)
    @Upsert suspend fun upsertContacts(entities: List<ContactEntity>)
    @Query("DELETE FROM contacts WHERE id = :id") suspend fun deleteContact(id: String)
    @Query("DELETE FROM contacts") suspend fun clearContacts()

    @Insert suspend fun enqueueEvent(event: PendingEventEntity): Long
    @Query("SELECT * FROM pending_events WHERE attempts < :maxAttempts ORDER BY createdAt ASC")
    suspend fun pendingEvents(maxAttempts: Int): List<PendingEventEntity>
    @Query("UPDATE pending_events SET attempts = attempts + 1, lastAttemptAt = :now, lastError = :error WHERE id = :id")
    suspend fun markAttempt(id: Long, now: Long, error: String?)
    @Query("DELETE FROM pending_events WHERE id = :id") suspend fun deletePending(id: Long)
    @Query("SELECT COUNT(*) FROM pending_events") fun observePendingCount(): Flow<Int>

    @Insert suspend fun insertLocation(location: LocationEntity): Long
    @Query("SELECT * FROM locations WHERE synced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun unsyncedLocations(limit: Int): List<LocationEntity>
    @Query("UPDATE locations SET synced = 1 WHERE id IN (:ids)")
    suspend fun markLocationsSynced(ids: List<Long>)
    @Query("DELETE FROM locations WHERE timestamp < :before AND synced = 1")
    suspend fun purgeOldLocations(before: Long)

    @Query("SELECT * FROM safety_timers WHERE status = 'ACTIVE' ORDER BY expiresAt ASC")
    fun observeActiveTimers(): Flow<List<SafetyTimerEntity>>
    @Query("SELECT * FROM safety_timers WHERE id = :id") suspend fun getTimer(id: String): SafetyTimerEntity?
    @Upsert suspend fun upsertTimer(entity: SafetyTimerEntity)
    @Query("DELETE FROM safety_timers WHERE id = :id") suspend fun deleteTimer(id: String)

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC LIMIT 200")
    fun observeNotifications(): Flow<List<NotificationEntity>>
    @Upsert suspend fun upsertNotification(entity: NotificationEntity)
    @Query("UPDATE notifications SET read = 1 WHERE id = :id") suspend fun markNotificationRead(id: String)
    @Query("UPDATE notifications SET read = 1") suspend fun markAllNotificationsRead()

    // ---- Chat ----
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeChat(sessionId: String = "default"): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentChat(sessionId: String = "default", limit: Int = 20): List<ChatMessageEntity>

    @Insert
    suspend fun insertChatMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearChat(sessionId: String = "default")
}