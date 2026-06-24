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
            "team_vicenza" to mapOf("name" to "Pallavolo Vicenza"),
            "team_verona" to mapOf("name" to "Volley Verona"),
            "team_padova" to mapOf("name" to "Padova Volley")
        )

        teams.forEach { (id, data) ->
            db.collection("teams").document(id).set(data)
                .addOnSuccessListener { Log.d("FirebaseSeeder", "Team $id caricato!") }
                .addOnFailureListener { e -> Log.e("FirebaseSeeder", "Errore team $id", e) }
        }

        // 2. Matches
        val match1 = mapOf(
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

        val match2 = mapOf(
            "code" to "F02-2026",
            "season" to "2026",
            "status" to "pending",
            "homeTeamId" to "team_verona",
            "awayTeamId" to "team_padova",
            "refereeId" to null,
            "homeScore" to 0,
            "awayScore" to 0,
            "scheduledAt" to Date(),
            "updatedAt" to Date()
        )

        db.collection("matches").document("match_1").set(match1)
            .addOnSuccessListener { Log.d("FirebaseSeeder", "Match 1 caricato!") }
        db.collection("matches").document("match_2").set(match2)
            .addOnSuccessListener { Log.d("FirebaseSeeder", "Match 2 caricato!") }
    }
}
