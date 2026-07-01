package com.example.fairball.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

// ─────────────────────────────────────────────────────────────────────────────
// Helper Composables (Defined at top to ensure visibility)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeSectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

@Composable
fun DocMiniPreview(uri: String?, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        if (uri != null && uri != "null" && uri.isNotEmpty()) {
            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.size(100.dp).background(Color.LightGray, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.ImageNotSupported, null) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────────────

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
        if (effectiveUid == null && role != "admin") {
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("teams").addSnapshotListener { teamSnap, _ ->
            if (teamSnap != null) {
                teamsList = teamSnap.toObjects(Team::class.java)
                teamsMap = teamsList.associate { it.id to it.name }
            }
        }

        db.collection("matches").addSnapshotListener { matchSnap, _ ->
            if (matchSnap != null) {
                val matches = matchSnap.documents.mapNotNull { doc ->
                    doc.toObject(Match::class.java)?.copy(id = doc.id)
                }
                allMatches = matches

                if (role != "admin" && effectiveUid != null) {
                    myMatches = matches
                        .filter { (it.refereeId == effectiveUid || it.coRefereeId == effectiveUid) && it.status == "assigned" }
                        .sortedBy { it.scheduledAt?.seconds ?: 0L }

                    availableMatches = matches
                        .filter { it.status == "pending" && !it.refereeApplications.contains(effectiveUid) }
                        .sortedBy { it.scheduledAt?.seconds ?: 0L }
                }
                isLoading = false
            }
        }

        if (role == "admin") {
            db.collection("users").whereEqualTo("role", "referee").addSnapshotListener { refSnap, _ ->
                if (refSnap != null) allReferees = refSnap.toObjects(User::class.java)
            }
        }
    }

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

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                if (role == "admin") {
                    AdminHomeContent(allMatches, allReferees, teamsList, onViewRefereeProfile)
                } else {
                    RefereeHomeContent(effectiveUid, myMatches, availableMatches, teamsMap, db, onViewMap, onArbitrateMatch, allMatches)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onViewChampionship, modifier = Modifier.fillMaxWidth()) { Text("Vedi Tutto il Campionato") }
        }
    }
}

@Composable
fun ColumnScope.AdminHomeContent(
    allMatches: List<Match>,
    allReferees: List<User>,
    teamsList: List<Team>,
    onViewRefereeProfile: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val pendingApps = allMatches.filter { it.status == "pending" && it.refereeApplications.isNotEmpty() }
    val waitingAppr = allMatches.filter { it.status == "waiting_approval" }
    val totalTasks = pendingApps.size + waitingAppr.size

    TabRow(selectedTabIndex = selectedTab) {
        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = {
            BadgedBox(badge = { if(totalTasks > 0) Badge { Text("$totalTasks") } }) {
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
                    items(pendingApps) { MatchApplicationCard(it, allReferees, teamsList) }
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
                    items(allMatches.sortedByDescending { it.scheduledAt }) { HomeMatchAdminCard(it, allReferees, teamsList) }
                    item { Spacer(Modifier.height(80.dp)) }
                }
                val db = FirebaseFirestore.getInstance()
                var showAddDialog by remember { mutableStateOf(false) }
                FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 8.dp)) { Icon(Icons.Default.Add, null) }
                if (showAddDialog) {
                    MatchEditDialog(teams = teamsList, onDismiss = { showAddDialog = false }, onSave = { newMatch ->
                        db.collection("matches").document().set(newMatch.copy(id = java.util.UUID.randomUUID().toString()))
                        showAddDialog = false
                    })
                }
            }
        }
        2 -> {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allReferees) { RefereeAdminCard(it, onViewProfile = { onViewRefereeProfile(it.uid) }) }
            }
        }
    }
}

@Composable
fun ColumnScope.RefereeHomeContent(
    effectiveUid: String?,
    myMatches: List<Match>,
    availableMatches: List<Match>,
    teamsMap: Map<String, String>,
    db: FirebaseFirestore,
    onViewMap: () -> Unit,
    onArbitrateMatch: (String) -> Unit,
    allMatches: List<Match>
) {
    val myPendingApps = allMatches.filter { it.status == "pending" && it.refereeApplications.contains(effectiveUid) }
    val myWaitingAppr = allMatches.filter { it.status == "waiting_approval" && it.refereeId == effectiveUid }

    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(onClick = onViewMap, modifier = Modifier.fillMaxWidth().height(60.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, null); Spacer(Modifier.width(8.dp)); Text("Mappa Palestre", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (myMatches.isNotEmpty()) {
            item { HomeSectionTitle("I Miei Impegni Confermati", Icons.Default.EventAvailable) }
            items(myMatches) { MyMatchCard(it, teamsMap, onVai = { onArbitrateMatch(it.id) }, onDisdici = { db.collection("matches").document(it.id).update(mapOf("refereeId" to null, "status" to "pending")) }) }
        }

        if (myPendingApps.isNotEmpty() || myWaitingAppr.isNotEmpty()) {
            item { HomeSectionTitle("In Sospeso", Icons.Default.HourglassEmpty) }
            items(myPendingApps) { match ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${teamsMap[match.homeTeamId]} vs ${teamsMap[match.awayTeamId]}", fontWeight = FontWeight.Bold)
                            Text("Richiesta di prenotazione inviata", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { db.collection("matches").document(match.id).update("refereeApplications", FieldValue.arrayRemove(effectiveUid)) }) { Icon(Icons.Default.Cancel, null, tint = Color.Red) }
                    }
                }
            }
            items(myWaitingAppr) { match ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${teamsMap[match.homeTeamId]} vs ${teamsMap[match.awayTeamId]}", fontWeight = FontWeight.Bold)
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
                AvailableMatchCard(match, teamsMap, onPrenotati = { db.collection("matches").document(match.id).update("refereeApplications", FieldValue.arrayUnion(effectiveUid)) })
            }
        }
    }
}

@Composable
fun MatchApplicationCard(match: Match, referees: List<User>, teams: List<Team>) {
    val db = FirebaseFirestore.getInstance()
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Gara ${match.code}: ${teams.find{it.id==match.homeTeamId}?.name} vs ${teams.find{it.id==match.awayTeamId}?.name}", fontWeight = FontWeight.Bold)
            Text("${match.scheduledAt?.toFormattedDate()} ${match.scheduledAt?.toFormattedTime()}", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Text("Candidati:", style = MaterialTheme.typography.labelMedium)
            match.refereeApplications.forEach { uid ->
                val ref = referees.find { it.uid == uid }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(ref?.displayName ?: "Sconosciuto", modifier = Modifier.weight(1f))
                    Button(onClick = { db.collection("matches").document(match.id).update(mapOf("refereeId" to uid, "status" to "assigned", "assignedAt" to Timestamp.now(), "refereeApplications" to emptyList<String>())) }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(30.dp)) { Text("Scegli", fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
fun MatchApprovalCard(match: Match, referees: List<User>, teams: List<Team>) {
    val db = FirebaseFirestore.getInstance()
    var showDocs by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${teams.find{it.id==match.homeTeamId}?.name} ${match.homeScore} - ${match.awayScore} ${teams.find{it.id==match.awayTeamId}?.name}", fontWeight = FontWeight.Bold)
                    Text("Arbitro: ${referees.find{it.uid==match.refereeId}?.displayName}", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { db.collection("matches").document(match.id).update("status", "finished") }) { Text("Approva") }
            }
            TextButton(onClick = { showDocs = !showDocs }) { Text(if(showDocs) "Nascondi Documenti" else "Verifica Documenti") }
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
fun HomeMatchAdminCard(match: Match, referees: List<User>, teams: List<Team>) {
    val db = FirebaseFirestore.getInstance()
    var showEditDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var isAssigningCoReferee by remember { mutableStateOf(false) }

    val homeName = teams.find { it.id == match.homeTeamId }?.name ?: match.homeTeamId
    val awayName = teams.find { it.id == match.awayTeamId }?.name ?: match.awayTeamId
    val refName = referees.find { it.uid == match.refereeId }?.displayName ?: "—"
    val coRefName = referees.find { it.uid == match.coRefereeId }?.displayName

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gara ${match.code} [${match.status.uppercase()}]", fontSize = 12.sp, color = Color.Gray)
                    Text("$homeName vs $awayName", fontWeight = FontWeight.Bold)
                    Text("Arbitri: $refName ${if(coRefName!=null) "/ $coRefName" else ""}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { showEditDialog = true }) { Icon(Icons.Default.Edit, null) }
                IconButton(onClick = { db.collection("matches").document(match.id).delete() }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { isAssigningCoReferee = false; showAssignDialog = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("Assegna 1°", fontSize = 11.sp) }
                OutlinedButton(onClick = { isAssigningCoReferee = true; showAssignDialog = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("Assegna 2°", fontSize = 11.sp) }
            }
        }
    }

    if (showEditDialog) { MatchEditDialog(match = match, teams = teams, onDismiss = { showEditDialog = false }, onSave = { db.collection("matches").document(match.id).set(it); showEditDialog = false }) }
    if (showAssignDialog) {
        AlertDialog(onDismissRequest = { showAssignDialog = false }, title = { Text("Assegna Arbitro") }, text = {
            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                items(referees) { r ->
                    TextButton(onClick = {
                        val field = if (isAssigningCoReferee) "coRefereeId" else "refereeId"
                        db.collection("matches").document(match.id).update(mapOf(field to r.uid, "status" to "assigned"))
                        showAssignDialog = false
                    }, modifier = Modifier.fillMaxWidth()) { Text(r.displayName) }
                }
            }
        }, confirmButton = { TextButton(onClick = { showAssignDialog = false }) { Text("Chiudi") } })
    }
}
