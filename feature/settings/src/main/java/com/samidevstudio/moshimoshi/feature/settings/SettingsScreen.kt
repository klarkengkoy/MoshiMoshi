package com.samidevstudio.moshimoshi.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.samidevstudio.moshimoshi.core.data.repository.AuthRepository

@Composable
fun SettingsScreen(
    versionName: String,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(authRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser = uiState.currentUser

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Profile Section
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (currentUser?.photoUrl != null) {
                AsyncImage(
                    model = currentUser.photoUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { viewModel.onEditNameClick() }
        ) {
            Text(
                text = currentUser?.displayName ?: "Guest User",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Edit, 
                contentDescription = "Edit Name", 
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Text(
            text = if (currentUser?.isAnonymous == true) "Temporary Guest Account" else (currentUser?.email ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 1. App Settings
        SettingsGroup(title = "App Settings") {
            SettingsItem(
                icon = Icons.Default.Translate,
                title = "Preferred Language",
                subtitle = "Japanese (Standard)",
                onClick = { /* Future logic */ }
            )
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Reminders",
                subtitle = "Daily practice alerts",
                onClick = { /* Future logic */ }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2. Sign Out Section
        SettingsGroup(title = "Session") {
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Sign Out",
                onClick = { viewModel.onSignOutClick() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Danger Zone
        SettingsGroup(title = "Danger Zone") {
            SettingsItem(
                icon = Icons.Default.DeleteForever,
                title = "Delete Account",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { viewModel.onDeleteAccountClick() }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "MoshiMoshi Beta",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Text(
            text = "Version $versionName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }

    // Dialogs
    if (uiState.showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Update Name") },
            text = {
                OutlinedTextField(
                    value = uiState.newName,
                    onValueChange = { viewModel.onNameChange(it) },
                    label = { Text("Display Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveName() }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) { Text("Cancel") }
            }
        )
    }

    if (uiState.showLogoutWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Warning: Guest Account") },
            text = { Text("You are currently using a Guest account. If you sign out now, all your practice data and progress will be permanently lost. Would you like to sign out anyway?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmSignOut() }) { 
                    Text("Sign Out", color = MaterialTheme.colorScheme.error) 
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) { Text("Keep Practicing") }
            }
        )
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Delete Account?") },
            text = { Text("This action is permanent. All your data will be removed from MoshiMoshi servers.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteAccount() }) { 
                    Text("Delete", color = MaterialTheme.colorScheme.error) 
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, color = titleColor, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        leadingContent = { 
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = if (titleColor == Color.Unspecified) MaterialTheme.colorScheme.primary else titleColor
            ) 
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
