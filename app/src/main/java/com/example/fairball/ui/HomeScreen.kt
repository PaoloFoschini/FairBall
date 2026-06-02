package com.example.fairball.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    role: String,
    onViewReferees: () -> Unit,
    onViewProfile: () -> Unit,
    onViewChampionship: () -> Unit
) {
    // Mock data per le partite assegnate
    val assignedMatches = listOf("Partita 1: Team A vs Team B", "Partita 2: Team C vs Team D")

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

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(assignedMatches) { match ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(text = match, modifier = Modifier.padding(16.dp))
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
        }
    }
}
