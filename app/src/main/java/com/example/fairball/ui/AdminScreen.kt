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
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.example.fairball.model.Venue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Partite", "Utenti")

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
                1 -> UserManagementList()
            }
        }
    }
}

@Composable
fun MatchManagementList() {
    val scope = rememberCoroutineScope()
    val matches by FirestoreRepository.matchesFlow().collectAsState(initial = null)
    val teams by FirestoreRepository.teamsFlow().collectAsState(initial = null)
    val venues by FirestoreRepository.venuesFlow().collectAsState(initial = null)
    val referees by FirestoreRepository.refereesFlow().collectAsState(initial = null)
    var showAddDialog by remember { mutableStateOf(false) }

    val isLoading = matches == null || teams == null || venues == null || referees == null

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            matches!!.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessuna partita.", color = Color.Gray)
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(matches!!, key = { it.id }) { match ->
                        MatchAdminCard(
                            match = match,
                            referees = referees!!,
                            teams = teams!!,
                            venues = venues!!
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
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
            match = null,
            teams = teams ?: emptyList(),
            venues = venues ?: emptyList(),
            onDismiss = { showAddDialog = false },
            onSave = { newMatch ->
                scope.launch {
                    FirestoreRepository.createMatch(newMatch)
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun MatchAdminCard(
    match: Match,
    referees: List<User>,
    teams: List<Team>,
    venues: List<Venue>
) {
    val scope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var isAssigningCoReferee by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val homeTeamName = teams.find { it.id == match.homeTeamId }?.name ?: match.homeTeamId
    val awayTeamName = teams.find { it.id == match.awayTeamId }?.name ?: match.awayTeamId
    val refName = referees.find { it.uid == match.refereeId }?.displayName ?: "— nessuno —"
    val coRefName = referees.find { it.uid == match.coRefereeId }?.displayName
    val venueName = venues.find { it.id == match.venueId }?.name

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    if (venueName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            Spacer(Modifier.width(2.dp))
                            Text(venueName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    if (match.status == "finished") {
                        Text(
                            "Risultato: ${match.homeScore} – ${match.awayScore}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifica partita")
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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
                OutlinedButton(
                    onClick = { isAssigningCoReferee = false; showAssignDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Person, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Assegna", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(8.dp))
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

    if (showEditDialog) {
        MatchEditDialog(
            match = match,
            teams = teams,
            venues = venues,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                scope.launch {
                    FirestoreRepository.updateMatch(
                        matchId = match.id,
                        fields = mapOf(
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Elimina Partita") },
            text = { Text("Vuoi eliminare la gara ${match.code}? L'operazione è irreversibile.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        FirestoreRepository.deleteMatch(match.id)
                        showDeleteConfirm = false
                    }
                }) { Text("ELIMINA", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("ANNULLA") }
            }
        )
    }
}

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

    // Filtra gli arbitri in base ai criteri richiesti direttamente dall'oggetto match sincronizzato
    val availableReferees = referees.filter { referee ->
        // 1. Evita assegnazione contemporanea dello stesso utente come Arbitro e Co-Arbitro
        if (isCoReferee && match.refereeId == referee.uid) return@filter false
        if (!isCoReferee && match.coRefereeId == referee.uid) return@filter false

        // 2. Controllo conflitti orari (stesso giorno e stessa ora di inizio)
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

@Composable
fun UserManagementList(onViewProfile: ((String) -> Unit)? = null) {
    val users by FirestoreRepository.usersFlow().collectAsState(initial = null)

    when {
        users == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        users!!.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessun utente registrato.", color = Color.Gray)
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(users!!, key = { it.uid }) { user ->
                    RefereeAdminCard(
                        referee = user,
                        onViewProfile = onViewProfile?.let { { it(user.uid) } }
                    )
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun RefereeAdminCard(referee: User, onViewProfile: (() -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) } // <-- Aggiunto lo stato per mostrare il Dialog
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
                        referee.role == "admin" -> "Amministratore"
                        else -> "Arbitro"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (referee.role == "admin" || isSuperAdmin) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }

            if (isSuperAdmin) {
                // Il superadmin non può essere modificato né eliminato da nessuno.
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Account protetto",
                    tint = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                // Cambiato il click: ora imposta showEditDialog su true
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifica profilo", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina utente", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // <-- Inserito il controllo per mostrare il dialogo di modifica
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
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Elimina Arbitro") },
            text = { Text("Vuoi eliminare definitivamente l'account di ${referee.displayName}? L'operazione è irreversibile.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        FirestoreRepository.deleteUser(referee.uid)
                        showDeleteConfirm = false
                    }
                }) { Text("ELIMINA", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("ANNULLA") }
            }
        )
    }
}