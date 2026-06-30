package com.example.fairball.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.User
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
    var allMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var allReferees by remember { mutableStateOf<List<User>>(emptyList()) }
    var teamsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var teamsList by remember { mutableStateOf<List<Team>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(effectiveUid) {
        if (effectiveUid == null) {
            isLoading = false
            return@LaunchedEffect
        }

        // Real-time listener for Teams
        db.collection("teams").addSnapshotListener { teamSnap, _ ->
            if (teamSnap != null) {
                teamsList = teamSnap.toObjects(Team::class.java)
                teamsMap = teamsList.associate { it.id to it.name }
            }
        }

        // Real-time listener for Matches
        db.collection("matches").addSnapshotListener { matchSnap, _ ->
            if (matchSnap != null) {
                val matches = matchSnap.documents.mapNotNull { doc ->
                    doc.toObject(Match::class.java)?.copy(id = doc.id)
                }
                allMatches = matches

                if (role != "admin") {
                    myMatches = matches.filter { (it.refereeId == effectiveUid || it.coRefereeId == effectiveUid) && it.status != "finished" }
                        .sortedBy { it.scheduledAt?.seconds ?: 0L }

                    availableMatches = matches.filter { (it.refereeId == null || it.refereeId == "") && it.status == "pending" }
                        .sortedBy { it.scheduledAt?.seconds ?: 0L }
                }
                isLoading = false
            }
        }

        if (role == "admin") {
            // Real-time listener for Referees
            db.collection("users").whereEqualTo("role", "referee").addSnapshotListener { refSnap, _ ->
                if (refSnap != null) {
                    allReferees = refSnap.toObjects(User::class.java)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FairBall") },
                navigationIcon = {
                    IconButton(onClick = onViewReferees) { 
                        Icon(Icons.Default.Sports, "Arbitri") 
                    }
                },
                actions = {
                    IconButton(onClick = onViewMap) { Icon(Icons.Default.Map, "Mappa") }
                    IconButton(onClick = onViewProfile) { Icon(Icons.Default.AccountCircle, "Profilo") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Benvenuto, ${if (role == "admin") "Amministratore" else "Arbitro"}", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (role == "admin") {
                        item { HomeSectionTitle("Gestione Partite", Icons.Default.Settings) }
                        items(allMatches.filter { it.status != "finished" }) { match ->
                            MatchAdminCard(match, allReferees, teamsList)
                        }
                        
                        item { HomeSectionTitle("Gestione Arbitri", Icons.Default.People) }
                        items(allReferees) { referee ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onViewReferees() }
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(referee.displayName, modifier = Modifier.weight(1f))
                                    Text("Vedi Profilo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                                }
                            }
                        }
                    } else {
                        item {
                             Card(
                                onClick = onViewMap,
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Map, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Visualizza Mappa Palestre", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item { HomeSectionTitle("I Miei Impegni", Icons.Default.EventAvailable) }
                        if (myMatches.isEmpty()) {
                            item { Text("Nessuna gara prenotata.", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                        } else {
                            items(myMatches) { match ->
                                MyMatchCard(
                                    match = match,
                                    teams = teamsMap,
                                    onVai = { if (match.id.isNotEmpty()) onArbitrateMatch(match.id) },
                                    onDisdici = {
                                        db.collection("matches").document(match.id)
                                            .update(mapOf("refereeId" to null, "status" to "pending", "assignedAt" to null))
                                    }
                                )
                            }
                        }

                        item { HomeSectionTitle("Gare Disponibili", Icons.Default.AddCircleOutline) }
                        if (availableMatches.isEmpty()) {
                            item { Text("Nessuna gara disponibile.", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                        } else {
                            items(availableMatches) { match ->
                                AvailableMatchCard(
                                    match = match,
                                    teams = teamsMap,
                                    onPrenotati = {
                                        db.collection("matches").document(match.id)
                                            .update(mapOf("refereeId" to effectiveUid, "status" to "assigned", "assignedAt" to Timestamp.now()))
                                    }
                                )
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
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MyMatchCard(
    match: Match,
    teams: Map<String, String>,
    onVai: () -> Unit,
    onDisdici: () -> Unit
) {
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
            Text("${teams[match.homeTeamId] ?: match.homeTeamId} vs ${teams[match.awayTeamId] ?: match.awayTeamId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onVai, modifier = Modifier.weight(1f)) { Text("VAI") }
                OutlinedButton(onClick = onDisdici, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Text("DISDICI") }
            }
        }
    }
}

@Composable
fun AvailableMatchCard(
    match: Match,
    teams: Map<String, String>,
    onPrenotati: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            match.scheduledAt?.let {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(it.toFormattedDate(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(it.toFormattedTime(), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${teams[match.homeTeamId] ?: match.homeTeamId} vs ${teams[match.awayTeamId] ?: match.awayTeamId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(onClick = onPrenotati) { Text("PRENOTATI") }
            }
        }
    }
}
