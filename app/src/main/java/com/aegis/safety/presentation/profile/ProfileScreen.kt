package com.aegis.safety.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.safety.domain.repository.AuthRepositoryInterface
import com.aegis.safety.presentation.components.AegisTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: AuthRepositoryInterface
) : androidx.lifecycle.ViewModel() {
    fun logout(onDone: () -> Unit) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            auth.logout()
            onDone()
        }
    }
}

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onSettings: () -> Unit,
    onPrivacy: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit,
    vm: ProfileViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AegisTopBar(title = "Profile", onBack = onBack) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            
            Text(
                "ACCOUNT", 
                style = MaterialTheme.typography.labelMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    MenuItem("Settings", onClick = onSettings)
                    HorizontalDivider(
                        Modifier.padding(start = 16.dp), 
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    MenuItem("Privacy Center", onClick = onPrivacy)
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                "SUPPORT", 
                style = MaterialTheme.typography.labelMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    MenuItem("Help", onClick = onHelp)
                    HorizontalDivider(
                        Modifier.padding(start = 16.dp), 
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    MenuItem("About", onClick = onAbout)
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = { vm.logout(onLogout) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Sign Out", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, 
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}