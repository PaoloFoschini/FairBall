package com.example.fairball.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fairball.data.FirestoreRepository
import com.example.fairball.data.ThemePreference
import com.example.fairball.model.Match
import com.example.fairball.model.MatchStatus
import com.example.fairball.model.UserRole
import com.example.fairball.model.statusEnum
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Schermata di visualizzazione del profilo utente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    refereeId: String?,
    onBack: () -> Unit,
    onViewMatchReport: (String) -> Unit,
    onLogoutSuccess: () -> Unit,
    currentTheme: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentSessionUid = Session.uid
    val isMyProfile = refereeId == null || refereeId == currentSessionUid
    val targetUid = refereeId ?: currentSessionUid ?: ""

    val allUsers by FirestoreRepository.usersFlow().collectAsState(initial = null)
    val allMatches by FirestoreRepository.matchesFlow().collectAsState(initial = null)
    val allTeams by FirestoreRepository.teamsFlow().collectAsState(initial = null)

    var user by remember { mutableStateOf<com.example.fairball.model.User?>(null) }
    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var teams by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(allUsers, allMatches, allTeams, targetUid) {
        if (allUsers == null || allMatches == null || allTeams == null) return@LaunchedEffect
        user = allUsers!!.find { it.uid == targetUid }
        matches = allMatches!!.filter { match ->
            (match.refereeId == targetUid || match.coRefereeId == targetUid) && match.statusEnum == MatchStatus.FINISHED
        }
        teams = allTeams!!.associate { it.id to it.name }
        isLoading = false
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && targetUid.isNotEmpty()) {
            scope.launch {
                FirestoreRepository.updateUserPhoto(targetUid, uri.toString())
                user = user?.copy(photoUrl = uri.toString())
            }
        }
    }

    Scaffold(
        topBar = {
            BackTopBar(
                title = if (isMyProfile) "Il Tuo Profilo" else user?.displayName ?: "Profilo Arbitro",
                onBack = onBack
            )
        }
    ) { padding ->
        if (isLoading) {
            LoadingBox(modifier = Modifier.fillMaxSize().padding(padding))
        } else if (user == null) {
            EmptyStateBox(
                "Utente non trovato",
                modifier = Modifier.fillMaxSize().padding(padding),
                textColor = Color.Unspecified
            )
        } else {
            val listState = rememberLazyListState()
            val userRole = user?.role ?: ""

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ProfileHeader(
                        user = user!!,
                        isMyProfile = isMyProfile,
                        onEditPhoto = { imagePickerLauncher.launch("image/*") },
                        onEditDetails = { showEditDialog = true }
                    )
                }

                if (UserRole.fromRaw(userRole) != UserRole.ADMIN) {
                    val stats = calculateRefereeStats(user!!, matches)
                    item {
                        Text("Badge e Traguardi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        stats.badges.forEach { badge -> BadgeItem(badge = badge) }
                    }

                    item {
                        Text("Storico Partite (${stats.matchCount})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    if (matches.isEmpty()) {
                        item {
                            Text("Nessuna partita disputata o approvata", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    } else {
                        items(matches) { match ->
                            MatchHistoryItem(match = match, teams = teams, onClick = { onViewMatchReport(match.id) })
                        }
                    }
                }

                if (isMyProfile) {
                    item {
                        Text("Impostazioni Tema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Column {
                            listOf(
                                ThemePreference.SYSTEM to "Sistema (segue dispositivo)",
                                ThemePreference.LIGHT to "Chiaro",
                                ThemePreference.DARK to "Scuro",
                                ThemePreference.CUSTOM to "Personalizzato (verde/arancio)"
                            ).forEach { (pref, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onThemeChange(pref) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = currentTheme == pref,
                                        onClick = { onThemeChange(pref) }
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(16.dp))
                    }
                }

                if (isMyProfile) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    auth.signOut()
                                    Session.clear()
                                    onLogoutSuccess()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.ExitToApp, null)
                                Spacer(Modifier.width(8.dp))
                                Text("LOGOUT")
                            }
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = BorderStroke(1.dp, Color.Red)
                            ) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                                Spacer(Modifier.width(8.dp))
                                Text("ELIMINA ACCOUNT")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && user != null) {
        ProfileEditDialog(
            currentUser = user!!,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newEmail ->
                scope.launch {
                    try {
                        if (isMyProfile) {
                            val firebaseUser = auth.currentUser
                            if (firebaseUser != null && firebaseUser.email != newEmail) {
                                firebaseUser.verifyBeforeUpdateEmail(newEmail)
                                Toast.makeText(context, "Link di verifica inviato alla nuova e-mail", Toast.LENGTH_LONG).show()
                            }
                        }

                        FirestoreRepository.updateUserProfile(targetUid, newName, newEmail)

                        user = user?.copy(displayName = newName, email = newEmail)
                        showEditDialog = false
                    } catch (e: Exception) {
                        Toast.makeText(context, "Errore durante l'aggiornamento: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Elimina Account",
            message = "Sei sicuro di voler eliminare definitivamente il tuo account? Questa azione è irreversibile e rimuoverà tutti i tuoi dati da FairBall.",
            confirmBold = true,
            onConfirm = {
                scope.launch {
                    FirestoreRepository.deleteUser(targetUid)
                    auth.currentUser?.delete()
                    showDeleteConfirm = false
                    Session.clear()
                    onLogoutSuccess()
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

/**
 * Componente per la visualizzazione dell'header del profilo.
 */
@Composable
fun ProfileHeader(
    user: com.example.fairball.model.User,
    isMyProfile: Boolean,
    onEditPhoto: () -> Unit,
    onEditDetails: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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
                Icon(Icons.Default.Person, null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            if (isMyProfile) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Cambia foto", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(start = if (isMyProfile) 32.dp else 0.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(user.displayName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(user.email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            if (isMyProfile) {
                IconButton(onClick = onEditDetails, modifier = Modifier.padding(start = 8.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Modifica Anagrafica",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dialog di modifica profilo.
 */
@Composable
fun ProfileEditDialog(
    currentUser: com.example.fairball.model.User,
    onDismiss: () -> Unit,
    onSave: (newName: String, newEmail: String) -> Unit
) {
    var name by remember { mutableStateOf(currentUser.displayName) }
    var email by remember { mutableStateOf(currentUser.email) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica Profilo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Visualizzato") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Indirizzo E-mail") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && email.isNotBlank()) onSave(name, email) },
                enabled = name.isNotBlank() && email.isNotBlank()
            ) {
                Text("SALVA")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ANNULLA")
            }
        }
    )
}

/**
 * Componente per la visualizzazione dello storico delle partite di un arbitro.
 */
@Composable
fun MatchHistoryItem(match: Match, teams: Map<String, String>, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${teams.nameOf(match.homeTeamId)} vs ${teams.nameOf(match.awayTeamId)}",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Categoria: " + match.category, style = MaterialTheme.typography.bodySmall)
                }
                if (match.statusEnum == MatchStatus.FINISHED) {
                    Text(
                        "${match.homeScore} - ${match.awayScore}",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp
                    )
                }
            }
            if (match.statusEnum == MatchStatus.FINISHED) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}