package com.example.fairball.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.fairball.data.FirestoreRepository
import com.example.fairball.model.Match
import com.example.fairball.model.Venue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit,
    onViewMatchReport: (String) -> Unit = {}
) {
    val venues by FirestoreRepository.venuesFlow().collectAsState(initial = null)
    var teamsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(Unit) {
        teamsMap = FirestoreRepository.fetchTeamNameMap()
    }

    var selectedVenue by remember { mutableStateOf<Venue?>(null) }
    var venueMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var isLoadingMatches by remember { mutableStateOf(false) }

    val userLocation by rememberUserLocation()

    LaunchedEffect(selectedVenue) {
        val venue = selectedVenue ?: return@LaunchedEffect
        isLoadingMatches = true
        venueMatches = FirestoreRepository.fetchFinishedMatchesAtVenue(venue.id)
        isLoadingMatches = false
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
        if (venues == null) {

            Surface(modifier = Modifier.fillMaxSize().padding(padding)){}
        } else {
            VenueMapView(
                modifier = Modifier.fillMaxSize().padding(padding),
                venues = venues!!,
                userLocation = userLocation,
                onMarkerClick = { venue -> selectedVenue = venue }
            )
        }
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