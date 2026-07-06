package com.example.fairball.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fairball.data.FirestoreRepository
import com.example.fairball.model.Notification
import com.example.fairball.model.NotificationType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    uid: String,
    onBack: () -> Unit,
    onOpenMatch: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val notifications by FirestoreRepository.notificationsFlow(uid).collectAsState(initial = null)
    var locallyReadNotificationIds by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifiche") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    if (!notifications.isNullOrEmpty()) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    notifications?.let { list ->
                                        locallyReadNotificationIds = locallyReadNotificationIds + list.map { it.id }.toSet()
                                    }
                                    FirestoreRepository.markAllNotificationsRead(uid)
                                }
                            }
                        ) {
                            Text("Segna come lette")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            notifications == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            notifications!!.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Nessuna notifica per ora.", color = Color.Gray)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(notifications!!, key = { it.id }) { notification ->
                        val isRead = notification.read || locallyReadNotificationIds.contains(notification.id)

                        NotificationCard(
                            notification = notification,
                            isReadOverride = isRead,
                            onClick = {
                                scope.launch {
                                    if (!isRead) {
                                        locallyReadNotificationIds = locallyReadNotificationIds + notification.id
                                        FirestoreRepository.markNotificationRead(notification.id)
                                    }
                                    if (notification.type == NotificationType.RESULT_PUBLISHED) {
                                        notification.relatedMatchId?.let { onOpenMatch(it) }
                                    } else {
                                        onBack()
                                    }
                                }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

private fun iconForType(type: String): ImageVector = when (type) {
    NotificationType.NEW_MATCH -> Icons.Default.SportsSoccer
    NotificationType.REFEREE_REQUEST -> Icons.Default.AssignmentInd
    NotificationType.APPROVAL_REQUEST -> Icons.Default.RateReview
    NotificationType.ASSIGNED -> Icons.Default.EventAvailable
    NotificationType.RESULT_PUBLISHED -> Icons.Default.CheckCircle
    NotificationType.RESULT_REJECTED -> Icons.Default.ErrorOutline
    else -> Icons.Default.Notifications
}

@Composable
private fun NotificationCard(
    notification: Notification,
    isReadOverride: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isReadOverride)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconForType(notification.type), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(notification.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Text(notification.message, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (!isReadOverride) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp, top = 4.dp)
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.error, shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
}