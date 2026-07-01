package com.example.fairball.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.fairball.model.Match
import com.example.fairball.model.Venue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit,
    onViewMatchReport: (String) -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()

    var venues by remember { mutableStateOf<List<Venue>>(emptyList()) }
    var teamsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedVenue by remember { mutableStateOf<Venue?>(null) }
    var venueMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var isLoadingMatches by remember { mutableStateOf(false) }

    val userLocation by rememberUserLocation()

    LaunchedEffect(Unit) {
        db.collection("venues").addSnapshotListener { snap, _ ->
            venues = snap?.toObjects(Venue::class.java) ?: emptyList()
        }
        db.collection("teams").get().addOnSuccessListener { snap ->
            teamsMap = snap.documents.associate { it.id to (it.getString("name") ?: it.id) }
        }
    }

    // Ogni volta che si seleziona un impianto, carichiamo le partite finite giocate lì
    LaunchedEffect(selectedVenue) {
        val venue = selectedVenue
        if (venue == null) {
            venueMatches = emptyList()
            return@LaunchedEffect
        }
        isLoadingMatches = true
        db.collection("matches")
            .whereEqualTo("venueId", venue.id)
            .whereEqualTo("status", "finished")
            .get()
            .addOnSuccessListener { snap ->
                venueMatches = snap.documents
                    .mapNotNull { it.toObject(Match::class.java) }
                    .sortedByDescending { it.scheduledAt?.seconds ?: 0L }
                isLoadingMatches = false
            }
            .addOnFailureListener { isLoadingMatches = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mappa Impianti") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        VenueMapView(
            modifier = Modifier.fillMaxSize().padding(padding),
            venues = venues,
            userLocation = userLocation,
            onMarkerClick = { venue -> selectedVenue = venue }
        )
    }

    selectedVenue?.let { venue ->
        VenueDetailSheet(
            venue = venue,
            matches = venueMatches,
            teams = teamsMap,
            isLoading = isLoadingMatches,
            onDismiss = { selectedVenue = null },
            onViewMatch = { matchId ->
                selectedVenue = null
                onViewMatchReport(matchId)
            }
        )
    }
}