package com.example.fairball.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.util.GeoPoint

/**
 * Richiede i permessi di posizione e restituisce la posizione attuale
 * dell'utente. Gestisce sia ACCESS_FINE_LOCATION che ACCESS_COARSE_LOCATION.
 *
 * Non si limita a leggere `lastLocation` (che è solo una cache, spesso
 * vuota o vecchia se nessuna app ha richiesto di recente la posizione) ma
 * forza attivamente il dispositivo a calcolare un fix aggiornato tramite
 * `getCurrentLocation`.
 *
 * Il tentativo di localizzazione viene ripetuto anche ogni volta che l'app
 * torna in primo piano (ON_RESUME), non solo alla primissima apertura:
 * questo copre il caso in cui l'utente attiva il GPS dalle impostazioni di
 * sistema mentre la mappa è già aperta e poi torna indietro, oppure
 * concede il permesso dalle impostazioni dell'app.
 */
@Composable
fun rememberUserLocation(): State<GeoPoint?> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val location = remember { mutableStateOf<GeoPoint?>(null) }
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var cancellationTokenSource by remember { mutableStateOf(CancellationTokenSource()) }

    fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return hasFine || hasCoarse
    }

    val fetchLocation = {
        if (hasLocationPermission()) {
            if (cancellationTokenSource.token.isCancellationRequested) {
                cancellationTokenSource = CancellationTokenSource()
            }
            try {
                fusedClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null && location.value == null) {
                        location.value = GeoPoint(loc.latitude, loc.longitude)
                    }
                }
            } catch (e: SecurityException) {
            }
            try {
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { loc ->
                    if (loc != null) {
                        location.value = GeoPoint(loc.latitude, loc.longitude)
                    }
                }
            } catch (e: SecurityException) {
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) fetchLocation()
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission()) {
            fetchLocation()
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                fetchLocation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            cancellationTokenSource.cancel()
        }
    }

    return location
}