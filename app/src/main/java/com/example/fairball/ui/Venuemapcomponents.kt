package com.example.fairball.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fairball.data.FirestoreRepository
import com.example.fairball.model.Match
import com.example.fairball.model.Venue
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

private val DEFAULT_CENTER = GeoPoint(45.4642, 9.1900)

/**
 * Mappa che mostra un marker per ogni impianto e, se disponibile,
 * un marker per la posizione dell'utente (su cui viene centrata la mappa).
 *
 * - onMarkerClick: tap su un impianto esistente
 * - onLongPress: tenuta premuta su un punto libero della mappa (usata per aggiungere
 *   un nuovo impianto); se null, la funzione è disabilitata
 */
@Composable
fun VenueMapView(
    modifier: Modifier = Modifier,
    venues: List<Venue>,
    userLocation: GeoPoint?,
    onMarkerClick: (Venue) -> Unit,
    onLongPress: ((GeoPoint) -> Unit)? = null
) {
    var hasCentered by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osm_pref", Context.MODE_PRIVATE))
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(14.0)
                controller.setCenter(DEFAULT_CENTER)

                if (onLongPress != null) {
                    val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            p?.let { onLongPress(it) }
                            return true
                        }
                    })
                    overlays.add(0, eventsOverlay)
                }
            }
        },
        onRelease = { mapView -> mapView.onDetach() },
        update = { mapView ->
            mapView.overlays.removeAll { it is Marker }

            venues.forEach { venue ->
                val marker = Marker(mapView)
                marker.position = GeoPoint(venue.latitude, venue.longitude)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = venue.name
                marker.snippet = venue.address
                marker.setOnMarkerClickListener { _, _ ->
                    onMarkerClick(venue)
                    true
                }
                mapView.overlays.add(marker)
            }

            if (userLocation != null && !hasCentered) {
                mapView.controller.animateTo(userLocation)
                mapView.controller.setZoom(15.0)
                hasCentered = true

                val userMarker = Marker(mapView)
                userMarker.position = userLocation
                userMarker.title = "La tua posizione"
                mapView.overlays.add(userMarker)
            }

            mapView.invalidate()
        }
    )
}

/**
 * Scheda con i dettagli di un impianto e le ultime partite giocate lì.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueDetailSheet(
    venue: Venue,
    matches: List<Match>,
    teams: Map<String, String>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onViewMatch: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(venue.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (venue.university.isNotEmpty()) {
                Text(venue.university, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            if (venue.address.isNotEmpty()) {
                Text(venue.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text("Ultime partite giocate qui", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            when {
                isLoading -> {
                    LoadingBox(modifier = Modifier.fillMaxWidth().padding(24.dp))
                }
                matches.isEmpty() -> {
                    Text("Nessuna partita disputata qui finora.", color = Color.Gray)
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        matches.take(15).forEach { match ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onViewMatch(match.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${teams.nameOf(match.homeTeamId)} vs ${teams.nameOf(match.awayTeamId)}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        match.scheduledAt?.let {
                                            Text(it.toFormattedDate(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                    }
                                    Text(
                                        "${match.homeScore} - ${match.awayScore}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Dialog a tutto schermo per selezionare un impianto esistente oppure crearne uno nuovo.
 * Usato dal form di modifica/creazione partita.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenuePickerDialog(
    venues: List<Venue>,
    onDismiss: () -> Unit,
    onVenueSelected: (Venue) -> Unit,
    onVenueCreated: (Venue) -> Unit
) {
    val userLocation by rememberUserLocation()
    var pendingNewVenuePoint by remember { mutableStateOf<GeoPoint?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Seleziona Impianto") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Chiudi")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                VenueMapView(
                    modifier = Modifier.fillMaxSize(),
                    venues = venues,
                    userLocation = userLocation,
                    onMarkerClick = { venue -> onVenueSelected(venue) },
                    onLongPress = { point -> pendingNewVenuePoint = point }
                )
                Text(
                    "Tocca un impianto per selezionarlo, oppure tieni premuto sulla mappa per aggiungerne uno nuovo",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    pendingNewVenuePoint?.let { point ->
        NewVenueDialog(
            point = point,
            onDismiss = { pendingNewVenuePoint = null },
            onSave = { name, university, address ->
                val venue = FirestoreRepository.createVenue(name, university, address, point.latitude, point.longitude)
                onVenueCreated(venue)
                pendingNewVenuePoint = null
            }
        )
    }
}

/**
 * Dialog per la creazione di un nuovo impianto.
 */
@Composable
private fun NewVenueDialog(
    point: GeoPoint,
    onDismiss: () -> Unit,
    onSave: (name: String, university: String, address: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var university by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Place, contentDescription = null) },
        title = { Text("Nuovo Impianto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Posizione: ${"%.5f".format(point.latitude)}, ${"%.5f".format(point.longitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome impianto") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = university,
                    onValueChange = { university = it },
                    label = { Text("Università / Campus") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Indirizzo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, university, address) },
                enabled = name.isNotBlank()
            ) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}