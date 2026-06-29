package com.example.fairball.data

import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

object FirebaseDataSeeder {
    private var seeded = false

    fun seedData() {
        if (seeded) return
        seeded = true

        val db = FirebaseFirestore.getInstance()

        // 1. Teams
        val teams = listOf(
            Team("team_vicenza", "Pallavolo Vicenza"),
            Team("team_verona", "Volley Verona"),
            Team("team_padova", "Padova Volley"),
            Team("team_treviso", "Treviso Volley"),
            Team("team_venezia", "Venezia Volley")
        )
        teams.forEach { db.collection("teams").document(it.id).set(it) }

        // 2. Users
        val users = listOf(
            User("mario_rossi_test_id", "Mario Rossi", "mario@fairball.com", "referee"),
            User("luca_bianchi_id", "Luca Bianchi", "luca@fairball.com", "referee"),
            User("paolo_verdi_id", "Paolo Verdi", "paolo@fairball.com", "referee"),
            User("sara_neri_id", "Sara Neri", "sara@fairball.com", "referee"),
            User("admin_test_id", "Admin Pro", "admin@fairball.com", "admin")
        )
        users.forEach { db.collection("users").document(it.uid).set(it) }

        val now = Date()
        fun daysFromNow(days: Int) = Timestamp(Date(now.time + days * 86400000L))

        // 3. Matches
        val matches = listOf(
            // Future - assegnate
            Match(
                id = "match_f1",
                code = "RS-001",
                status = "assigned",
                homeTeamId = "team_vicenza",
                awayTeamId = "team_verona",
                refereeId = "mario_rossi_test_id",
                phase = "Regular Season",
                category = "Maschile",
                scheduledAt = daysFromNow(2)
            ),
            Match(
                id = "match_f2",
                code = "RS-002",
                status = "assigned",
                homeTeamId = "team_padova",
                awayTeamId = "team_treviso",
                refereeId = "luca_bianchi_id",
                phase = "Regular Season",
                category = "Femminile",
                scheduledAt = daysFromNow(4)
            ),
            // Future - non assegnate
            Match(
                id = "match_f3",
                code = "RS-003",
                status = "pending",
                homeTeamId = "team_venezia",
                awayTeamId = "team_vicenza",
                phase = "Regular Season",
                category = "Misto",
                scheduledAt = daysFromNow(6)
            ),
            Match(
                id = "match_f4",
                code = "RS-004",
                status = "pending",
                homeTeamId = "team_verona",
                awayTeamId = "team_venezia",
                phase = "Regular Season",
                category = "Maschile",
                scheduledAt = daysFromNow(10)
            ),
            Match(
                id = "match_f5",
                code = "SF-001",
                status = "pending",
                homeTeamId = "team_treviso",
                awayTeamId = "team_padova",
                phase = "Semifinale",
                category = "Femminile",
                scheduledAt = daysFromNow(15)
            ),
            // Passate - finite
            Match(
                id = "match_p1",
                code = "RS-000",
                status = "finished",
                homeTeamId = "team_vicenza",
                awayTeamId = "team_treviso",
                refereeId = "mario_rossi_test_id",
                homeScore = 3,
                awayScore = 1,
                phase = "Regular Season",
                category = "Maschile",
                scheduledAt = daysFromNow(-7)
            ),
            Match(
                id = "match_p2",
                code = "RS-00B",
                status = "finished",
                homeTeamId = "team_verona",
                awayTeamId = "team_padova",
                refereeId = "luca_bianchi_id",
                homeScore = 0,
                awayScore = 3,
                phase = "Regular Season",
                category = "Femminile",
                scheduledAt = daysFromNow(-5)
            ),
            Match(
                id = "match_p3",
                code = "RS-00C",
                status = "finished",
                homeTeamId = "team_venezia",
                awayTeamId = "team_verona",
                refereeId = "paolo_verdi_id",
                homeScore = 2,
                awayScore = 3,
                phase = "Regular Season",
                category = "Misto",
                scheduledAt = daysFromNow(-3)
            ),
            Match(
                id = "match_p4",
                code = "RS-00D",
                status = "finished",
                homeTeamId = "team_treviso",
                awayTeamId = "team_venezia",
                refereeId = "sara_neri_id",
                homeScore = 3,
                awayScore = 2,
                phase = "Regular Season",
                category = "Maschile",
                scheduledAt = daysFromNow(-1)
            )
        )
        matches.forEach { db.collection("matches").document(it.id).set(it) }
    }
}