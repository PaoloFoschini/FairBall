package com.example.fairball.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fairball.data.AppConfig
import com.example.fairball.data.FirestoreRepository
import com.example.fairball.model.Match
import com.example.fairball.model.User
import com.example.fairball.model.UserRole
import com.example.fairball.model.roleEnum
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Dialog per assegnare un arbitro ad una partita.
 */
@Composable
fun AssignRefereeDialog(
    match: Match,
    referees: List<User>,
    isCoReferee: Boolean,
    currentRefereeId: String?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val allMatches by FirestoreRepository.matchesFlow().collectAsState(initial = emptyList())

    val availableReferees = referees.filter { referee ->
        if (isCoReferee && match.refereeId == referee.uid) return@filter false
        if (!isCoReferee && match.coRefereeId == referee.uid) return@filter false

        val currentMatchDate = match.scheduledAt?.toFormattedDate()
        val currentMatchTime = match.scheduledAt?.toFormattedTime()

        if (currentMatchDate != null && currentMatchTime != null) {
            val hasConflict = allMatches.any { otherMatch ->
                otherMatch.id != match.id &&
                        (otherMatch.refereeId == referee.uid || otherMatch.coRefereeId == referee.uid) &&
                        otherMatch.scheduledAt?.toFormattedDate() == currentMatchDate &&
                        otherMatch.scheduledAt?.toFormattedTime() == currentMatchTime
            }

            if (hasConflict) return@filter false
        }

        true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCoReferee) "Assegna Co-Arbitro" else "Assegna Arbitro Principale") },
        text = {
            LazyColumn {
                item {
                    TextButton(
                        onClick = {
                            scope.launch {
                                FirestoreRepository.removeReferee(match.id, isCoReferee)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.Red)
                        Spacer(Modifier.width(8.dp))
                        Text("Rimuovi assegnazione", color = Color.Red)
                    }
                }
                items(availableReferees) { referee ->
                    val isCurrent = referee.uid == currentRefereeId
                    TextButton(
                        onClick = {
                            scope.launch {
                                FirestoreRepository.assignReferee(match.id, referee.uid, isCoReferee)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isCurrent) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            referee.displayName,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                if (availableReferees.isEmpty()) {
                    item {
                        Text(
                            "Nessun arbitro disponibile o già impegnato in questa data/ora.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        }
    )
}

/**
 * Scheda di amministrazione di un arbitro.
 */
@Composable
fun RefereeAdminCard(referee: User, onViewProfile: (() -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val isSuperAdmin = referee.uid == AppConfig.SUPERADMIN_UID

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(referee.displayName, style = MaterialTheme.typography.titleMedium)
                Text(referee.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(
                    text = when {
                        isSuperAdmin -> "Superadmin"
                        referee.roleEnum == UserRole.ADMIN -> "Amministratore"
                        else -> "Arbitro"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (referee.roleEnum == UserRole.ADMIN || isSuperAdmin) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }

            if (isSuperAdmin) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Account protetto",
                    tint = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifica profilo", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina utente", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showEditDialog) {
        RefereeEditDialog(
            referee = referee,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newEmail, newRole ->
                scope.launch {
                    FirestoreRepository.updateUserProfile(referee.uid, newName, newEmail, newRole)
                    showEditDialog = false
                }
            }
        )
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Elimina Arbitro",
            message = "Vuoi eliminare definitivamente l'account di ${referee.displayName}? L'operazione è irreversibile.",
            onConfirm = {
                scope.launch {
                    FirestoreRepository.deleteUser(referee.uid)
                    showDeleteConfirm = false
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}