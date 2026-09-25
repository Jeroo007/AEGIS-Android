package com.aegis.safety.presentation.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.safety.presentation.components.AegisTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    vm: NotificationsViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        AegisTopBar(title = "Notifications", onBack = onBack, actions = {
            IconButton(onClick = vm::markAllRead) {
                Icon(Icons.Default.DoneAll, contentDescription = "Mark all read")
            }
        })
    }) { padding ->
        Column(Modifier.padding(padding)) {
            if (s.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notifications")
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.items, key = { it.id }) { n ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(n.title, fontWeight = FontWeight.SemiBold)
                                Text(n.body, style = MaterialTheme.typography.bodyMedium)
                                Text(SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                                    .format(Date(n.createdAt)),
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}