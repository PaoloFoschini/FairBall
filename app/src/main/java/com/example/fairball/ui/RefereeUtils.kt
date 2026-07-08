package com.example.fairball.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.fairball.model.Match
import com.example.fairball.model.User
import java.util.*

/**
 * Struttura di definizione di un badge
 */
data class Badge(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val isUnlocked: Boolean = false,
    val progress: Int = 0,
    val target: Int = 1
)

/**
 * Statistiche di un arbitro (partite arbitrate e badge)
 */
data class RefereeStat(
    val user: User,
    val matchCount: Int,
    val badges: List<Badge>
)

/**
 * Funzione di calcolo di raggiungimento di badge
 */
fun calculateRefereeStats(user: User, allMatches: List<Match>): RefereeStat {
    val myFinishedMatches = allMatches.filter {
        (it.refereeId == user.uid || it.coRefereeId == user.uid) && it.status == "finished"
    }
    val totalMatches = myFinishedMatches.size
    val badges = mutableListOf<Badge>()

    // --- Badge 1: Salvatore della Patria ---
    val lastMinuteCount = myFinishedMatches.count { m ->
        val sched = m.scheduledAt?.toDate()?.time ?: 0L
        val ass = m.assignedAt?.toDate()?.time ?: 0L
        val diffHours = if (ass > 0) (sched - ass) / (1000 * 60 * 60) else 100L
        ass > 0 && diffHours in 0..24
    }
    badges.add(Badge(
        name = "Salvatore della Patria",
        description = "Gara accettata con preavviso < 24h",
        icon = Icons.Default.Shield,
        color = Color(0xFFE91E63),
        isUnlocked = lastMinuteCount >= 1,
        progress = lastMinuteCount,
        target = 1
    ))

    // --- Badge 2: Pioniere dei Campi ---
    val uniqueVenues = myFinishedMatches.map { it.venueId }.filter { it.isNotEmpty() }.distinct().size
    val venueColor = when {
        uniqueVenues >= 10 -> Color(0xFFFFD700)
        uniqueVenues >= 5 -> Color(0xFFC0C0C0)
        else -> Color(0xFFCD7F32)
    }
    badges.add(Badge(
        name = "Pioniere dei Campi",
        description = "Arbitra in almeno 3 impianti diversi",
        icon = Icons.Default.Stadium,
        color = venueColor,
        isUnlocked = uniqueVenues >= 3,
        progress = uniqueVenues,
        target = 3
    ))

    // --- Badge 3: Stakanovista ---
    val calendar = Calendar.getInstance()
    val maxWeekly = myFinishedMatches.groupBy { m ->
        val date = m.scheduledAt?.toDate() ?: Date()
        calendar.time = date
        "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.WEEK_OF_YEAR)}"
    }.values.maxOfOrNull { it.size } ?: 0
    badges.add(Badge(
        name = "Stakanovista",
        description = "Almeno 3 partite in una settimana solare",
        icon = Icons.Default.Bolt,
        color = Color(0xFFFF9800),
        isUnlocked = maxWeekly >= 3,
        progress = maxWeekly,
        target = 3
    ))

    // --- Badge 4: Coppia Fissa (Matematicamente allineato!) ---
    val partners = myFinishedMatches.mapNotNull { if (it.refereeId == user.uid) it.coRefereeId else it.refereeId }.filter { it.isNotEmpty() }
    val maxTogether = if (partners.isEmpty()) 0 else partners.groupingBy { it }.eachCount().values.maxOf { it }
    badges.add(Badge(
        name = "Coppia Fissa",
        description = "Arbitra 3+ volte con lo stesso collega",
        icon = Icons.Default.People,
        color = Color(0xFF2196F3),
        isUnlocked = maxTogether >= 3,
        progress = maxTogether,
        target = 3
    ))

    // --- Badge 5: Il Grande Palco ---
    val hasFinalStage = myFinishedMatches.any { it.phase == "Semifinale" || it.phase == "Finale" }
    badges.add(Badge(
        name = "Il Grande Palco",
        description = "Hai diretto una Semifinale o una Finale",
        icon = Icons.Default.EmojiEvents,
        color = Color(0xFFFFD700),
        isUnlocked = hasFinalStage,
        progress = if (hasFinalStage) 1 else 0,
        target = 1
    ))

    // --- Badge 6: Arbitro Eclettico ---
    val cats = myFinishedMatches.map { it.category }.distinct()
    val isEclettico = cats.contains("Maschile") && cats.contains("Femminile") && cats.contains("Misto")
    badges.add(Badge(
        name = "Arbitro Eclettico",
        description = "Dirigi match Maschili, Femminili e Misti",
        icon = Icons.Default.Category,
        color = Color(0xFF9C27B0),
        isUnlocked = isEclettico,
        progress = cats.size,
        target = 3
    ))

    return RefereeStat(user, totalMatches, badges)
}

/**
 * Componente per la visualizzazione di un badge.
 */
@Composable
fun BadgeItem(badge: Badge) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = badge.icon,
            contentDescription = null,
            tint = if (badge.isUnlocked) badge.color else Color.Gray.copy(alpha = 0.3f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = badge.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
            )
            Text(
                text = badge.description,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            if (!badge.isUnlocked && badge.target > 1 && badge.progress > 0) {
                LinearProgressIndicator(
                    progress = { (badge.progress.toFloat() / badge.target.toFloat()).coerceAtMost(1f) },
                    modifier = Modifier.width(100.dp).height(4.dp).padding(top = 4.dp),
                    color = badge.color.copy(alpha = 0.5f)
                )
            }
        }
        if (badge.isUnlocked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}