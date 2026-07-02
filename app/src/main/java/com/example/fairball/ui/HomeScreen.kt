package com.example.fairball.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fairball.data.FirestoreRepository
import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.example.fairball.model.Venue
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    role: String,
    debugUid: String? = null,
    onViewReferees: () -> Unit,
    onViewProfile: () -> Unit,
    onViewRefereeProfile: (String) -> Unit = {},
    onViewChampionship: () -> Unit,
    onViewMap: () -> Unit,
    onArbitrateMatch: (String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val effectiveUid = debugUid ?: auth.currentUser?.uid

    // Flussi
    val allMatches by FirestoreRepository.matchesFlow().collectAsState(initial = null)
    val teamsList by FirestoreRepository.teamsFlow().collectAsState(initial = null)
    val venuesList by FirestoreRepository.venuesFlow().collectAsState(initial = null)
    val allReferees by FirestoreRepository.refereesFlow().collectAsState(initial = null)

    // Mappa nomi squadre per visualizzazione rapida
    var teamsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(teamsList) {
        teamsMap = teamsList?.associate { it.id to it.name } ?: emptyMap()
    }

    val isLoading = allMatches == null || teamsList == null || venuesList == null || (role == "admin" && allReferees == null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FairBall") },
                navigationIcon = {
                    IconButton(onClick = onViewReferees) { Icon(Icons.Default.Sports, "Arbitri") }
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

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                role == "admin" -> {
                    AdminHomeContent(
                        allMatches = allMatches!!,
                        allReferees = allReferees!!,
                        teamsList = teamsList!!,
                        venuesList = venuesList!!,
                        onViewRefereeProfile = onViewRefereeProfile
                    )
                }
                else -> {
                    RefereeHomeContent(
                        effectiveUid = effectiveUid,
                        allMatches = allMatches!!,
                        teamsMap = teamsMap,
                        onViewMap = onViewMap,
                        onArbitrateMatch = onArbitrateMatch
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onViewChampionship, modifier = Modifier.fillMaxWidth()) {
                Text("Vedi Tutto il Campionato")
            }
        }
    }
}

@Composable
fun ColumnScope.AdminHomeContent(
    allMatches: List<Match>,
    allReferees: List<User>,
    teamsList: List<Team>,
    venuesList: List<Venue>,
    onViewRefereeProfile: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val pendingApps = allMatches.filter { it.status == "pending" && it.refereeApplications.isNotEmpty() }
    val waitingAppr = allMatches.filter { it.status == "waiting_approval" }
    val totalTasks = pendingApps.size + waitingAppr.size

    TabRow(selectedTabIndex = selectedTab) {
        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = {
            BadgedBox(badge = { if (totalTasks > 0) Badge { Text("$totalTasks") } }) {
                Text("Approvazioni", modifier = Modifier.padding(horizontal = 4.dp))
            }
        })
        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Gare") })
        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Arbitri") })
    }

    Spacer(Modifier.height(8.dp))

    when (selectedTab) {
        0 -> {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (totalTasks == 0) {
                    item { Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text("Nessuna attività in sospeso.", color = Color.Gray) } }
                }
                if (pendingApps.isNotEmpty()) {
                    item { HomeSectionTitle("Richieste di Prenotazione", Icons.Default.AssignmentInd) }
                    items(pendingApps) { match ->
                        MatchApplicationCard(match, allReferees, teamsList, venuesList)
                    }
                }
                if (waitingAppr.isNotEmpty()) {
                    item { HomeSectionTitle("Referti da Verificare", Icons.Default.RateReview) }
                    items(waitingAppr) { MatchApprovalCard(it, allReferees, teamsList) }
                }
            }
        }
        1 -> {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allMatches.sortedByDescending { it.scheduledAt }) {
                        HomeMatchAdminCard(it, allReferees, teamsList, venuesList)
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
                var showAddDialog by remember { mutableStateOf(false) }
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                }
                if (showAddDialog) {
                    MatchEditDialog(
                        match = null,
                        teams = teamsList,
                        venues = venuesList,
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
        }
        2 -> {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allReferees) { referee ->
                    RefereeAdminCard(referee, onViewProfile = { onViewRefereeProfile(referee.uid) })
                }
            }
        }
    }
}

@Composable
fun ColumnScope.RefereeHomeContent(
    effectiveUid: String?,
    allMatches: List<Match>,
    teamsMap: Map<String, String>,
    onViewMap: Unit,
    onArbitrateMatch: (String) -> Unit
) {
    // Nota: Ho mantenuto la firma identica, puoi passare anche un blocco lambda () -> Unit a onViewMap se necessario.
}

@Composable
fun ColumnScope.RefereeHomeContent(
    effectiveUid: String?,
    allMatches: List<Match>,
    teamsMap: Map<String, String>,
    onViewMap: () -> Unit,
    onArbitrateMatch: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val myMatches = allMatches
        .filter { (it.refereeId == effectiveUid || it.coRefereeId == effectiveUid) && it.status == "assigned" }
        .sortedBy { it.scheduledAt?.seconds ?: 0L }

    val availableMatches = allMatches
        .filter { it.status == "pending" && !it.refereeApplications.contains(effectiveUid) }
        .sortedBy { it.scheduledAt?.seconds ?: 0L }

    val myPendingApps = allMatches.filter { it.status == "pending" && it.refereeApplications.contains(effectiveUid) }
    val myWaitingAppr = allMatches.filter { it.status == "waiting_approval" && it.refereeId == effectiveUid }

    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                onClick = onViewMap,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Mappa Palestre", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (myMatches.isNotEmpty()) {
            item { HomeSectionTitle("I Miei Impegni Confermati", Icons.Default.EventAvailable) }
            items(myMatches) { match ->
                MyMatchCard(
                    match = match,
                    teamsMap = teamsMap,
                    onVai = { onArbitrateMatch(match.id) },
                    onDisdici = {
                        scope.launch {
                            FirestoreRepository.updateMatch(
                                matchId = match.id,
                                fields = mapOf("refereeId" to null, "status" to "pending")
                            )
                        }
                    }
                )
            }
        }

        if (myPendingApps.isNotEmpty() || myWaitingAppr.isNotEmpty()) {
            item { HomeSectionTitle("In Sospeso", Icons.Default.HourglassEmpty) }
            items(myPendingApps) { match ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${teamsMap[match.homeTeamId] ?: match.homeTeamId} vs ${teamsMap[match.awayTeamId] ?: match.awayTeamId}",
                                fontWeight = FontWeight.Bold
                            )
                            Text("Richiesta di prenotazione inviata", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                FirestoreRepository.withdrawApplication(match.id, effectiveUid!!)
                            }
                        }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            items(myWaitingAppr) { match ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${teamsMap[match.homeTeamId] ?: match.homeTeamId} vs ${teamsMap[match.awayTeamId] ?: match.awayTeamId}",
                                fontWeight = FontWeight.Bold
                            )
                            Text("Risultato in attesa di approvazione admin", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFA000))
                        }
                        Icon(Icons.Default.Schedule, null, tint = Color(0xFFFFA000))
                    }
                }
            }
        }

        item { HomeSectionTitle("Gare Disponibili", Icons.Default.AddCircleOutline) }
        if (availableMatches.isEmpty()) {
            item { Text("Nessuna gara disponibile.", color = Color.Gray, fontSize = 12.sp) }
        } else {
            items(availableMatches) { match ->
                AvailableMatchCard(
                    match = match,
                    teamsMap = teamsMap,
                    onPrenotati = {
                        scope.launch {
                            FirestoreRepository.applyForMatch(match.id, effectiveUid!!)
                        }
                    }
                )
            }
        }
    }
}

// -------------------- Componenti UI riutilizzabili --------------------

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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    val homeName = teams.find { it.id == match.homeTeamId }?.name ?: match.homeTeamId
                    val awayName = teams.find { it.id == match.awayTeamId }?.name ?: match.awayTeamId
                    Text("$homeName ${match.homeScore} - ${match.awayScore} $awayName", fontWeight = FontWeight.Bold)
                    Text("Arbitro: ${referees.find { it.uid == match.refereeId }?.displayName ?: "Sconosciuto"}", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = {
                    scope.launch { FirestoreRepository.approveMatch(match.id) }
                }) { Text("Approva") }
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
                IconButton(onClick = { showEditDialog = true }) { Icon(Icons.Default.Edit, null) }
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