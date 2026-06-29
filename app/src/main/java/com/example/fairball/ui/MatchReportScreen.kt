package com.example.fairball.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchReportScreen(
    matchId: String,
    onClose: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var match by remember { mutableStateOf<Match?>(null) }
    var homeTeam by remember { mutableStateOf<Team?>(null) }
    var awayTeam by remember { mutableStateOf<Team?>(null) }
    var refereeName by remember { mutableStateOf("Caricamento...") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(matchId) {
        if (matchId.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }
        db.collection("matches").document(matchId).get()
            .addOnSuccessListener { doc ->
                val m = doc.toObject(Match::class.java)?.copy(id = doc.id)
                match = m
                if (m != null) {
                    db.collection("teams").document(m.homeTeamId).get().addOnSuccessListener { h ->
                        homeTeam = h.toObject(Team::class.java)
                    }
                    db.collection("teams").document(m.awayTeamId).get().addOnSuccessListener { a ->
                        awayTeam = a.toObject(Team::class.java)
                    }
                    m.refereeId?.let { rid ->
                        db.collection("users").document(rid).get().addOnSuccessListener { u ->
                            refereeName = u.getString("displayName") ?: "Sconosciuto"
                            isLoading = false
                        }
                    } ?: run { 
                        refereeName = "Non assegnato"
                        isLoading = false 
                    }
                } else {
                    isLoading = false
                }
            }
            .addOnFailureListener { isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dettaglio Partita") },
                navigationIcon = {
                    IconButton(onClick = { onClose() }) {
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
        } else if (match != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Risultato Finale", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            ScoreDisplay(homeTeam?.name ?: match?.homeTeamId ?: "Casa", match?.homeScore ?: 0)
                            Text("-", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            ScoreDisplay(awayTeam?.name ?: match?.awayTeamId ?: "Ospiti", match?.awayScore ?: 0)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Arbitro: $refereeName", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Text("Documentazione Allegata", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
                
                ReportImageSection("Distinta Casa", match?.photoDistintaA)
                ReportImageSection("Distinta Ospiti", match?.photoDistintaB)
                ReportImageSection("Referto Gara", match?.photoReferto)

                Button(onClick = { onClose() }, modifier = Modifier.fillMaxWidth()) {
                    Text("CHIUDI")
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Dati partita non trovati.")
            }
        }
    }
}

@Composable
fun ScoreDisplay(name: String, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Text(score.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold)
    }
}

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
        Spacer(modifier = Modifier.height(12.dp))
    }
}
