package com.example.fairball.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fairball.data.FirestoreRepository

/**
 * Statistiche di filtro di un arbitro.
 */
private enum class RefereeSortOrder(val label: String) {
    MATCHES("Più partite"),
    NAME("Nome A-Z")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueRefereesScreen(
    onBack: () -> Unit,
    onRefereeClick: (String) -> Unit
) {
    val allReferees by FirestoreRepository.refereesFlow().collectAsState(initial = null)
    val allMatches by FirestoreRepository.matchesFlow().collectAsState(initial = null)
    var isLoading by remember { mutableStateOf(true) }
    var refereeStats by remember { mutableStateOf<List<RefereeStat>>(emptyList()) }

    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(RefereeSortOrder.MATCHES) }
    var onlyWithBadges by remember { mutableStateOf(false) }

    LaunchedEffect(allReferees, allMatches) {
        if (allReferees != null && allMatches != null) {
            refereeStats = allReferees!!.map { referee ->
                calculateRefereeStats(referee, allMatches!!)
            }.sortedByDescending { it.matchCount }
            isLoading = false
        }
    }

    val filteredStats = refereeStats
        .filter { searchQuery.isBlank() || it.user.displayName.contains(searchQuery, ignoreCase = true) }
        .filter { !onlyWithBadges || it.badges.any { badge -> badge.isUnlocked } }
        .let { list ->
            when (sortOrder) {
                RefereeSortOrder.MATCHES -> list.sortedByDescending { it.matchCount }
                RefereeSortOrder.NAME -> list.sortedBy { it.user.displayName.lowercase() }
            }
        }

    Scaffold(
        topBar = { BackTopBar(title = "Classifica Arbitri", onBack = onBack) }
    ) { padding ->
        if (isLoading) {
            LoadingBox(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                SearchField(searchQuery, { searchQuery = it }, "Cerca arbitro per nome")
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RefereeSortOrder.values().forEach { order ->
                        CompactFilterChip(order.label, sortOrder == order) { sortOrder = order }
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(18.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    CompactFilterChip("Solo con badge", onlyWithBadges) { onlyWithBadges = !onlyWithBadges }
                }
                Spacer(Modifier.height(8.dp))

                if (filteredStats.isEmpty()) {
                    EmptyStateBox("Nessun arbitro trovato.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredStats, key = { it.user.uid }) { stat ->
                            RefereeStatCard(stat, onClick = { onRefereeClick(stat.user.uid) })
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

/**
 * Statistiche di un arbitro, compresi badge e partite.
 */
@Composable
fun RefereeStatCard(stat: RefereeStat, onClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val unlockedBadges = stat.badges.filter { it.isUnlocked }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stat.user.displayName.take(1).uppercase(), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stat.user.displayName, style = MaterialTheme.typography.titleMedium)
                    Text("${stat.matchCount} partite arbitrate", style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    unlockedBadges.take(3).forEach { badge ->
                        Icon(badge.icon, null, tint = badge.color, modifier = Modifier.size(20.dp).padding(horizontal = 2.dp))
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    }
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider()
                    stat.badges.forEach { BadgeItem(it) }
                }
            }
        }
    }
}