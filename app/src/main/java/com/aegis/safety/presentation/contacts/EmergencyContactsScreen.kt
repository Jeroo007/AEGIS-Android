package com.aegis.safety.presentation.contacts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.safety.presentation.components.AegisTopBar

@Composable
fun EmergencyContactsScreen(
    onBack: () -> Unit,
    vm: ContactsViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }

    // Uses our custom contract that returns a Phone-table row
    val pickPhone = rememberLauncherForActivityResult(
        contract = PickPhoneNumberContract()
    ) { intent ->
        vm.importFromIntent(context, intent)
    }

    Scaffold(
        topBar = {
            AegisTopBar(title = "Emergency Contacts", onBack = onBack, actions = {
                IconButton(onClick = { showImportSheet = true }) {
                    Icon(Icons.Default.ContactPhone, contentDescription = "Import")
                }
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            })
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (s.loading && s.contacts.isEmpty()) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            s.error?.let { err ->
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f))
                        IconButton(onClick = { /* dismiss */ }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            if (s.contacts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.Contacts, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("No emergency contacts yet",
                            style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("Add trusted people who will be notified in an emergency",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = { pickPhone.launch(null) }) {
                            Icon(Icons.Default.ContactPhone, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Import from contacts")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { showAdd = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Enter manually")
                        }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { pickPhone.launch(null) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContactPhone, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Import")
                            }
                            OutlinedButton(
                                onClick = { showAdd = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Manual")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    items(s.contacts, key = { it.id }) { c ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, fontWeight = FontWeight.SemiBold)
                                    Text(c.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                                    c.relationship?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("Priority ${c.priority + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { vm.remove(c.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddContactDialog(
            onDismiss = { showAdd = false },
            onAdd = { n, p, r -> vm.add(n, p, r); showAdd = false }
        )
    }

    if (showImportSheet) {
        ImportOptionsSheet(
            onDismiss = { showImportSheet = false },
            onPickContact = {
                showImportSheet = false
                pickPhone.launch(null)
            },
            onManual = {
                showImportSheet = false
                showAdd = true
            }
        )
    }
}

@Composable
private fun ImportOptionsSheet(
    onDismiss: () -> Unit,
    onPickContact: () -> Unit,
    onManual: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add emergency contact") },
        text = {
            Column {
                Text("Choose how to add a trusted person.")
                Spacer(Modifier.height(12.dp))
                ListItem(
                    headlineContent = { Text("Import from contacts") },
                    supportingContent = { Text("Pick a phone number from your contacts") },
                    leadingContent = { Icon(Icons.Default.ContactPhone, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onPickContact)
                )
                ListItem(
                    headlineContent = { Text("Enter manually") },
                    supportingContent = { Text("Type the name and phone number") },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onManual)
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var rel by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(rel, { rel = it }, label = { Text("Relationship (optional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name.trim(), phone.trim(), rel.trim().ifBlank { null }) },
                enabled = name.isNotBlank() && phone.length >= 5
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}