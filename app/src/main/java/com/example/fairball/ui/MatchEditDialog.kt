package com.example.fairball.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.Venue
import com.google.firebase.Timestamp

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

    var code by remember { mutableStateOf(match?.code ?: "") }
    var category by remember { mutableStateOf(match?.category?.ifEmpty { "Maschile" } ?: "Maschile") }
    var phase by remember { mutableStateOf(match?.phase?.ifEmpty { "Regular Season" } ?: "Regular Season") }
    var selectedVenue by remember { mutableStateOf(venues.find { it.id == match?.venueId }) }
    var showVenuePicker by remember { mutableStateOf(false) }
    var homeTeamId by remember { mutableStateOf(match?.homeTeamId ?: "") }
    var awayTeamId by remember { mutableStateOf(match?.awayTeamId ?: "") }
    var homeScoreStr by remember { mutableStateOf(match?.homeScore?.toString() ?: "0") }
    var awayScoreStr by remember { mutableStateOf(match?.awayScore?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Modifica Partita" else "Nuova Partita") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Codice Gara") },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    TeamDropdown(teams, homeTeamId) { homeTeamId = it }
                }
                item {
                    Text("Squadra Ospiti:", style = MaterialTheme.typography.labelMedium)
                    TeamDropdown(teams, awayTeamId) { awayTeamId = it }
                }
                if (isEditing) {
                    item {
                        Text("Punteggio (correzione):", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = homeScoreStr,
                                onValueChange = { homeScoreStr = it.filter { c -> c.isDigit() } },
                                label = { Text("Casa") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Text("–", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = awayScoreStr,
                                onValueChange = { awayScoreStr = it.filter { c -> c.isDigit() } },
                                label = { Text("Ospiti") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newMatch = (match ?: defaultMatch).copy(
                    code = code,
                    category = category,
                    phase = phase,
                    venueId = selectedVenue?.id ?: match?.venueId ?: "",
                    homeTeamId = homeTeamId,
                    awayTeamId = awayTeamId,
                    homeScore = homeScoreStr.toIntOrNull() ?: match?.homeScore ?: 0,
                    awayScore = awayScoreStr.toIntOrNull() ?: match?.awayScore ?: 0,
                    scheduledAt = match?.scheduledAt ?: Timestamp.now()
                )
                onSave(newMatch)
            }) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )

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