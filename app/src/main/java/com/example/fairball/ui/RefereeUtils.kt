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

data class Badge(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val isUnlocked: Boolean = false,
    val progress: Int = 0,
    val target: Int = 1
)

data class RefereeStat(
    val user: User,
    val matchCount: Int,
    val badges: List<Badge>
)

fun calculateRefereeStats(user: User, allMatches: List<Match>): RefereeStat {
    val myFinishedMatches = allMatches.filter { (it.refereeId == user.uid || it.coRefereeId == user.uid) && it.status == "finished" }
    val totalMatches = myFinishedMatches.size
    val badges = mutableListOf<Badge>()

    val lastMinuteCount = myFinishedMatches.count { m ->
        val sched = m.scheduledAt?.toDate()?.time ?: 0L
        val ass = m.assignedAt?.toDate()?.time ?: 0L
        val diffHours = if (ass > 0) (sched - ass) / (1000 * 60 * 60) else 100L
        ass > 0 && diffHours in 0..24
    }
    badges.add(Badge("Salvatore della Patria", "Gara accettata con preavviso < 24h", Icons.Default.Shield, Color(0xFFE91E63), lastMinuteCount > 0))

    val uniqueVenues = myFinishedMatches.map { it.venueId }.filter { it.isNotEmpty() }.distinct().size
    val venueColor = when {
        uniqueVenues >= 10 -> Color(0xFFFFD700)
        uniqueVenues >= 5 -> Color(0xFFC0C0C0)
        else -> Color(0xFFCD7F32)
    }
    badges.add(Badge("Pioniere dei Campi", "Arbitra in 3, 5 o 10 impianti diversi", Icons.Default.Stadium, venueColor, uniqueVenues >= 3, uniqueVenues, 3))

    val calendar = Calendar.getInstance()
    val maxWeekly = myFinishedMatches.groupBy { m ->
        val date = m.scheduledAt?.toDate() ?: Date()
        calendar.time = date
        "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.WEEK_OF_YEAR)}"
    }.values.maxOfOrNull { it.size } ?: 0
    badges.add(Badge("Stakanovista", "Almeno 3 partite in una settimana solare", Icons.Default.Bolt, Color(0xFFFF9800), maxWeekly >= 3, maxWeekly, 3))

    val partners = myFinishedMatches.mapNotNull { if (it.refereeId == user.uid) it.coRefereeId else it.refereeId }
    val maxTogether = if (partners.isEmpty()) 0 else partners.groupingBy { it }.eachCount().values.maxOf { it }
    badges.add(Badge("Coppia Fissa", "Arbitra 3+ volte con lo stesso collega", Icons.Default.People, Color(0xFF2196F3), maxTogether >= 3, maxTogether, 3))

    val hasFinalStage = myFinishedMatches.any { it.phase == "Semifinale" || it.phase == "Finale" }
    badges.add(Badge("Il Grande Palco", "Hai diretto una Semifinale o una Finale", Icons.Default.EmojiEvents, Color(0xFFFFD700), hasFinalStage))

    val cats = myFinishedMatches.map { it.category }.distinct()
    val isEclettico = cats.contains("Maschile") && cats.contains("Femminile") && cats.contains("Misto")
    badges.add(Badge("Arbitro Eclettico", "Dirigi match Maschili, Femminili e Misti", Icons.Default.Category, Color(0xFF9C27B0), isEclettico))

    return RefereeStat(user, totalMatches, badges)
}

@Composable
fun BadgeItem(badge: Badge) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(badge.icon, null, tint = if (badge.isUnlocked) badge.color else Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(badge.name, style = MaterialTheme.typography.bodyMedium, color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray)
            Text(badge.description, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            if (!badge.isUnlocked && badge.target > 1 && badge.progress > 0) {
                LinearProgressIndicator(
                    progress = (badge.progress.toFloat() / badge.target.toFloat()).coerceAtMost(1f),
                    modifier = Modifier.width(100.dp).height(4.dp).padding(top = 4.dp),
                    color = badge.color.copy(alpha = 0.5f)
                )
            }
        }
        if (badge.isUnlocked) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
        }
    }
}