package com.example.fairball.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampionshipScreen(
    onBack: () -> Unit,
    onViewReport: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var allMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var allUsers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var allTeams by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    fun refreshData() {
        isLoading = true
        db.collection("users").get().addOnSuccessListener { userSnap ->
            allUsers = userSnap.documents.associate { it.id to (it.getString("displayName") ?: "Sconosciuto") }
            db.collection("teams").get().addOnSuccessListener { teamSnap ->
                allTeams = teamSnap.documents.associate { it.id to (it.getString("name") ?: it.id) }
                db.collection("matches").addSnapshotListener { result, _ ->
                    allMatches = result?.documents?.mapNotNull { doc ->
                        doc.toObject(Match::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    isLoading = false
                }
            }
        }.addOnFailureListener { isLoading = false }
    }

    LaunchedEffect(Unit) { refreshData() }

    val futureMatches = allMatches.filter { it.status != "finished" }
        .sortedBy { it.scheduledAt?.seconds ?: Long.MAX_VALUE }
    val pastMatches = allMatches.filter { it.status == "finished" }
        .sortedByDescending { it.scheduledAt?.seconds ?: 0L }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campionato") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Gare Future") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Passate") })
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val currentList = if (selectedTab == 0) futureMatches else pastMatches
                
                if (currentList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nessuna partita trovata.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(currentList) { match ->
                            if (selectedTab == 0) {
                                ChampionshipFutureMatchItem(
                                    match = match,
                                    currentUserId = currentUser?.uid,
                                    users = allUsers,
                                    teams = allTeams
                                )
                            } else {
                                ChampionshipPastMatchItem(
                                    match = match,
                                    users = allUsers,
                                    teams = allTeams,
                                    onViewReport = { if(match.id.isNotEmpty()) onViewReport(match.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChampionshipFutureMatchItem(
    match: Match,
    currentUserId: String?,
    users: Map<String, String>,
    teams: Map<String, String>
) {
    val db = FirebaseFirestore.getInstance()
    val isAssigned = !match.refereeId.isNullOrEmpty()
    val isAssignedToMe = match.refereeId == currentUserId

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Gara: ${match.code}", fontWeight = FontWeight.Bold)
                Text("${teams[match.homeTeamId] ?: match.homeTeamId} vs ${teams[match.awayTeamId] ?: match.awayTeamId}")
                
                val statusText = if (isAssigned) "ASSEGNATA" else "NON ASSEGNATA"
                val statusColor = if (isAssigned) Color(0xFF4CAF50) else Color(0xFFE91E63)
                Text(statusText, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                
                if (isAssigned) {
                    Text("Arbitro: ${users[match.refereeId]}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            if (currentUserId != null) {
                if (!isAssigned) {
                    Button(onClick = {
                        if (match.id.isNotEmpty()) {
                            db.collection("matches").document(match.id).update(mapOf(
                                "refereeId" to currentUserId,
                                "status" to "assigned",
                                "assignedAt" to Timestamp.now()
                            ))
                        }
                    }) { Text("ARBITRA") }
                } else if (isAssignedToMe) {
                    OutlinedButton(onClick = {
                        if (match.id.isNotEmpty()) {
                            db.collection("matches").document(match.id).update(mapOf(
                                "refereeId" to null,
                                "status" to "pending",
                                "assignedAt" to null
                            ))
                        }
                    }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) {
                        Text("DISDICI")
                    }
                }
            }
        }
    }
}

@Composable
fun ChampionshipPastMatchItem(
    match: Match,
    users: Map<String, String>,
    teams: Map<String, String>,
    onViewReport: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Gara: ${match.code}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                PastScoreDisplay(teams[match.homeTeamId] ?: match.homeTeamId, match.homeScore)
                Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                PastScoreDisplay(teams[match.awayTeamId] ?: match.awayTeamId, match.awayScore)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Arbitro", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(users[match.refereeId] ?: "N/D", style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = onViewReport) { Text("DETTAGLI") }
            }
        }
    }
}

@Composable
fun PastScoreDisplay(name: String, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, style = MaterialTheme.typography.bodySmall)
        Text(score.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}
