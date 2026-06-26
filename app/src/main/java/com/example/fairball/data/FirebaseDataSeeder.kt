package com.example.fairball.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

object FirebaseDataSeeder {
    fun seedData() {
        val db = FirebaseFirestore.getInstance()
        Log.d("FirebaseSeeder", "Inizio seeding dati...")

        // 1. Teams
        val teams = mapOf(
            "team_vicenza" to mapOf("id" to "team_vicenza", "name" to "Pallavolo Vicenza"),
            "team_verona" to mapOf("id" to "team_verona", "name" to "Volley Verona"),
            "team_padova" to mapOf("id" to "team_padova", "name" to "Padova Volley")
        )

        teams.forEach { (id, data) ->
            db.collection("teams").document(id).set(data)
                .addOnSuccessListener { Log.d("FirebaseSeeder", "Team $id caricato!") }
        }

        // 2. Mario Rossi Referee
        val marioId = "mario_rossi_test_id"
        val marioRossi = mapOf(
            "displayName" to "Mario Rossi",
            "email" to "mario.rossi@example.com",
            "role" to "referee"
        )
        db.collection("users").document(marioId).set(marioRossi)

        // 3. Matches
        val matchMario = mapOf(
            "id" to "match_mario_1",
            "code" to "FINALE-001",
            "season" to "2024",
            "status" to "assegnata",
            "homeTeamId" to "team_vicenza",
            "awayTeamId" to "team_padova",
            "refereeId" to marioId,
            "homeScore" to 0,
            "awayScore" to 0,
            "scheduledAt" to Date(),
            "updatedAt" to Date()
        )

        db.collection("matches").document("match_mario_1").set(matchMario)
            .addOnSuccessListener { Log.d("FirebaseSeeder", "Match Mario caricato!") }

        val match1 = mapOf(
            "id" to "match_1",
            "code" to "F02-2026",
            "season" to "2026",
            "status" to "pending",
            "homeTeamId" to "team_vicenza",
            "awayTeamId" to "team_verona",
            "refereeId" to null,
            "homeScore" to 0,
            "awayScore" to 0,
            "scheduledAt" to Date(),
            "updatedAt" to Date()
        )

        db.collection("matches").document("match_1").set(match1)
    }
}
