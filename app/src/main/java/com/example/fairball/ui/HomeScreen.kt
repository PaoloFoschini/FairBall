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
import androidx.compose.ui.unit.sp
import com.example.fairball.data.FirestoreRepository
import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.example.fairball.model.Venue
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

    val allMatches by FirestoreRepository.matchesFlow().collectAsState(initial = null)
    val teamsList by FirestoreRepository.teamsFlow().collectAsState(initial = null)
    val venuesList by FirestoreRepository.venuesFlow().collectAsState(initial = null)
    val allReferees by FirestoreRepository.refereesFlow().collectAsState(initial = null)
    val allUsers by FirestoreRepository.usersFlow().collectAsState(initial = null)

    var teamsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(teamsList) {
        teamsMap = teamsList?.associate { it.id to it.name } ?: emptyMap()
    }

    val isLoading = allMatches == null || teamsList == null || venuesList == null ||
            (role == "admin" && (allReferees == null || allUsers == null))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FairBall") },
                navigationIcon = {
                    if (role == "admin") {
                        IconButton(onClick = onViewReferees) { Icon(Icons.Default.Sports, "Arbitri") }
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
                        allUsers = allUsers!!,
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
    allUsers: List<User>,
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
            Text("Richieste", modifier = Modifier.padding(horizontal = 4.dp))
        })
        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Gare") })
        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Utenti") })
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
                    items(waitingAppr) { match ->
                        MatchApprovalCard(match, allReferees, teamsList)
                    }
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
                items(allUsers, key = { it.uid }) { user ->
                    RefereeAdminCard(user, onViewProfile = { onViewRefereeProfile(user.uid) })
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

    val myWaitingAppr = allMatches.filter {
        (it.status == "waiting_approval" || it.status == "rejected") && it.refereeId == effectiveUid
    }

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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (match.status == "rejected")
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    else
                        CardDefaults.cardColors()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${teamsMap[match.homeTeamId] ?: match.homeTeamId} vs ${teamsMap[match.awayTeamId] ?: match.awayTeamId}",
                                    fontWeight = FontWeight.Bold
                                )
                                if (match.status == "rejected") {
                                    Text("❌ MODIFICHE RICHIESTE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Risultato in attesa di approvazione admin", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFA000))
                                }
                            }

                            if (match.status == "rejected") {
                                Button(onClick = { onArbitrateMatch(match.id) }) {
                                    Text("Modifica")
                                }
                            } else {
                                Icon(Icons.Default.Schedule, null, tint = Color(0xFFFFA000))
                            }
                        }

                        if (match.status == "rejected" && !match.adminComment.isNullOrEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Nota Admin: ${match.adminComment}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
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