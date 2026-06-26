package com.example.fairball.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchRefereeScreen(
    matchId: String,
    onBack: () -> Unit,
    onEndMatch: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var match by remember { mutableStateOf<Match?>(null) }
    var homeTeam by remember { mutableStateOf<Team?>(null) }
    var awayTeam by remember { mutableStateOf<Team?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var homeScore by remember { mutableIntStateOf(0) }
    var awayScore by remember { mutableIntStateOf(0) }

    // Timer state
    var timeLeftSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(matchId) {
        db.collection("matches").document(matchId).get().addOnSuccessListener { document ->
            val m = document.toObject(Match::class.java)
            match = m
            if (m != null) {
                homeScore = m.homeScore
                awayScore = m.awayScore
                
                // Fetch Team names
                db.collection("teams").document(m.homeTeamId).get().addOnSuccessListener { homeDoc ->
                    homeTeam = homeDoc.toObject(Team::class.java)
                }
                db.collection("teams").document(m.awayTeamId).get().addOnSuccessListener { awayDoc ->
                    awayTeam = awayDoc.toObject(Team::class.java)
                    isLoading = false
                }
            } else {
                isLoading = false
            }
        }
    }

    LaunchedEffect(isTimerRunning, timeLeftSeconds) {
        if (isTimerRunning && timeLeftSeconds > 0) {
            delay(1000L)
            timeLeftSeconds -= 1
        } else if (timeLeftSeconds == 0) {
            isTimerRunning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arbitraggio Partita") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (match != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Score Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamScoreControl(
                        teamName = homeTeam?.name ?: "Casa",
                        score = homeScore,
                        onScoreChange = { 
                            homeScore = it
                            db.collection("matches").document(matchId).update("homeScore", it)
                        }
                    )
                    Text("VS", style = MaterialTheme.typography.headlineMedium)
                    TeamScoreControl(
                        teamName = awayTeam?.name ?: "Ospiti",
                        score = awayScore,
                        onScoreChange = { 
                            awayScore = it
                            db.collection("matches").document(matchId).update("awayScore", it)
                        }
                    )
                }

                Divider()

                // Timer Section
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(timeLeftSeconds),
                        style = MaterialTheme.typography.displayLarge
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { timeLeftSeconds = 20 * 60; isTimerRunning = true }) { Text("20 min") }
                        Button(onClick = { timeLeftSeconds = 5 * 60; isTimerRunning = true }) { Text("5 min") }
                        Button(onClick = { timeLeftSeconds = 1 * 60; isTimerRunning = true }) { Text("1 min") }
                    }
                    
                    if (timeLeftSeconds > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        FilledIconButton(
                            onClick = { isTimerRunning = !isTimerRunning },
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isTimerRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isTimerRunning) "Pausa" else "Avvia",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Text(
                            text = if (isTimerRunning) "PAUSA" else "AVVIA",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { onEndMatch(matchId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("FINE PARTITA")
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Errore nel caricamento della partita")
            }
        }
    }
}

@Composable
fun TeamScoreControl(teamName: String, score: Int, onScoreChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(teamName, style = MaterialTheme.typography.titleMedium)
        Text(score.toString(), style = MaterialTheme.typography.displayMedium)
        Row {
            IconButton(onClick = { if (score > 0) onScoreChange(score - 1) }) {
                Text("-", style = MaterialTheme.typography.headlineMedium)
            }
            IconButton(onClick = { onScoreChange(score + 1) }) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
