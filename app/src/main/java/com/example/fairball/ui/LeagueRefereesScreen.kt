package com.example.fairball.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fairball.data.FirestoreRepository
import com.example.fairball.model.Match
import com.example.fairball.model.User

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

    LaunchedEffect(allReferees, allMatches) {
        if (allReferees != null && allMatches != null) {
            refereeStats = allReferees!!.map { referee ->
                calculateRefereeStats(referee, allMatches!!)
            }.sortedByDescending { it.matchCount }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Classifica Arbitri") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("Hall of Fame", style = MaterialTheme.typography.headlineSmall) }
                items(refereeStats) { stat ->
                    RefereeStatCard(stat, onClick = { onRefereeClick(stat.user.uid) })
                }
            }
        }
    }
}

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