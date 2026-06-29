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
import androidx.compose.ui.unit.dp
import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Partite", "Arbitri")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestione Admin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            when (selectedTab) {
                0 -> MatchManagementList()
                1 -> RefereeManagementList()
            }
        }
    }
}

@Composable
fun MatchManagementList() {
    val db = FirebaseFirestore.getInstance()
    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var referees by remember { mutableStateOf<List<User>>(emptyList()) }
    var teams by remember { mutableStateOf<List<Team>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        db.collection("matches").addSnapshotListener { snapshot, _ ->
            matches = snapshot?.toObjects(Match::class.java) ?: emptyList()
        }
        db.collection("users").whereEqualTo("role", "referee").addSnapshotListener { snapshot, _ ->
            referees = snapshot?.toObjects(User::class.java) ?: emptyList()
        }
        db.collection("teams").addSnapshotListener { snapshot, _ ->
            teams = snapshot?.toObjects(Team::class.java) ?: emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(matches) { match ->
                MatchAdminCard(match, referees, teams)
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi Partita")
        }
    }

    if (showAddDialog) {
        MatchEditDialog(
            teams = teams,
            onDismiss = { showAddDialog = false },
            onSave = { newMatch ->
                val docRef = db.collection("matches").document()
                db.collection("matches").document(docRef.id).set(newMatch.copy(id = docRef.id))
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MatchAdminCard(match: Match, referees: List<User>, teams: List<Team>) {
    val db = FirebaseFirestore.getInstance()
    var showAssignDialog by remember { mutableStateOf(false) }
    var isAssigningCoReferee by remember { mutableStateOf(false) }

    val homeTeamName = teams.find { it.id == match.homeTeamId }?.name ?: match.homeTeamId
    val awayTeamName = teams.find { it.id == match.awayTeamId }?.name ?: match.awayTeamId
    val refName = referees.find { it.uid == match.refereeId }?.displayName ?: "Da assegnare"
    val coRefName = referees.find { it.uid == match.coRefereeId }?.displayName ?: "Nessuno"

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gara: ${match.code} (${match.category})", style = MaterialTheme.typography.titleMedium)
                    Text("$homeTeamName vs $awayTeamName")
                    Text("Arbitri: $refName / $coRefName", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { isAssigningCoReferee = false; showAssignDialog = true }) {
                    Icon(Icons.Default.Person, contentDescription = "Assegna Arbitro")
                }
                IconButton(onClick = { isAssigningCoReferee = true; showAssignDialog = true }) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "Assegna Co-Arbitro")
                }
                IconButton(onClick = { db.collection("matches").document(match.id).delete() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showAssignDialog) {
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text(if (isAssigningCoReferee) "Assegna Co-Arbitro" else "Assegna Arbitro Principale") },
            text = {
                LazyColumn {
                    items(referees) { referee ->
                        TextButton(
                            onClick = {
                                val field = if (isAssigningCoReferee) "coRefereeId" else "refereeId"
                                val updates = mutableMapOf<String, Any?>(field to referee.uid)
                                if (!isAssigningCoReferee) {
                                    updates["assignedAt"] = Timestamp.now()
                                    updates["status"] = "assigned"
                                }
                                db.collection("matches").document(match.id).update(updates)
                                showAssignDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(referee.displayName)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAssignDialog = false }) { Text("Chiudi") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchEditDialog(
    match: Match = Match(),
    teams: List<Team>,
    onDismiss: () -> Unit,
    onSave: (Match) -> Unit
) {
    var code by remember { mutableStateOf(match.code) }
    var category by remember { mutableStateOf(match.category) }
    var phase by remember { mutableStateOf(match.phase) }
    var venue by remember { mutableStateOf(match.venueId) }
    var homeTeamId by remember { mutableStateOf(match.homeTeamId) }
    var awayTeamId by remember { mutableStateOf(match.awayTeamId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova Partita") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = code, onValueChange = { code = it }, label = { Text("Codice Gara") })
                TextField(value = venue, onValueChange = { venue = it }, label = { Text("Impianto (Nome/ID)") })
                
                Text("Categoria:")
                Row {
                    listOf("Maschile", "Femminile", "Misto").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
                
                Text("Fase:")
                Row {
                    listOf("Regular Season", "Semifinale", "Finale").forEach { p ->
                        FilterChip(
                            selected = phase == p,
                            onClick = { phase = p },
                            label = { Text(p) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }

                Text("Squadre:")
                TeamDropdown(teams, homeTeamId) { homeTeamId = it }
                TeamDropdown(teams, awayTeamId) { awayTeamId = it }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onSave(match.copy(
                    code = code, category = category, phase = phase, 
                    venueId = venue, homeTeamId = homeTeamId, awayTeamId = awayTeamId,
                    scheduledAt = Timestamp.now() // In un caso reale si userebbe un DatePicker
                )) 
            }) {
                Text("Salva")
            }
        }
    )
}

@Composable
fun RefereeManagementList() {
    val db = FirebaseFirestore.getInstance()
    var referees by remember { mutableStateOf<List<User>>(emptyList()) }
    LaunchedEffect(Unit) {
        db.collection("users").whereEqualTo("role", "referee").addSnapshotListener { snapshot, _ ->
            referees = snapshot?.toObjects(User::class.java) ?: emptyList()
        }
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(referees) { referee ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(referee.displayName, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun TeamDropdown(teams: List<Team>, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTeam = teams.find { it.id == selectedId }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedTeam?.name ?: "Seleziona Squadra")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            teams.forEach { team ->
                DropdownMenuItem(text = { Text(team.name) }, onClick = { onSelect(team.id); expanded = false })
            }
        }
    }
}
