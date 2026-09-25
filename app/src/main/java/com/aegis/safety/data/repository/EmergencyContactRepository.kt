package com.aegis.safety.data.repository

import com.aegis.safety.core.database.AegisDao
import com.aegis.safety.core.database.entities.ContactEntity
import com.aegis.safety.data.remote.AegisApiService
import com.aegis.safety.data.remote.dto.CreateContactRequest
import com.aegis.safety.data.remote.dto.UpdateContactRequest
import com.aegis.safety.domain.models.EmergencyContact
import com.aegis.safety.domain.repository.EmergencyContactRepositoryInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyContactRepository @Inject constructor(
    private val api: AegisApiService,
    private val dao: AegisDao
) : EmergencyContactRepositoryInterface {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeContacts(): Flow<List<EmergencyContact>> =
        dao.observeContacts().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllSnapshot(): List<EmergencyContact> =
        dao.getContactsSnapshot().map { it.toDomain() }

    override suspend fun refresh() {
        syncScope.launch {
            runCatching {
                // Best-effort backend sync — never blocks, never throws
                api.getContacts()
            }
        }
    }

    override suspend fun create(
        name: String, phone: String, relationship: String?
    ): Result<EmergencyContact> {
        val local = EmergencyContact(
            id = UUID.randomUUID().toString(),
            userId = "me",
            name = name.trim(),
            phoneNumber = phone.trim(),
            relationship = relationship?.trim()?.ifBlank { null },
            priority = 0,
            verified = false,
            notifyOnSos = true,
            createdAt = System.currentTimeMillis()
        )
        dao.upsertContact(local.toEntity())
        // Fire-and-forget backend sync
        syncScope.launch {
            runCatching { api.createContact(CreateContactRequest(name, phone, relationship)) }
        }
        return Result.success(local)
    }

    override suspend fun update(contact: EmergencyContact): Result<EmergencyContact> {
        dao.upsertContact(contact.toEntity())
        syncScope.launch {
            runCatching {
                api.updateContact(contact.id, UpdateContactRequest(
                    name = contact.name,
                    phoneNumber = contact.phoneNumber,
                    relationship = contact.relationship,
                    priority = contact.priority,
                    notifyOnSos = contact.notifyOnSos
                ))
            }
        }
        return Result.success(contact)
    }

    override suspend fun delete(id: String): Result<Unit> {
        dao.deleteContact(id)
        syncScope.launch { runCatching { api.deleteContact(id) } }
        return Result.success(Unit)
    }
}

private fun ContactEntity.toDomain() = EmergencyContact(
    id, userId, name, phoneNumber, relationship, priority, verified, notifyOnSos, createdAt)

private fun EmergencyContact.toEntity() = ContactEntity(
    id = id, userId = userId, name = name, phoneNumber = phoneNumber,
    relationship = relationship, priority = priority, verified = verified,
    notifyOnSos = notifyOnSos, createdAt = createdAt, synced = false)