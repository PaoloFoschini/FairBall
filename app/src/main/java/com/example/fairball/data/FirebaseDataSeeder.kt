package com.example.fairball.data

import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

object FirebaseDataSeeder {
    fun seedData() {
        val db = FirebaseFirestore.getInstance()
        
        // 1. Teams
        val teams = listOf(
            Team("team_vicenza", "Pallavolo Vicenza"),
            Team("team_verona", "Volley Verona"),
            Team("team_padova", "Padova Volley")
        )
        teams.forEach { db.collection("teams").document(it.id).set(it) }

        // 2. Referees
        val referees = listOf(
            User("mario_rossi_test_id", "Mario Rossi", "mario@fairball.com", "referee"),
            User("luca_bianchi_id", "Luca Bianchi", "luca@fairball.com", "referee"),
            User("paolo_verdi_id", "Paolo Verdi", "paolo@fairball.com", "referee"),
            User("admin_test_id", "Admin Pro", "admin@fairball.com", "admin")
        )
        referees.forEach { db.collection("users").document(it.uid).set(it) }

        // 3. Matches
        val now = Date()
        val historicalMatches = listOf(
            Match(
                id = "match_mario_1",
                code = "FINALE-001",
                status = "assigned",
                homeTeamId = "team_vicenza",
                awayTeamId = "team_padova",
                refereeId = "mario_rossi_test_id",
                scheduledAt = Timestamp(now)
            ),
            Match(
                id = "h1",
                code = "EXT-01",
                status = "finished",
                homeTeamId = "team_verona",
                awayTeamId = "team_padova",
                refereeId = "luca_bianchi_id",
                homeScore = 3,
                awayScore = 1,
                scheduledAt = Timestamp(Date(now.time - 86400000))
            ),
            Match(
                id = "h2",
                code = "EXT-02",
                status = "pending",
                homeTeamId = "team_verona",
                awayTeamId = "team_padova",
                scheduledAt = Timestamp(Date(now.time + 86400000))
            )
        )

        historicalMatches.forEach { db.collection("matches").document(it.id).set(it) }
    }
}
