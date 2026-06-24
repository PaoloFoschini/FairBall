package com.example.fairball.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fairball.model.Match
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    role: String,
    onViewReferees: () -> Unit,
    onViewProfile: () -> Unit,
    onViewChampionship: () -> Unit,
    onViewMap: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    
    var assignedMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            db.collection("matches")
                .whereEqualTo("refereeId", currentUser.uid)
                .get()
                .addOnSuccessListener { result ->
                    assignedMatches = result.toObjects(Match::class.java)
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FairBall Home") },
                navigationIcon = {
                    IconButton(onClick = onViewReferees) {
                        Icon(Icons.Default.Sports, contentDescription = "Arbitri Lega")
                    }
                },
                actions = {
                    IconButton(onClick = onViewProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profilo")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Benvenuto, $role",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Le tue partite assegnate:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (assignedMatches.isEmpty()) {
                Text("Nessuna partita assegnata.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(assignedMatches) { match ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Gara: ${match.code}", style = MaterialTheme.typography.bodyLarge)
                                Text(text = "Stato: ${match.status}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "${match.homeTeamId} vs ${match.awayTeamId}")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onViewChampionship,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Vai al Campionato")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onViewMap,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Visualizza Mappa Campi")
            }
        }
    }
}
