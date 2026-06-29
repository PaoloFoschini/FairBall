package com.example.fairball.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    debugUid: String? = null,
    onBack: () -> Unit, 
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    val effectiveUid = debugUid ?: currentUser?.uid

    var userProfile by remember { mutableStateOf<User?>(null) }
    var assignedMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var pastMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var teamsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var refereeStat by remember { mutableStateOf<RefereeStat?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            effectiveUid?.let { uid ->
                db.collection("users").document(uid).update("photoUrl", it.toString())
                userProfile = userProfile?.copy(photoUrl = it.toString())
            }
        }
    }

    LaunchedEffect(effectiveUid) {
        if (effectiveUid != null) {
            db.collection("teams").get().addOnSuccessListener { teamSnap ->
                teamsMap = teamSnap.toObjects(Team::class.java).associate { it.id to it.name }
                
                db.collection("users").document(effectiveUid).get().addOnSuccessListener { doc ->
                    val user = doc.toObject(User::class.java)
                    userProfile = user

                    db.collection("matches").get().addOnSuccessListener { matchSnapshot ->
                        val allMatches = matchSnapshot.toObjects(Match::class.java)
                        val myMatches = allMatches.filter { it.refereeId == effectiveUid || it.coRefereeId == effectiveUid }
                        
                        assignedMatches = myMatches.filter { it.status != "finished" }.sortedBy { it.scheduledAt }
                        pastMatches = myMatches.filter { it.status == "finished" }.sortedByDescending { it.scheduledAt }
                        
                        if (user != null) {
                            refereeStat = calculateRefereeStats(user, allMatches)
                        }
                        isLoading = false
                    }.addOnFailureListener { isLoading = false }
                }.addOnFailureListener { isLoading = false }
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mio Profilo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Statistiche e Badge") },
                                onClick = { showMenu = false; coroutineScope.launch { listState.animateScrollToItem(1) } },
                                leadingIcon = { Icon(Icons.Default.Analytics, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Prossime Partite") },
                                onClick = { showMenu = false; coroutineScope.launch { listState.animateScrollToItem(2) } },
                                leadingIcon = { Icon(Icons.Default.Event, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Storico Partite") },
                                onClick = { showMenu = false; coroutineScope.launch { listState.animateScrollToItem(4) } },
                                leadingIcon = { Icon(Icons.Default.History, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Cambia Foto") },
                                onClick = { showMenu = false; photoLauncher.launch("image/*") },
                                leadingIcon = { Icon(Icons.Default.PhotoCamera, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = { showMenu = false; auth.signOut(); onLogout() },
                                leadingIcon = { Icon(Icons.Default.Logout, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Elimina Account", color = Color.Red) },
                                onClick = { showMenu = false; showDeleteDialog = true },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { ProfileHeader(userProfile) { photoLauncher.launch("image/*") } }
                
                item {
                    Text("I Miei Traguardi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    RefereeSummaryCard(refereeStat)
                }

                item { Text("Prossimi Impegni", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                if (assignedMatches.isEmpty()) {
                    item { Text("Nessuna partita programmata.", color = Color.Gray, fontSize = 14.sp) }
                } else {
                    items(assignedMatches) { MatchProfileCard(it, teamsMap) }
                }

                item { Text("Gare Dirette in Passato", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                if (pastMatches.isEmpty()) {
                    item { Text("Nessuna partita passata.", color = Color.Gray, fontSize = 14.sp) }
                } else {
                    items(pastMatches) { MatchProfileCard(it, teamsMap) }
                }
                
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Elimina Account") },
            text = { Text("Sei sicuro di voler eliminare definitivamente il tuo profilo? Perderai tutti i badge e lo storico partite.") },
            confirmButton = {
                TextButton(onClick = {
                    effectiveUid?.let { uid ->
                        db.collection("users").document(uid).delete().addOnSuccessListener {
                            currentUser?.delete()?.addOnCompleteListener { onLogout() } ?: onLogout()
                        }
                    }
                }) { Text("ELIMINA", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("ANNULLA") } }
        )
    }
}

@Composable
fun ProfileHeader(user: User?, onPhotoClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(120.dp).clickable { onPhotoClick() }, contentAlignment = Alignment.BottomEnd) {
            if (user?.photoUrl != null) {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize().clip(CircleShape).border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(60.dp))
                }
            }
            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(6.dp), tint = Color.White)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(user?.displayName ?: "Arbitro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    }
}

@Composable
fun RefereeSummaryCard(stat: RefereeStat?) {
    var expanded by remember { mutableStateOf(false) }
    stat?.let {
        Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Gare dirette", style = MaterialTheme.typography.labelMedium)
                        Text("${stat.matchCount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                    Row {
                        stat.badges.filter { it.isUnlocked }.take(4).forEach {
                            Icon(it.icon, null, tint = it.color, modifier = Modifier.size(28.dp).padding(horizontal = 2.dp))
                        }
                    }
                }
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        stat.badges.forEach { BadgeItem(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchProfileCard(match: Match, teams: Map<String, String>) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Gara: ${match.code}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("${teams[match.homeTeamId] ?: match.homeTeamId} vs ${teams[match.awayTeamId] ?: match.awayTeamId}", fontWeight = FontWeight.Bold)
                Text(match.phase, style = MaterialTheme.typography.bodySmall)
            }
            if (match.status == "finished") {
                Text("${match.homeScore} - ${match.awayScore}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            } else {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                    Text("IN ARRIVO", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
