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
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit
) {
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
            onDismiss = { showAddDialog = false },
            onSave = { newMatch ->
                val docRef = db.collection("matches").document()
                db.collection("matches").document(docRef.id).set(newMatch.copy(id = docRef.id))
                showAddDialog = false
            },
            teams = teams
        )
    }
}

@Composable
fun MatchAdminCard(match: Match, referees: List<User>, teams: List<Team>) {
    val db = FirebaseFirestore.getInstance()
    var showAssignDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val homeTeamName = teams.find { it.id == match.homeTeamId }?.name ?: match.homeTeamId
    val awayTeamName = teams.find { it.id == match.awayTeamId }?.name ?: match.awayTeamId

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gara: ${match.code}", style = MaterialTheme.typography.titleMedium)
                    Text("$homeTeamName vs $awayTeamName")
                    Text("Arbitro: ${referees.find { it.uid == match.refereeId }?.displayName ?: "Non assegnato"}", 
                        style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { showAssignDialog = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Assegna Arbitro")
                }
                IconButton(onClick = { showEditDialog = true }) { Icon(Icons.Default.Edit, contentDescription = "Modifica") }
                IconButton(onClick = { db.collection("matches").document(match.id).delete() }) { 
                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error) 
                }
            }
        }
    }

    if (showAssignDialog) {
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("Assegna Arbitro") },
            text = {
                LazyColumn {
                    items(referees) { referee ->
                        TextButton(
                            onClick = {
                                db.collection("matches").document(match.id).update("refereeId", referee.uid)
                                showAssignDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(referee.displayName)
                        }
                    }
                    item {
                        TextButton(onClick = {
                            db.collection("matches").document(match.id).update("refereeId", null)
                            showAssignDialog = false
                        }) {
                            Text("Rimuovi assegnazione", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAssignDialog = false }) { Text("Annulla") } }
        )
    }

    if (showEditDialog) {
        MatchEditDialog(
            match = match,
            onDismiss = { showEditDialog = false },
            onSave = { updatedMatch ->
                db.collection("matches").document(match.id).set(updatedMatch)
                showEditDialog = false
            },
            teams = teams
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
    var homeTeamId by remember { mutableStateOf(match.homeTeamId) }
    var awayTeamId by remember { mutableStateOf(match.awayTeamId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (match.id.isEmpty()) "Nuova Partita" else "Modifica Partita") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = code, onValueChange = { code = it }, label = { Text("Codice Gara") })
                
                Text("Squadra Casa:")
                TeamDropdown(teams, homeTeamId) { homeTeamId = it }
                
                Text("Squadra Ospite:")
                TeamDropdown(teams, awayTeamId) { awayTeamId = it }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(match.copy(code = code, homeTeamId = homeTeamId, awayTeamId = awayTeamId)) }) {
                Text("Salva")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
                DropdownMenuItem(
                    text = { Text(team.name) },
                    onClick = {
                        onSelect(team.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RefereeManagementList() {
    val db = FirebaseFirestore.getInstance()
    var referees by remember { mutableStateOf<List<User>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        db.collection("users").whereEqualTo("role", "referee").addSnapshotListener { snapshot, _ ->
            referees = snapshot?.toObjects(User::class.java) ?: emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(referees) { referee ->
                RefereeAdminCard(referee)
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi Arbitro")
        }
    }

    if (showAddDialog) {
        RefereeEditDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newReferee ->
                val docRef = db.collection("users").document()
                db.collection("users").document(docRef.id).set(newReferee.copy(uid = docRef.id))
                showAddDialog = false
            }
        )
    }
}

@Composable
fun RefereeAdminCard(referee: User) {
    val db = FirebaseFirestore.getInstance()
    var showEditDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(referee.displayName, style = MaterialTheme.typography.titleMedium)
                Text(referee.email, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { showEditDialog = true }) { Icon(Icons.Default.Edit, contentDescription = "Modifica") }
            IconButton(onClick = { db.collection("users").document(referee.uid).delete() }) { 
                Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error) 
            }
        }
    }

    if (showEditDialog) {
        RefereeEditDialog(
            referee = referee,
            onDismiss = { showEditDialog = false },
            onSave = { updatedReferee ->
                db.collection("users").document(referee.uid).set(updatedReferee)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefereeEditDialog(
    referee: User = User(),
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var name by remember { mutableStateOf(referee.displayName) }
    var email by remember { mutableStateOf(referee.email) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (referee.uid.isEmpty()) "Nuovo Arbitro" else "Modifica Arbitro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Nome Completo") })
                TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(referee.copy(displayName = name, email = email, role = "referee")) }) {
                Text("Salva")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
