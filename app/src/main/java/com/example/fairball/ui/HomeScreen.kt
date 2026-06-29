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
import androidx.compose.ui.unit.dp
import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    role: String,
    debugUid: String? = null,
    onViewReferees: () -> Unit,
    onViewProfile: () -> Unit,
    onViewChampionship: () -> Unit,
    onViewMap: () -> Unit,
    onArbitrateMatch: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val effectiveUid = debugUid ?: currentUser?.uid

    var myMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var availableMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var teamsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadData() {
        if (effectiveUid == null) {
            isLoading = false
            return
        }
        isLoading = true
        db.collection("teams").get().addOnSuccessListener { teamSnap ->
            teamsMap = teamSnap.documents.associate { it.id to (it.getString("name") ?: it.id) }
            
            db.collection("matches").whereIn("status", listOf("pending", "assigned", "assegnata")).get().addOnSuccessListener { matchSnap ->
                val allMatches = matchSnap.documents.mapNotNull { doc ->
                    doc.toObject(Match::class.java)?.copy(id = doc.id)
                }
                
                myMatches = allMatches.filter { it.refereeId == effectiveUid }
                    .sortedBy { it.scheduledAt?.seconds ?: 0L }
                
                availableMatches = allMatches.filter { it.refereeId == null || it.refereeId == "" }
                    .sortedBy { it.scheduledAt?.seconds ?: 0L }
                
                isLoading = false
            }.addOnFailureListener { isLoading = false }
        }.addOnFailureListener { isLoading = false }
    }

    LaunchedEffect(effectiveUid) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FairBall") },
                navigationIcon = {
                    IconButton(onClick = onViewReferees) { Icon(Icons.Default.Sports, "Arbitri") }
                },
                actions = {
                    IconButton(onClick = onViewProfile) { Icon(Icons.Default.AccountCircle, "Profilo") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Benvenuto, $role", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { HomeSectionTitle("I Miei Impegni", Icons.Default.EventAvailable) }
                    if (myMatches.isEmpty()) {
                        item { Text("Nessuna gara prenotata.", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                    } else {
                        items(myMatches) { match ->
                            MyMatchCard(match, teamsMap, 
                                onVai = { if (match.id.isNotEmpty()) onArbitrateMatch(match.id) },
                                onDisdici = {
                                    db.collection("matches").document(match.id)
                                        .update(mapOf("refereeId" to null, "status" to "pending", "assignedAt" to null))
                                        .addOnSuccessListener { loadData() }
                                }
                            )
                        }
                    }

                    item { HomeSectionTitle("Gare Disponibili", Icons.Default.AddCircleOutline) }
                    if (availableMatches.isEmpty()) {
                        item { Text("Nessuna gara disponibile.", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                    } else {
                        items(availableMatches) { match ->
                            AvailableMatchCard(match, teamsMap) {
                                db.collection("matches").document(match.id)
                                    .update(mapOf("refereeId" to effectiveUid, "status" to "assigned", "assignedAt" to Timestamp.now()))
                                    .addOnSuccessListener { loadData() }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onViewChampionship, modifier = Modifier.fillMaxWidth()) { Text("Vedi Tutto il Campionato") }
        }
    }
}

@Composable
fun HomeSectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun MyMatchCard(match: Match, teams: Map<String, String>, onVai: () -> Unit, onDisdici: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Gara: ${match.code}", style = MaterialTheme.typography.labelSmall)
            Text("${teams[match.homeTeamId] ?: match.homeTeamId} vs ${teams[match.awayTeamId] ?: match.awayTeamId}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onVai, modifier = Modifier.weight(1f)) { Text("VAI") }
                OutlinedButton(onClick = onDisdici, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Text("DISDICI") }
            }
        }
    }
}

@Composable
fun AvailableMatchCard(match: Match, teams: Map<String, String>, onPrenotati: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Gara: ${match.code}", style = MaterialTheme.typography.labelSmall)
                Text("${teams[match.homeTeamId] ?: match.homeTeamId} vs ${teams[match.awayTeamId] ?: match.awayTeamId}", style = MaterialTheme.typography.titleMedium)
            }
            Button(onClick = onPrenotati) { Text("PRENOTATI") }
        }
    }
}
