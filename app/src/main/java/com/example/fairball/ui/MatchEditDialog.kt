package com.example.fairball.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.Venue
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Dialog per la modifica di una partita.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchEditDialog(
    match: Match? = null,
    teams: List<Team>,
    venues: List<Venue>,
    onDismiss: () -> Unit,
    onSave: (Match) -> Unit
) {
    val isEditing = match?.id?.isNotEmpty() == true
    val defaultMatch = Match()

    var category by remember { mutableStateOf(match?.category?.ifEmpty { "Maschile" } ?: "Maschile") }
    var phase by remember { mutableStateOf(match?.phase?.ifEmpty { "Regular Season" } ?: "Regular Season") }
    var selectedVenue by remember { mutableStateOf(venues.find { it.id == match?.venueId }) }
    var showVenuePicker by remember { mutableStateOf(false) }
    var homeTeamId by remember { mutableStateOf(match?.homeTeamId ?: "") }
    var awayTeamId by remember { mutableStateOf(match?.awayTeamId ?: "") }
    var homeScoreStr by remember { mutableStateOf(match?.homeScore?.toString() ?: "0") }
    var awayScoreStr by remember { mutableStateOf(match?.awayScore?.toString() ?: "0") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val initialCalendar = remember {
        Calendar.getInstance().apply {
            match?.scheduledAt?.toDate()?.let { time = it }
        }
    }

    var selectedDateMillis by remember {
        mutableStateOf(
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.YEAR, initialCalendar.get(Calendar.YEAR))
                set(Calendar.MONTH, initialCalendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, initialCalendar.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    var selectedHour by remember { mutableStateOf(initialCalendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(initialCalendar.get(Calendar.MINUTE)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Modifica Partita" else "Nuova Partita") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMessage != null) {
                    item {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                item {
                    Text("Data e Ora:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarMonth, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(formatSelectedDate(selectedDateMillis))
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Schedule, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(String.format(Locale.ITALY, "%02d:%02d", selectedHour, selectedMinute))
                        }
                    }
                }
                item {
                    Text("Impianto:", style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(
                        onClick = { showVenuePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Place, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(selectedVenue?.name ?: "Seleziona su mappa")
                    }
                }
                item {
                    Text("Categoria:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Maschile", "Femminile", "Misto").forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
                item {
                    Text("Fase:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Regular Season", "Semifinale", "Finale").forEach { p ->
                            FilterChip(
                                selected = phase == p,
                                onClick = { phase = p },
                                label = { Text(p) }
                            )
                        }
                    }
                }
                item {
                    Text("Squadra Casa:", style = MaterialTheme.typography.labelMedium)
                    TeamDropdown(teams.filter { it.id != awayTeamId }, homeTeamId) { homeTeamId = it }
                }
                item {
                    Text("Squadra Ospiti:", style = MaterialTheme.typography.labelMedium)
                    TeamDropdown(teams.filter { it.id != homeTeamId }, awayTeamId) { awayTeamId = it }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val errors = mutableListOf<String>()
                if (homeTeamId.isBlank()) errors.add("Squadra casa")
                if (awayTeamId.isBlank()) errors.add("Squadra ospiti")
                if (homeTeamId.isNotBlank() && homeTeamId == awayTeamId) {
                    errors.add("Squadra Casa e Squadra Ospiti devono essere diverse")
                }
                if (selectedVenue == null) errors.add("Impianto")
                if (category.isBlank()) errors.add("Categoria")
                if (phase.isBlank()) errors.add("Fase")
                if (selectedDateMillis == 0L) errors.add("Data")

                if (errors.isNotEmpty()) {
                    errorMessage = "Campi obbligatori mancanti: ${errors.joinToString(", ")}"
                    return@Button
                }

                errorMessage = null

                val utcDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = selectedDateMillis
                }
                val finalCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, utcDate.get(Calendar.YEAR))
                    set(Calendar.MONTH, utcDate.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, utcDate.get(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val newMatch = (match ?: defaultMatch).copy(
                    code = "",
                    category = category,
                    phase = phase,
                    venueId = selectedVenue?.id ?: match?.venueId ?: "",
                    homeTeamId = homeTeamId,
                    awayTeamId = awayTeamId,
                    homeScore = homeScoreStr.toIntOrNull() ?: match?.homeScore ?: 0,
                    awayScore = awayScoreStr.toIntOrNull() ?: match?.awayScore ?: 0,
                    scheduledAt = Timestamp(finalCalendar.time)
                )
                onSave(newMatch)
            }) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Seleziona Ora") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Annulla") }
            }
        )
    }

    if (showVenuePicker) {
        VenuePickerDialog(
            venues = venues,
            onDismiss = { showVenuePicker = false },
            onVenueSelected = { venue ->
                selectedVenue = venue
                showVenuePicker = false
            },
            onVenueCreated = { venue ->
                selectedVenue = venue
                showVenuePicker = false
            }
        )
    }
}

/**
 * Lista per la selezione di una squadra.
 */
@Composable
fun TeamDropdown(teams: List<Team>, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTeam = teams.find { it.id == selectedId }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedTeam?.name ?: "Seleziona Squadra")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            teams.forEach { team ->
                DropdownMenuItem(
                    text = { Text(team.name) },
                    onClick = { onSelect(team.id); expanded = false }
                )
            }
        }
    }
}

/**
 * Formatta la data in formato dd/MM/yyyy.
 */
private fun formatSelectedDate(utcMillis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(java.util.Date(utcMillis))
}