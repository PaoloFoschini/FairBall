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
            matchId = match.id,
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
    matchId: String,
    referees: List<User>,
    isCoReferee: Boolean,
    currentRefereeId: String?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCoReferee) "Assegna Co-Arbitro" else "Assegna Arbitro Principale") },
        text = {
            LazyColumn {
                item {
                    TextButton(
                        onClick = {
                            scope.launch {
                                FirestoreRepository.removeReferee(matchId, isCoReferee)
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
                items(referees) { referee ->
                    val isCurrent = referee.uid == currentRefereeId
                    TextButton(
                        onClick = {
                            scope.launch {
                                FirestoreRepository.assignReferee(matchId, referee.uid, isCoReferee)
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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        }
    )
}

@Composable
fun RefereeManagementList(onViewProfile: ((String) -> Unit)? = null) {
    val referees by FirestoreRepository.refereesFlow().collectAsState(initial = null)

    when {
        referees == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        referees!!.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessun arbitro registrato.", color = Color.Gray)
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(referees!!, key = { it.uid }) { referee ->
                    RefereeAdminCard(
                        referee = referee,
                        onViewProfile = onViewProfile?.let { { it(referee.uid) } }
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
                    Icon(Icons.Default.Person, contentDescription = "Vedi profilo", tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Elimina arbitro", tint = MaterialTheme.colorScheme.error)
            }
        }
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