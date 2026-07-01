package com.example.fairball.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    refereeId: String?,
    onBack: () -> Unit,
    onViewMatchReport: (String) -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    val currentSessionUid = Session.uid
    val isMyProfile = refereeId == null || refereeId == currentSessionUid
    val targetUid = refereeId ?: currentSessionUid ?: ""

    var user by remember { mutableStateOf<User?>(null) }
    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var teams by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Launcher per cambiare l'immagine del profilo
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && targetUid.isNotEmpty()) {
            db.collection("users").document(targetUid)
                .update("photoUrl", uri.toString())
                .addOnSuccessListener {
                    user = user?.copy(photoUrl = uri.toString())
                }
        }
    }

    LaunchedEffect(targetUid) {
        if (targetUid.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("users").document(targetUid).get()
            .addOnSuccessListener { document ->
                user = document.toObject(User::class.java)

                db.collection("matches").get()
                    .addOnSuccessListener { matchSnapshots ->
                        matches = matchSnapshots.toObjects(Match::class.java)

                        db.collection("teams").get()
                            .addOnSuccessListener { teamSnapshots ->
                                teams = teamSnapshots.documents.associate {
                                    it.id to (it.getString("name") ?: it.id)
                                }
                                isLoading = false
                            }
                            .addOnFailureListener { isLoading = false }
                    }
                    .addOnFailureListener { isLoading = false }
            }
            .addOnFailureListener { isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isMyProfile) "Il Tuo Profilo" else user?.displayName ?: "Profilo Arbitro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (user == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Utente non trovato")
            }
        } else {
            val listState = rememberLazyListState()
            val userRole = user?.role ?: ""

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Profilo con supporto al cambio immagine se è il proprio profilo
                item {
                    ProfileHeader(
                        user = user!!,
                        isMyProfile = isMyProfile,
                        onEditPhoto = { imagePickerLauncher.launch("image/*") }
                    )
                }

                // Condizione: Mostra statistiche, badge e storico PARTITE SOLO SE l'utente NON è un Admin
                if (userRole != "admin") {
                    val stats = calculateRefereeStats(user!!, matches)

                    item {
                        Text("Badge e Traguardi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        stats.badges.forEach { badge ->
                            BadgeItem(badge = badge)
                        }
                    }

                    item {
                        Text("Storico Partite (${stats.matchCount})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    val myFinishedMatches = matches.filter {
                        (it.refereeId == user!!.uid || it.coRefereeId == user!!.uid) && it.status == "finished"
                    }

                    if (myFinishedMatches.isEmpty()) {
                        item {
                            Text("Nessuna partita disputata o approvata", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    } else {
                        items(myFinishedMatches) { match ->
                            MatchHistoryItem(match = match, teams = teams, onClick = { onViewMatchReport(match.id) })
                        }
                    }
                }

                // Pulsanti di Azione Account (Logout ed Eliminazione) per l'utente corrente (Sia Admin che Arbitro)
                if (isMyProfile) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    auth.signOut()
                                    Session.clear()
                                    onLogoutSuccess()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("LOGOUT")
                            }

                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = BorderStroke(1.dp, Color.Red)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                Spacer(Modifier.width(8.dp))
                                Text("ELIMINA ACCOUNT")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Elimina Account") },
            text = { Text("Sei sicuro di voler eliminare definitivamente il tuo account? Questa azione è irreversibile e rimuoverà tutti i tuoi dati da FairBall.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (targetUid.isNotEmpty()) {
                            db.collection("users").document(targetUid).delete()
                                .addOnCompleteListener {
                                    auth.currentUser?.delete()?.addOnCompleteListener {
                                        showDeleteConfirm = false
                                        Session.clear()
                                        onLogoutSuccess()
                                    } ?: run {
                                        showDeleteConfirm = false
                                        Session.clear()
                                        onLogoutSuccess()
                                    }
                                }
                        }
                    }
                ) {
                    Text("ELIMINA", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("ANNULLA")
                }
            }
        )
    }
}

@Composable
fun ProfileHeader(user: User, isMyProfile: Boolean, onEditPhoto: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(enabled = isMyProfile) { onEditPhoto() },
            contentAlignment = Alignment.Center
        ) {
            if (!user.photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = "Foto Profilo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (isMyProfile) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Cambia foto",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = user.displayName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(text = user.email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    }
}

@Composable
fun MatchHistoryItem(match: Match, teams: Map<String, String>, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        // Ripristiniamo il colore pieno di sfondo senza variazioni o bordi indotti
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Togliamo l'ombra sollevata
        border = null // Forza la rimozione di qualsiasi linea o bordo perimetrale grigio
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Gara: ${match.code}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        "${teams[match.homeTeamId] ?: match.homeTeamId} vs ${teams[match.awayTeamId] ?: match.awayTeamId}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(match.phase, style = MaterialTheme.typography.bodySmall)
                }
                if (match.status == "finished") {
                    Text(
                        "${match.homeScore} - ${match.awayScore}",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp
                    )
                }
            }
            if (match.status == "finished") {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Vedi referto completo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}