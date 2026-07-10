package com.example.fairball.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.fairball.data.FirestoreRepository
import com.example.fairball.model.Match
import java.util.Locale

/**
 * Schermata di visualizzazione del report della partita.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchReportScreen(matchId: String, onClose: () -> Unit) {
    val allMatches by FirestoreRepository.matchesFlow().collectAsState(initial = null)
    val allTeams by FirestoreRepository.teamsFlow().collectAsState(initial = null)
    val allUsers by FirestoreRepository.usersFlow().collectAsState(initial = null)
    val allVenues by FirestoreRepository.venuesFlow().collectAsState(initial = null)

    var match by remember { mutableStateOf<Match?>(null) }
    var homeTeamName by remember { mutableStateOf("") }
    var awayTeamName by remember { mutableStateOf("") }
    var refereeName by remember { mutableStateOf("") }
    var venueName by remember { mutableStateOf("") }
    var category by remember {mutableStateOf("")}
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(allMatches, allTeams, allUsers, allVenues, matchId) {
        if (allMatches == null || allTeams == null || allUsers == null || allVenues == null) return@LaunchedEffect
        val m = allMatches!!.find { it.id == matchId }
        match = m
        if (m != null) {
            homeTeamName = allTeams!!.nameOf(m.homeTeamId)
            awayTeamName = allTeams!!.nameOf(m.awayTeamId)
            refereeName = allUsers!!.find { it.uid == m.refereeId }?.displayName ?: "Non assegnato"
            venueName = allVenues!!.find { it.id == m.venueId }?.name ?: m.venueId
            category = m.category
        }
        isLoading = false
    }

    Scaffold(
        topBar = { BackTopBar(title = "Dettaglio Partita", onBack = onClose) }
    ) { padding ->
        when {
            isLoading -> {
                LoadingBox(modifier = Modifier.fillMaxSize().padding(padding))
            }
            match == null -> {
                EmptyStateBox(
                    "Dati partita non trovati.",
                    modifier = Modifier.fillMaxSize().padding(padding),
                    textColor = Color.Unspecified
                )
            }
            else -> {
                val m = match!!
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                            Text(
                                text = m.phase.uppercase(Locale.ITALY),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))

                            val dateStr = m.scheduledAt?.let { "${it.toFormattedDate()} ${it.toFormattedTime()}" } ?: "Data non disponibile"

                            Text(
                                text = "Data: $dateStr",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "Luogo: $venueName",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "Categoria: $category",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(14.dp))

                            HorizontalDivider()
                            Spacer(Modifier.height(14.dp))

                            Text("Risultato Finale", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ScoreDisplay(homeTeamName.ifEmpty { m.homeTeamId }, m.homeScore)
                                Text("-", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                ScoreDisplay(awayTeamName.ifEmpty { m.awayTeamId }, m.awayScore)
                            }
                            Spacer(Modifier.height(16.dp))

                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))

                            Text("Arbitro: $refereeName", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Text("Documentazione Allegata", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
                    ReportImageSection("Distinta Casa", m.photoDistintaA)
                    ReportImageSection("Distinta Ospiti", m.photoDistintaB)
                    ReportImageSection("Referto Gara", m.photoReferto)

                    Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                        Text("CHIUDI")
                    }
                }
            }
        }
    }
}

/**
 * Componente per la visualizzazione del punteggio di una squadra.
 */
@Composable
fun ScoreDisplay(name: String, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Text(score.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold)
    }
}

/**
 * Componente per la visualizzazione di una immagine nel report della partita.
 */
@Composable
fun ReportImageSection(label: String, uri: String?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        if (!uri.isNullOrEmpty()) {
            AsyncImage(
                model = uri,
                contentDescription = label,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.small)
                    .border(1.dp, Color.Gray, MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.LightGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Immagine non disponibile", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}