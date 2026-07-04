package com.example.fairball.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fairball.data.FirestoreRepository
import com.example.fairball.model.Match
import com.example.fairball.model.Team
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchRefereeScreen(
    matchId: String,
    onBack: () -> Unit,
    onEndMatch: (String) -> Unit
) {
    val matchFlow by FirestoreRepository.matchesFlow().collectAsState(initial = emptyList())
    val teamsFlow by FirestoreRepository.teamsFlow().collectAsState(initial = emptyList())

    var currentMatch by remember { mutableStateOf(Match(id = matchId, code = "Gara...")) }
    var homeTeam by remember { mutableStateOf<Team?>(null) }
    var awayTeam by remember { mutableStateOf<Team?>(null) }

    var homeScore by remember { mutableIntStateOf(0) }
    var awayScore by remember { mutableIntStateOf(0) }
    var isDataInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(matchFlow, teamsFlow, matchId) {
        val matchesList = matchFlow
        if (!matchesList.isNullOrEmpty()) {
            val foundMatch = matchesList.find { it.id == matchId }
            if (foundMatch != null) {
                currentMatch = foundMatch
                if (!isDataInitialized) {
                    homeScore = foundMatch.homeScore
                    awayScore = foundMatch.awayScore
                    isDataInitialized = true
                }

                val teamsList = teamsFlow
                if (!teamsList.isNullOrEmpty()) {
                    homeTeam = teamsList.find { it.id == foundMatch.homeTeamId }
                    awayTeam = teamsList.find { it.id == foundMatch.awayTeamId }
                }
            }
        }
    }

    var timeLeftSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(homeScore, awayScore) {
        if (isDataInitialized) {
            FirestoreRepository.updateScore(matchId, homeScore, awayScore)
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
        val homeName = homeTeam?.name ?: currentMatch.homeTeamId.ifBlank { "Squadra Casa" }
        val awayName = awayTeam?.name ?: currentMatch.awayTeamId.ifBlank { "Squadra Ospiti" }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = formatTime(timeLeftSeconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { timeLeftSeconds = 20 * 60; isTimerRunning = true },
                    shape = CircleShape,
                    modifier = Modifier.size(85.dp)
                ) { Text("20 min", style = MaterialTheme.typography.labelSmall) }

                Button(
                    onClick = { timeLeftSeconds = 5 * 60; isTimerRunning = true },
                    shape = CircleShape,
                    modifier = Modifier.size(75.dp)
                ) { Text("5 min", style = MaterialTheme.typography.labelSmall) }

                Button(
                    onClick = { timeLeftSeconds = 1 * 60; isTimerRunning = true },
                    shape = CircleShape,
                    modifier = Modifier.size(75.dp)
                ) { Text("1 min", style = MaterialTheme.typography.labelSmall) }
            }

            if (timeLeftSeconds > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(onClick = { isTimerRunning = !isTimerRunning }) {
                        Icon(if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    }
                    Text(if (isTimerRunning) "PAUSA" else "AVVIA", style = MaterialTheme.typography.labelMedium)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamScoreControl(
                    teamName = homeName,
                    score = homeScore,
                    onScoreChange = { homeScore = it }
                )
                Text("VS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                TeamScoreControl(
                    teamName = awayName,
                    score = awayScore,
                    onScoreChange = { awayScore = it }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onEndMatch(matchId) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Fine partita")
            }
        }
    }
}

@Composable
fun TeamScoreControl(teamName: String, score: Int, onScoreChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(teamName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Text(score.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (score > 0) onScoreChange(score - 1) }) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }
            OutlinedButton(onClick = { onScoreChange(score + 1) }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.ROOT, "%02d:%02d", m, s)
}