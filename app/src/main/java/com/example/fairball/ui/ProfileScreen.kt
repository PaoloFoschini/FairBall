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
    onLogout: () -> Unit,
    onViewMatchReport: (String) -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    // myUid è l'uid di chi ha effettuato il login (debug o reale).
    // NON usiamo currentUser?.uid da solo perché in modalità Debug
    // FirebaseAuth.currentUser è sempre null.
    val myUid = Session.uid
    val effectiveUid = debugUid ?: myUid
    val isOwnProfile = effectiveUid != null && effectiveUid == myUid

    var userProfile by remember { mutableStateOf<User?>(null) }
    var pastMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var teamsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var refereeStat by remember { mutableStateOf<RefereeStat?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var otherAdminCount by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()
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

                    // Fetch matches and stats even for own profile if user is a referee
                    db.collection("matches").get().addOnSuccessListener { matchSnapshot ->
                        val allMatches = matchSnapshot.toObjects(Match::class.java)
                        val myMatches = allMatches.filter { it.refereeId == effectiveUid || it.coRefereeId == effectiveUid }

                        pastMatches = myMatches.filter { it.status == "finished" }.sortedByDescending { it.scheduledAt }

                        if (user != null) {
                            refereeStat = calculateRefereeStats(user, allMatches)
                        }

                        if (user?.role == "admin" && isOwnProfile) {
                            db.collection("users").whereEqualTo("role", "admin").get().addOnSuccessListener { adminSnap ->
                                otherAdminCount = adminSnap.size() - 1
                                isLoading = false
                            }
                        } else {
                            isLoading = false
                        }
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
                title = {
                    val titleText = if (isOwnProfile) "Mio Profilo" else "Profilo Arbitro"
                    Text(titleText)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
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
                item {
                    ProfileHeader(
                        user = userProfile,
                        isOwnProfile = isOwnProfile,
                        onPhotoClick = { if (isOwnProfile) photoLauncher.launch("image/*") }
                    )
                }

                if (userProfile?.role != "admin") {
                    item {
                        val statTitle = if (isOwnProfile) "Le Mie Statistiche" else "Traguardi e Badge"
                        Text(statTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        RefereeSummaryCard(refereeStat)
                    }

                    item {
                        val matchTitle = if (isOwnProfile) "Le Mie Gare Dirette" else "Partite Arbitrate"
                        Text(matchTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (pastMatches.isEmpty()) {
                        item { Text("Nessuna partita arbitrata.", color = Color.Gray, fontSize = 14.sp) }
                    } else {
                        items(pastMatches) { match ->
                            MatchProfileCard(match, teamsMap, onViewMatchReport)
                        }
                    }
                }

                if (isOwnProfile) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { auth.signOut(); onLogout() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Logout, null)
                                Spacer(Modifier.width(8.dp))
                                Text("LOGOUT")
                            }
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Icon(Icons.Default.Delete, null)
                                Spacer(Modifier.width(8.dp))
                                Text("ELIMINA ACCOUNT")
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }

    if (showDeleteDialog) {
        val canDelete = userProfile?.role != "admin" || otherAdminCount > 0
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Elimina Account") },
            text = {
                if (canDelete) {
                    Text("Sei sicuro di voler eliminare definitivamente il tuo profilo? Questa azione è irreversibile.")
                } else {
                    Text("Non puoi eliminare il tuo account perché sei l'unico Admin. Nomina un altro Admin prima di procedere.")
                }
            },
            confirmButton = {
                if (canDelete) {
                    TextButton(onClick = {
                        effectiveUid?.let { uid ->
                            db.collection("users").document(uid).delete().addOnSuccessListener {
                                currentUser?.delete()?.addOnCompleteListener { onLogout() } ?: onLogout()
                            }
                        }
                    }) { Text("ELIMINA", color = Color.Red) }
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(if (canDelete) "ANNULLA" else "OK") } }
        )
    }
}

@Composable
fun ProfileHeader(user: User?, isOwnProfile: Boolean, onPhotoClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .then(if (isOwnProfile) Modifier.clickable { onPhotoClick() } else Modifier),
            contentAlignment = Alignment.BottomEnd
        ) {
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
            if (isOwnProfile) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(6.dp), tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(user?.displayName ?: "Utente", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        if (user?.role == "admin") {
            AssistChip(onClick = {}, label = { Text("ADMIN") }, leadingIcon = { Icon(Icons.Default.Security, null, Modifier.size(18.dp)) })
        }
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
fun MatchProfileCard(match: Match, teams: Map<String, String>, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick(match.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gara: ${match.code}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${teams[match.homeTeamId] ?: match.homeTeamId} vs ${teams[match.awayTeamId] ?: match.awayTeamId}", fontWeight = FontWeight.Bold)
                    Text(match.phase, style = MaterialTheme.typography.bodySmall)
                }
                if (match.status == "finished") {
                    Text("${match.homeScore} - ${match.awayScore}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                }
            }
            if (match.status == "finished") {
                Spacer(Modifier.height(8.dp))
                Text("Vedi referto completo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}