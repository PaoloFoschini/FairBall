package com.example.fairball.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fairball.data.FirestoreRepository
import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.example.fairball.model.Venue
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

@Composable
fun HomeSectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MyMatchCard(match: Match, teamsMap: Map<String, String>, onVai: () -> Unit, onDisdici: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            match.scheduledAt?.let {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(it.toFormattedDate(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(it.toFormattedTime(), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }
            }
            Text("${teamsMap[match.homeTeamId] ?: match.homeTeamId} vs ${teamsMap[match.awayTeamId] ?: match.awayTeamId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onVai, modifier = Modifier.weight(1f)) { Text("VAI") }
                OutlinedButton(onClick = onDisdici, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Text("DISDICI") }
            }
        }
    }
}

@Composable
fun AvailableMatchCard(match: Match, teamsMap: Map<String, String>, onPrenotati: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            match.scheduledAt?.let {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(it.toFormattedDate(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(it.toFormattedTime(), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${teamsMap[match.homeTeamId] ?: match.homeTeamId} vs ${teamsMap[match.awayTeamId] ?: match.awayTeamId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(onClick = onPrenotati) { Text("PRENOTATI") }
            }
        }
    }
}

@Composable
fun MatchApplicationCard(match: Match, referees: List<User>, teams: List<Team>, venues: List<Venue>) {
    val scope = rememberCoroutineScope()
    val homeTeamName = teams.find { it.id == match.homeTeamId }?.name ?: match.homeTeamId
    val awayTeamName = teams.find { it.id == match.awayTeamId }?.name ?: match.awayTeamId
    val venueName = venues.find { it.id == match.venueId }?.name ?: "Sede non specificata"

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$homeTeamName vs $awayTeamName", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text("${match.scheduledAt?.toFormattedDate()} ${match.scheduledAt?.toFormattedTime()}", fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text(venueName, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Text("Candidati:", style = MaterialTheme.typography.labelMedium)
            for (uid in match.refereeApplications) {
                val ref = referees.find { it.uid == uid }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(ref?.displayName ?: "Sconosciuto", modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            scope.launch {
                                FirestoreRepository.updateMatch(
                                    matchId = match.id,
                                    fields = mapOf(
                                        "refereeId" to uid,
                                        "status" to "assigned",
                                        "assignedAt" to Timestamp.now(),
                                        "refereeApplications" to emptyList<String>()
                                    )
                                )
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Scegli", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MatchApprovalCard(match: Match, referees: List<User>, teams: List<Team>) {
    val scope = rememberCoroutineScope()
    var showDocs by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionComment by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    val homeName = teams.find { it.id == match.homeTeamId }?.name ?: match.homeTeamId
                    val awayName = teams.find { it.id == match.awayTeamId }?.name ?: match.awayTeamId
                    Text("$homeName ${match.homeScore} - ${match.awayScore} $awayName", fontWeight = FontWeight.Bold)
                    Text("Arbitro: ${referees.find { it.uid == match.refereeId }?.displayName ?: "Sconosciuto"}", style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = {
                        scope.launch { FirestoreRepository.approveMatch(match.id) }
                    }) { Text("Approva") }

                    Button(
                        onClick = { showRejectDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Rifiuta") }
                }
            }
            TextButton(onClick = { showDocs = !showDocs }) { Text(if (showDocs) "Nascondi Documenti" else "Verifica Documenti") }
            if (showDocs) {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DocMiniPreview(match.photoDistintaA, "Distinta Casa")
                    DocMiniPreview(match.photoDistintaB, "Distinta Ospiti")
                    DocMiniPreview(match.photoReferto, "Referto")
                }
            }
        }
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Richiedi Correzione") },
            text = {
                OutlinedTextField(
                    value = rejectionComment,
                    onValueChange = { rejectionComment = it },
                    label = { Text("Cosa deve correggere l'arbitro?") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionComment.isNotBlank()) {
                            scope.launch {
                                FirestoreRepository.rejectMatchReport(match.id, rejectionComment)
                                showRejectDialog = false
                            }
                        }
                    },
                    enabled = rejectionComment.isNotBlank()
                ) { Text("Invia Nota") }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("Annulla") }
            }
        )
    }
}

@Composable
fun HomeMatchAdminCard(match: Match, referees: List<User>, teams: List<Team>, venues: List<Venue>) {
    val scope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var isAssigningCoReferee by remember { mutableStateOf(false) }

    val homeName = teams.find { it.id == match.homeTeamId }?.name ?: match.homeTeamId
    val awayName = teams.find { it.id == match.awayTeamId }?.name ?: match.awayTeamId
    val refName = referees.find { it.uid == match.refereeId }?.displayName ?: "—"
    val coRefName = referees.find { it.uid == match.coRefereeId }?.displayName
    val venueName = venues.find { it.id == match.venueId }?.name ?: "Sede non specificata"

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("$homeName vs $awayName", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${match.scheduledAt?.toFormattedDate() ?: "Data N/D"} ${match.scheduledAt?.toFormattedTime() ?: "Ora N/D"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(Modifier.width(4.dp))
                        Text(venueName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Arbitri: $refName ${if (coRefName != null) "/ $coRefName" else ""}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = {
                    scope.launch { FirestoreRepository.deleteMatch(match.id) }
                }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { isAssigningCoReferee = false; showAssignDialog = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("Assegna 1°", fontSize = 11.sp) }
                OutlinedButton(
                    onClick = { isAssigningCoReferee = true; showAssignDialog = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("Assegna 2°", fontSize = 11.sp) }
            }
        }
    }

    if (showEditDialog) {
        MatchEditDialog(
            match = match,
            teams = teams,
            venues = venues,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                scope.launch {
                    FirestoreRepository.updateMatch(match.id, updated.toMap())
                    showEditDialog = false
                }
            }
        )
    }

    if (showAssignDialog) {
        AssignRefereeDialog(
            match = match,
            referees = referees,
            isCoReferee = isAssigningCoReferee,
            currentRefereeId = if (isAssigningCoReferee) match.coRefereeId else match.refereeId,
            onDismiss = { showAssignDialog = false }
        )
    }
}

@Composable
fun DocMiniPreview(uri: String?, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        if (!uri.isNullOrEmpty() && uri != "null") {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.size(100.dp).background(Color.LightGray, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ImageNotSupported, null)
            }
        }
    }
}

private fun Match.toMap(): Map<String, Any?> = mapOf(
    "homeTeamId" to homeTeamId,
    "awayTeamId" to awayTeamId,
    "homeScore" to homeScore,
    "awayScore" to awayScore,
    "code" to code,
    "category" to category,
    "phase" to phase,
    "venueId" to venueId,
    "scheduledAt" to scheduledAt
)