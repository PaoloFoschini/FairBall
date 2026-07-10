package com.example.fairball.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fairball.data.FirestoreRepository
import com.example.fairball.model.Match
import com.example.fairball.model.MatchStatus
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.example.fairball.model.UserRole
import com.example.fairball.model.Venue
import com.example.fairball.model.roleEnum
import com.example.fairball.model.statusEnum
import com.example.fairball.ui.theme.AppColors
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Pagina principale dell'applicazione.
 */
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
    onArbitrateMatch: (String) -> Unit,
    onViewNotifications: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val effectiveUid = debugUid ?: auth.currentUser?.uid

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onViewMap()
    }

    val onViewMapWithLocationCheck: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            onViewMap()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val unreadCount by (effectiveUid?.let { FirestoreRepository.unreadNotificationCountFlow(it) }
        ?: kotlinx.coroutines.flow.flowOf(0)).collectAsState(initial = 0)
    var previousUnreadCount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(unreadCount) {
        val previous = previousUnreadCount
        if (previous != null && unreadCount > previous) {
            snackbarHostState.showSnackbar("Hai una nuova notifica")
        }
        previousUnreadCount = unreadCount
    }

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
            (UserRole.fromRaw(role) == UserRole.ADMIN && (allReferees == null || allUsers == null))

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("FairBall") },
                navigationIcon = {
                    IconButton(onClick = onViewReferees) { Icon(Icons.Default.Sports, "Arbitri") }
                },
                actions = {
                    IconButton(onClick = onViewNotifications) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(if (unreadCount > 9) "9+" else unreadCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifiche")
                        }
                    }
                    IconButton(onClick = onViewProfile) { Icon(Icons.Default.AccountCircle, "Profilo") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Benvenuto, ${if (UserRole.fromRaw(role) == UserRole.ADMIN) "Amministratore" else "Arbitro"}", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                UserRole.fromRaw(role) == UserRole.ADMIN -> {
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
                        onViewMap = onViewMapWithLocationCheck,
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

/**
 * Pagina home di amministrazione.
 */
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

    var richiesteCategoryFilter by remember { mutableStateOf<String?>(null) }
    var gareStatusFilter by remember { mutableStateOf<String?>(null) }
    var gareCategoryFilter by remember { mutableStateOf<String?>(null) }
    var gareSearchQuery by remember { mutableStateOf("") }
    var utentiRoleFilter by remember { mutableStateOf<String?>(null) }
    var utentiSearchQuery by remember { mutableStateOf("") }

    val pendingApps = allMatches.filter { it.statusEnum == MatchStatus.PENDING && it.refereeApplications.isNotEmpty() }
    val waitingAppr = allMatches.filter { it.statusEnum == MatchStatus.WAITING_APPROVAL }
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
            val richiesteCategories = (pendingApps + waitingAppr).map { it.category }.distinct().sorted()
            val filteredPendingApps = pendingApps.filter { richiesteCategoryFilter == null || it.category == richiesteCategoryFilter }
            val filteredWaitingAppr = waitingAppr.filter { richiesteCategoryFilter == null || it.category == richiesteCategoryFilter }
            val filteredTotalTasks = filteredPendingApps.size + filteredWaitingAppr.size

            Column(modifier = Modifier.weight(1f)) {
                if (richiesteCategories.size > 1) {
                    CategoryFilterRow(richiesteCategories, richiesteCategoryFilter) { richiesteCategoryFilter = it }
                    Spacer(Modifier.height(6.dp))
                }
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (totalTasks == 0) {
                        item { EmptyStateBox("Nessuna attività in sospeso.", modifier = Modifier.fillParentMaxSize()) }
                    } else if (filteredTotalTasks == 0) {
                        item { EmptyStateBox("Nessuna attività per questa categoria.", modifier = Modifier.fillParentMaxSize()) }
                    }
                    if (filteredPendingApps.isNotEmpty()) {
                        item { HomeSectionTitle("Richieste di Prenotazione", Icons.Default.AssignmentInd) }
                        items(filteredPendingApps) { match ->
                            MatchApplicationCard(match, allReferees, teamsList, venuesList)
                        }
                    }
                    if (filteredWaitingAppr.isNotEmpty()) {
                        item { HomeSectionTitle("Referti da Verificare", Icons.Default.RateReview) }
                        items(filteredWaitingAppr) { match ->
                            MatchApprovalCard(match, allReferees, teamsList)
                        }
                    }
                }
            }
        }
        1 -> {
            val gareStatuses = allMatches.map { it.status }.distinct()
            val gareCategories = allMatches.map { it.category }.distinct().sorted()
            val filteredMatches = allMatches
                .filter { gareStatusFilter == null || it.status == gareStatusFilter }
                .filter { gareCategoryFilter == null || it.category == gareCategoryFilter }
                .filter { match ->
                    if (gareSearchQuery.isBlank()) return@filter true
                    val homeName = teamsList.nameOf(match.homeTeamId)
                    val awayName = teamsList.nameOf(match.awayTeamId)
                    homeName.contains(gareSearchQuery, ignoreCase = true) ||
                            awayName.contains(gareSearchQuery, ignoreCase = true)
                }
                .sortedByDescending { it.scheduledAt }

            Column(modifier = Modifier.weight(1f)) {
                SearchField(gareSearchQuery, { gareSearchQuery = it }, "Cerca per squadra")
                if (gareStatuses.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    StatusFilterRow(gareStatuses, gareStatusFilter) { gareStatusFilter = it }
                }
                if (gareCategories.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    CategoryFilterRow(gareCategories, gareCategoryFilter) { gareCategoryFilter = it }
                }
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (filteredMatches.isEmpty()) {
                            item { EmptyStateBox("Nessuna gara trovata.", modifier = Modifier.fillParentMaxSize()) }
                        }
                        items(filteredMatches) {
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
        }
        2 -> {
            val filteredUsers = allUsers
                .filter { user ->
                    when (utentiRoleFilter) {
                        "admin" -> user.roleEnum == UserRole.ADMIN
                        "referee" -> user.roleEnum != UserRole.ADMIN
                        else -> true
                    }
                }
                .filter { user ->
                    utentiSearchQuery.isBlank() ||
                            user.displayName.contains(utentiSearchQuery, ignoreCase = true) ||
                            user.email.contains(utentiSearchQuery, ignoreCase = true)
                }

            Column(modifier = Modifier.weight(1f)) {
                SearchField(utentiSearchQuery, { utentiSearchQuery = it }, "Cerca per nome o email")
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterGroupLabel("Ruolo:")
                    CompactFilterChip("Tutti", utentiRoleFilter == null) { utentiRoleFilter = null }
                    CompactFilterChip("Amministratori", utentiRoleFilter == "admin") {
                        utentiRoleFilter = if (utentiRoleFilter == "admin") null else "admin"
                    }
                    CompactFilterChip("Arbitri", utentiRoleFilter == "referee") {
                        utentiRoleFilter = if (utentiRoleFilter == "referee") null else "referee"
                    }
                }
                Spacer(Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (filteredUsers.isEmpty()) {
                        item { EmptyStateBox("Nessun utente trovato.", modifier = Modifier.fillParentMaxSize()) }
                    }
                    items(filteredUsers, key = { it.uid }) { user ->
                        RefereeAdminCard(user, onViewProfile = { onViewRefereeProfile(user.uid) })
                    }
                }
            }
        }
    }
}

/**
 * Pagina home di un arbitro.
 */
@Composable
fun ColumnScope.RefereeHomeContent(
    effectiveUid: String?,
    allMatches: List<Match>,
    teamsMap: Map<String, String>,
    onViewMap: () -> Unit,
    onArbitrateMatch: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var availableCategoryFilter by remember { mutableStateOf<String?>(null) }
    var availableSearchQuery by remember { mutableStateOf("") }

    val myMatches = allMatches
        .filter { (it.refereeId == effectiveUid || it.coRefereeId == effectiveUid) && it.statusEnum == MatchStatus.ASSIGNED }
        .sortedBy { it.scheduledAt?.seconds ?: 0L }

    val availableMatchesAll = allMatches
        .filter { it.statusEnum == MatchStatus.PENDING && !it.refereeApplications.contains(effectiveUid) }
        .sortedBy { it.scheduledAt?.seconds ?: 0L }

    val availableCategories = availableMatchesAll.map { it.category }.distinct().sorted()

    val availableMatches = availableMatchesAll
        .filter { availableCategoryFilter == null || it.category == availableCategoryFilter }
        .filter { match ->
            if (availableSearchQuery.isBlank()) return@filter true
            val homeName = teamsMap.nameOf(match.homeTeamId)
            val awayName = teamsMap.nameOf(match.awayTeamId)
            homeName.contains(availableSearchQuery, ignoreCase = true) ||
                    awayName.contains(availableSearchQuery, ignoreCase = true)
        }

    val myPendingApps = allMatches.filter { it.statusEnum == MatchStatus.PENDING && it.refereeApplications.contains(effectiveUid) }

    val myWaitingAppr = allMatches.filter {
        // "rejected" non ha un caso MatchStatus corrispondente: confronto stringa intenzionale.
        (it.statusEnum == MatchStatus.WAITING_APPROVAL || it.status == "rejected") && it.refereeId == effectiveUid
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
                                fields = mapOf("refereeId" to null, "status" to MatchStatus.PENDING.raw)
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
                                "${teamsMap.nameOf(match.homeTeamId)} vs ${teamsMap.nameOf(match.awayTeamId)}",
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
                                    "${teamsMap.nameOf(match.homeTeamId)} vs ${teamsMap.nameOf(match.awayTeamId)}",
                                    fontWeight = FontWeight.Bold
                                )
                                if (match.status == "rejected") {
                                    Text("❌ MODIFICHE RICHIESTE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Risultato in attesa di approvazione admin", style = MaterialTheme.typography.labelSmall, color = AppColors.PendingApprovalAmber)
                                }
                            }

                            if (match.status == "rejected") {
                                Button(onClick = { onArbitrateMatch(match.id) }) {
                                    Text("Modifica")
                                }
                            } else {
                                Icon(Icons.Default.Schedule, null, tint = AppColors.PendingApprovalAmber)
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
        if (availableMatchesAll.isNotEmpty()) {
            item {
                Column {
                    SearchField(availableSearchQuery, { availableSearchQuery = it }, "Cerca per squadra")
                    if (availableCategories.size > 1) {
                        Spacer(Modifier.height(6.dp))
                        CategoryFilterRow(availableCategories, availableCategoryFilter) { availableCategoryFilter = it }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
        if (availableMatches.isEmpty()) {
            item {
                Text(
                    if (availableMatchesAll.isEmpty()) "Nessuna gara disponibile." else "Nessuna gara corrisponde ai filtri.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
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