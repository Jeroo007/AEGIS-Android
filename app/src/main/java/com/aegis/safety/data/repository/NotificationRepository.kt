package com.aegis.safety.data.repository

import com.aegis.safety.core.database.AegisDao
import com.aegis.safety.core.database.entities.NotificationEntity
import com.aegis.safety.domain.models.AegisNotification
import com.aegis.safety.domain.models.NotificationType
import com.aegis.safety.domain.repository.NotificationRepositoryInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val dao: AegisDao
) : NotificationRepositoryInterface {

    override fun observeNotifications(): Flow<List<AegisNotification>> =
        dao.observeNotifications().map { list -> list.map { it.toDomain() } }

    override suspend fun markRead(id: String) = dao.markNotificationRead(id)
    override suspend fun markAllRead() = dao.markAllNotificationsRead()
}

private fun NotificationEntity.toDomain() = AegisNotification(
    id, userId, title, body,
    runCatching { NotificationType.valueOf(type) }.getOrDefault(NotificationType.SYSTEM),
    createdAt, read, incidentId
)