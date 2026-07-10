package com.example.fairball.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fairball.data.FirestoreRepository
import com.example.fairball.model.Match
import com.example.fairball.model.Team
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Schermata di arbitraggio di una partita.
 */
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
    var selectedDurationMinutes by remember { mutableIntStateOf(20) }

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

    val homeName = homeTeam?.name ?: currentMatch.homeTeamId.ifBlank { "Squadra Casa" }
    val awayName = awayTeam?.name ?: currentMatch.awayTeamId.ifBlank { "Squadra Ospiti" }

    Scaffold(
        topBar = { BackTopBar(title = "Arbitraggio", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable {
                            if (timeLeftSeconds == 0) {
                                timeLeftSeconds = selectedDurationMinutes * 60
                            }
                            isTimerRunning = !isTimerRunning
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (timeLeftSeconds > 0) formatTime(timeLeftSeconds) else "$selectedDurationMinutes:00",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isTimerRunning) "Premi per fermare" else " Premi per avviare",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.wrapContentSize()
                ) {
                    val opzioniMinuti = listOf(20, 5, 1)
                    for (minuti in opzioniMinuti) {
                        if (minuti != selectedDurationMinutes) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable {
                                        selectedDurationMinutes = minuti
                                        timeLeftSeconds = minuti * 60
                                        isTimerRunning = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$minuti min",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamScoreColumn(
                    modifier = Modifier.weight(1f),
                    name = homeName,
                    score = homeScore,
                    onIncrement = { homeScore++ },
                    onDecrement = { if (homeScore > 0) homeScore-- }
                )

                TeamScoreColumn(
                    modifier = Modifier.weight(1f),
                    name = awayName,
                    score = awayScore,
                    onIncrement = { awayScore++ },
                    onDecrement = { if (awayScore > 0) awayScore-- }
                )
            }

            Button(
                onClick = { onEndMatch(matchId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Fine partita", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Colonna con nome squadra, punteggio e pulsanti +/- per l'arbitraggio live.
 */
@Composable
private fun TeamScoreColumn(
    modifier: Modifier = Modifier,
    name: String,
    score: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = score.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDecrement) {
                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onIncrement) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Formatta i secondi in un formato MM:SS.
 */
fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.ROOT, "%02d:%02d", m, s)
}