package com.example.fairball.ui

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.fairball.data.FirestoreRepository
import com.example.fairball.model.Match
import com.example.fairball.model.Venue

private fun isGpsEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

/**
 * Schermata di visualizzazione della mappa con i vari impianti
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit,
    onViewMatchReport: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isGpsActive by remember { mutableStateOf(isGpsEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGpsActive = isGpsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
        topBar = { BackTopBar(title = "Mappa Impianti", onBack = onBack) }
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

    if (!isGpsActive) {
        AlertDialog(
            onDismissRequest = { /* non chiudibile toccando fuori: serve una scelta esplicita */ },
            title = { Text("GPS disattivato") },
            text = { Text("Non puoi usare questo servizio se non attivi il GPS. Attivalo per continuare a usare la mappa.") },
            confirmButton = {
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) {
                    Text("Attiva GPS")
                }
            },
            dismissButton = {
                TextButton(onClick = onBack) {
                    Text("Indietro")
                }
            }
        )
    } else {
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
}