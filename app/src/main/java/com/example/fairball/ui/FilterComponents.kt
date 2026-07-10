package com.example.fairball.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Mappa degli stati delle gare.
 */
val statusLabels = mapOf(
    "pending" to "In Attesa",
    "assigned" to "Assegnata",
    "waiting_approval" to "Da Verificare",
    "finished" to "Conclusa",
    "rejected" to "Rifiutata"
)

/**
 * Bottone per filtri.
 */
@Composable
fun CompactFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = Modifier.height(30.dp)
    )
}

/**
 * Etichetta di filtro.
 */
@Composable
fun FilterGroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(end = 2.dp)
    )
}

/**
 * Lista di filtri per tipologia di gara.
 */
@Composable
fun CategoryFilterRow(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterGroupLabel("Categoria:")
        CompactFilterChip("Tutte", selected == null) { onSelect(null) }
        categories.forEach { category ->
            CompactFilterChip(category, selected == category) {
                onSelect(if (selected == category) null else category)
            }
        }
    }
}

/**
 * Lista di filtri per stato della partita.
 */
@Composable
fun StatusFilterRow(
    statuses: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterGroupLabel("Stato:")
        CompactFilterChip("Tutti", selected == null) { onSelect(null) }
        statuses.forEach { status ->
            CompactFilterChip(statusLabels[status] ?: status.replaceFirstChar { it.uppercase() }, selected == status) {
                onSelect(if (selected == status) null else status)
            }
        }
    }
}

/**
 * Filtro di ricerca.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                }
            }
        },
        singleLine = true
    )
}
