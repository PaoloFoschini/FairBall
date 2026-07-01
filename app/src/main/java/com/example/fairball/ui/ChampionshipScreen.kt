package com.example.fairball.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fairball.model.Match
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampionshipScreen(
    onBack: () -> Unit,
    onViewReport: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var pastMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var allUsers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var allTeams by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("users").get().addOnSuccessListener { userSnap ->
            allUsers = userSnap.documents.associate { it.id to (it.getString("displayName") ?: "Sconosciuto") }
        }
        db.collection("teams").get().addOnSuccessListener { teamSnap ->
            allTeams = teamSnap.documents.associate { it.id to (it.getString("name") ?: it.id) }
        }
        db.collection("matches")
            .whereEqualTo("status", "finished")
            .addSnapshotListener { result, _ ->
                pastMatches = result?.documents?.mapNotNull { doc ->
                    doc.toObject(Match::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.scheduledAt?.seconds ?: 0L } ?: emptyList()
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campionato") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            pastMatches.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text("Nessuna partita conclusa.") }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pastMatches) { match ->
                        ChampionshipPastMatchItem(
                            match = match,
                            users = allUsers,
                            teams = allTeams,
                            onViewReport = {
                                if (match.id.isNotEmpty()) onViewReport(match.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChampionshipPastMatchItem(
    match: Match,
    users: Map<String, String>,
    teams: Map<String, String>,
    onViewReport: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            match.scheduledAt?.let {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        it.toFormattedDate(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        it.toFormattedTime(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        teams[match.homeTeamId] ?: match.homeTeamId,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        teams[match.awayTeamId] ?: match.awayTeamId,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "${match.homeScore} - ${match.awayScore}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Arbitro", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        users[match.refereeId] ?: "N/D",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedButton(onClick = onViewReport) { Text("DETTAGLI") }
            }
        }
    }
}