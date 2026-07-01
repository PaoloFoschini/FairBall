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
import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

// ─────────────────────────────────────────────────────────────────────────────
// AdminScreen: schermata di gestione separata (accessibile via rotta "admin")
// ─────────────────────────────────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────────────────
// Lista partite (usata sia da AdminScreen che da HomeScreen admin)
// ─────────────────────────────────────────────────────────────────────────────
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
        if (matches.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessuna partita.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                item { Spacer(Modifier.height(8.dp)) }
                items(matches, key = { it.id }) { match ->
                    MatchAdminCard(match = match, referees = referees, teams = teams)
                    Spacer(Modifier.height(8.dp))
                }
                item { Spacer(Modifier.height(80.dp)) } // spazio per FAB
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

// ─────────────────────────────────────────────────────────────────────────────
// Card singola partita per l'admin
// Funzionalità: modifica squadre/punteggio · assegna arbitro (min 1, max 2) · elimina
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MatchAdminCard(match: Match, referees: List<User>, teams: List<Team>) {
    val db = FirebaseFirestore.getInstance()
    var showEditDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var isAssigningCoReferee by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val homeTeamName = teams.find { it.id == match.homeTeamId }?.name ?: match.homeTeamId
    val awayTeamName = teams.find { it.id == match.awayTeamId }?.name ?: match.awayTeamId
    val refName = referees.find { it.uid == match.refereeId }?.displayName ?: "— nessuno —"
    val coRefName = referees.find { it.uid == match.coRefereeId }?.displayName

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {

            // ── Intestazione partita ─────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Gara ${match.code}  ·  ${match.category}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Text(
                        "$homeTeamName vs $awayTeamName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (match.status == "finished") {
                        Text(
                            "Risultato: ${match.homeScore} – ${match.awayScore}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Pulsanti azione
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifica partita")
                }
                IconButton(onClick = {
                    showDeleteConfirm = true
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Sezione arbitri ──────────────────────────────────────────────
            Text("Arbitri", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(refName, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (coRefName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PersonAdd,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(coRefName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
                // Assegna arbitro principale
                OutlinedButton(
                    onClick = { isAssigningCoReferee = false; showAssignDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Person, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Assegna", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(8.dp))
                // Assegna co-arbitro (solo se l'arbitro principale è già assegnato)
                OutlinedButton(
                    onClick = { isAssigningCoReferee = true; showAssignDialog = true },
                    enabled = !match.refereeId.isNullOrEmpty(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.GroupAdd, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Co-Arb.", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    // ── Dialog: modifica squadre e punteggio ─────────────────────────────────
    if (showEditDialog) {
        MatchEditDialog(
            match = match,
            teams = teams,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                db.collection("matches").document(match.id).update(
                    mapOf(
                        "homeTeamId" to updated.homeTeamId,
                        "awayTeamId" to updated.awayTeamId,
                        "homeScore" to updated.homeScore,
                        "awayScore" to updated.awayScore,
                        "code" to updated.code,
                        "category" to updated.category,
                        "phase" to updated.phase,
                        "venueId" to updated.venueId
                    )
                )
                showEditDialog = false
            }
        )
    }

    // ── Dialog: assegnazione arbitro ─────────────────────────────────────────
    if (showAssignDialog) {
        val title = if (isAssigningCoReferee) "Assegna Co-Arbitro" else "Assegna Arbitro Principale"
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text(title) },
            text = {
                LazyColumn {
                    // Opzione "Rimuovi" per azzerare l'assegnazione
                    item {
                        TextButton(
                            onClick = {
                                val field = if (isAssigningCoReferee) "coRefereeId" else "refereeId"
                                db.collection("matches").document(match.id)
                                    .update(mapOf(field to null))
                                showAssignDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.Red)
                            Spacer(Modifier.width(8.dp))
                            Text("Rimuovi assegnazione", color = Color.Red)
                        }
                    }
                    items(referees) { referee ->
                        // Evidenzia quello già assegnato
                        val isCurrentRef = if (isAssigningCoReferee)
                            referee.uid == match.coRefereeId
                        else
                            referee.uid == match.refereeId

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
                            if (isCurrentRef) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                referee.displayName,
                                color = if (isCurrentRef) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                fontWeight = if (isCurrentRef) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAssignDialog = false }) { Text("Chiudi") }
            }
        )
    }

    // ── Dialog: conferma eliminazione partita ────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Elimina Partita") },
            text = { Text("Vuoi eliminare la gara ${match.code}? L'operazione è irreversibile.") },
            confirmButton = {
                TextButton(onClick = {
                    db.collection("matches").document(match.id).delete()
                    showDeleteConfirm = false
                }) { Text("ELIMINA", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("ANNULLA") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialog: crea / modifica partita
// Se viene passata una Match esistente, vengono precompilati i campi incluso il punteggio.
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchEditDialog(
    match: Match = Match(),
    teams: List<Team>,
    onDismiss: () -> Unit,
    onSave: (Match) -> Unit
) {
    val isEditing = match.id.isNotEmpty()

    var code by remember { mutableStateOf(match.code) }
    var category by remember { mutableStateOf(match.category.ifEmpty { "Maschile" }) }
    var phase by remember { mutableStateOf(match.phase.ifEmpty { "Regular Season" }) }
    var venue by remember { mutableStateOf(match.venueId) }
    var homeTeamId by remember { mutableStateOf(match.homeTeamId) }
    var awayTeamId by remember { mutableStateOf(match.awayTeamId) }
    var homeScoreStr by remember { mutableStateOf(match.homeScore.toString()) }
    var awayScoreStr by remember { mutableStateOf(match.awayScore.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Modifica Partita" else "Nuova Partita") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Codice Gara") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = venue,
                        onValueChange = { venue = it },
                        label = { Text("Impianto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Categoria:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Maschile", "Femminile", "Misto").forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
                item {
                    Text("Fase:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Regular Season", "Semifinale", "Finale").forEach { p ->
                            FilterChip(
                                selected = phase == p,
                                onClick = { phase = p },
                                label = { Text(p) }
                            )
                        }
                    }
                }
                item {
                    Text("Squadra Casa:", style = MaterialTheme.typography.labelMedium)
                    TeamDropdown(teams, homeTeamId) { homeTeamId = it }
                }
                item {
                    Text("Squadra Ospiti:", style = MaterialTheme.typography.labelMedium)
                    TeamDropdown(teams, awayTeamId) { awayTeamId = it }
                }
                // Punteggio: visibile sempre in modalità modifica, nascosto in creazione
                if (isEditing) {
                    item {
                        Text("Punteggio (correzione):", style = MaterialTheme.typography.labelMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = homeScoreStr,
                                onValueChange = { homeScoreStr = it.filter { c -> c.isDigit() } },
                                label = { Text("Casa") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Text("–", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = awayScoreStr,
                                onValueChange = { awayScoreStr = it.filter { c -> c.isDigit() } },
                                label = { Text("Ospiti") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    match.copy(
                        code = code,
                        category = category,
                        phase = phase,
                        venueId = venue,
                        homeTeamId = homeTeamId,
                        awayTeamId = awayTeamId,
                        homeScore = homeScoreStr.toIntOrNull() ?: match.homeScore,
                        awayScore = awayScoreStr.toIntOrNull() ?: match.awayScore,
                        scheduledAt = match.scheduledAt ?: Timestamp.now()
                    )
                )
            }) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Lista arbitri (usata sia da AdminScreen che da HomeScreen admin)
// Permette di visualizzare il profilo e di eliminare l'account di un arbitro.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RefereeManagementList(
    onViewProfile: ((String) -> Unit)? = null
) {
    val db = FirebaseFirestore.getInstance()
    var referees by remember { mutableStateOf<List<User>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("users").whereEqualTo("role", "referee").addSnapshotListener { snapshot, _ ->
            referees = snapshot?.toObjects(User::class.java) ?: emptyList()
        }
    }

    if (referees.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nessun arbitro registrato.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(referees, key = { it.uid }) { referee ->
                RefereeAdminCard(
                    referee = referee,
                    onViewProfile = onViewProfile?.let { cb -> { cb(referee.uid) } }
                )
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card singolo arbitro per la vista admin
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RefereeAdminCard(
    referee: User,
    onViewProfile: (() -> Unit)? = null
) {
    val db = FirebaseFirestore.getInstance()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(referee.displayName, style = MaterialTheme.typography.titleMedium)
                Text(referee.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (onViewProfile != null) {
                IconButton(onClick = onViewProfile) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Vedi profilo",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Elimina arbitro",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Elimina Arbitro") },
            text = {
                Text(
                    "Vuoi eliminare definitivamente l'account di ${referee.displayName}? " +
                            "L'operazione è irreversibile."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // Elimina il documento Firestore dell'arbitro.
                    // L'account Firebase Auth rimane (richiede Admin SDK lato server per rimuoverlo),
                    // ma l'utente non potrà più accedere all'app in modo significativo.
                    db.collection("users").document(referee.uid).delete()
                    showDeleteConfirm = false
                }) { Text("ELIMINA", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("ANNULLA") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dropdown selezione squadra (usato in MatchEditDialog)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TeamDropdown(teams: List<Team>, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTeam = teams.find { it.id == selectedId }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedTeam?.name ?: "Seleziona Squadra")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            teams.forEach { team ->
                DropdownMenuItem(
                    text = { Text(team.name) },
                    onClick = { onSelect(team.id); expanded = false }
                )
            }
        }
    }
}