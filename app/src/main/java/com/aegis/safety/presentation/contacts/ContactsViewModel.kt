package com.aegis.safety.presentation.contacts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import timber.log.Timber
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.domain.models.EmergencyContact
import com.aegis.safety.domain.repository.EmergencyContactRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ContactsUiState(
    val contacts: List<EmergencyContact> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repo: EmergencyContactRepositoryInterface
) : ViewModel() {

    private val _state = MutableStateFlow(ContactsUiState())
    val state: StateFlow<ContactsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeContacts().collect { list ->
                _state.update { it.copy(contacts = list) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { repo.refresh() }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(loading = false) }
        }
    }

    fun add(name: String, phone: String, relationship: String?) {
        viewModelScope.launch {
            repo.create(name, phone, relationship)
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun remove(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun update(contact: EmergencyContact) {
        viewModelScope.launch { repo.update(contact) }
    }

    /**
     * Handles the Intent returned by the system contacts picker.
     *
     * The Intent's data URI points to a row in the Phone table, which already
     * contains DISPLAY_NAME and NUMBER. One query, one row, we're done.
     */
    fun importFromIntent(context: Context, intent: Intent?) {
        val uri: Uri = intent?.data ?: run {
            _state.update { it.copy(error = "No contact selected") }
            return
        }

        viewModelScope.launch {
            val (name, phone) = withContext(Dispatchers.IO) {
                queryPhoneUri(context, uri)
            }

            if (name.isNullOrBlank()) {
                Timber.w("Contact name was empty for uri=%s", uri)
            }
            if (phone.isNullOrBlank()) {
                _state.update {
                    it.copy(error = "That contact has no phone number")
                }
                return@launch
            }

            val displayName = name?.takeIf { it.isNotBlank() } ?: phone

            // Avoid duplicates by normalized phone number
            val normalized = normalizePhone(phone)
            val existing = _state.value.contacts.any {
                normalizePhone(it.phoneNumber) == normalized
            }
            if (existing) {
                _state.update {
                    it.copy(error = "$displayName is already an emergency contact")
                }
                return@launch
            }

            repo.create(displayName, phone, null)
                .onSuccess { _state.update { it.copy(error = null) } }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    /**
     * Queries a Phone-table URI. The picker returns URIs that are already
     * scoped to the Phone table, so DISPLAY_NAME and NUMBER are present
     * as columns on the same row.
     *
     * We also handle the (rarer) case where the URI is a Contact URI by
     * falling back to a two-step query.
     */
    private fun queryPhoneUri(context: Context, uri: Uri): Pair<String?, String?> {
        // --- Attempt 1: treat URI as a Phone table row (the common case) ---
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    )
                    val numIdx = c.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    val name = if (nameIdx >= 0) c.getString(nameIdx) else null
                    val number = if (numIdx >= 0) c.getString(numIdx) else null
                    if (!number.isNullOrBlank()) {
                        return name?.trim() to number.trim()
                    }
                }
            }
        }.onFailure { Timber.w(it, "Phone-table query failed for %s", uri) }

        // --- Attempt 2: treat URI as a Contact row, then look up its phone ---
        return queryContactUri(context, uri)
    }

    private fun queryContactUri(context: Context, uri: Uri): Pair<String?, String?> {
        var name: String? = null
        var contactId: String? = null

        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME
                ),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val idIdx = c.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (idIdx >= 0) contactId = c.getString(idIdx)
                    if (nameIdx >= 0) name = c.getString(nameIdx)
                }
            }
        }.onFailure { Timber.w(it, "Contact-table query failed for %s", uri) }

        if (contactId.isNullOrBlank()) return name?.trim() to null

        var phone: String? = null
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (idx >= 0) phone = c.getString(idx)
                }
            }
        }.onFailure { Timber.w(it, "Phone lookup failed for contactId=%s", contactId) }

        return name?.trim() to phone?.trim()
    }

    private fun normalizePhone(raw: String): String {
        // Strip spaces, dashes, parens; keep leading + and digits
        val cleaned = raw.replace(Regex("[^+\\d]"), "")
        return if (cleaned.startsWith("+")) cleaned else cleaned.takeLast(10)
    }
}