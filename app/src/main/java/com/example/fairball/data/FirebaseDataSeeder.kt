package com.example.fairball.data

import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.example.fairball.model.Venue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseDataSeeder {
    fun seedData() {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        val currentUid = auth.currentUser?.uid

        val adminUid = if (currentUid != null && currentUid != "mario_rossi_test_id" && currentUid != "luigi_verdi_test_id") {
            currentUid
        } else {
            "admin_test_id"
        }

        val users = listOf(
            User(
                uid = "mario_rossi_test_id",
                displayName = "Mario Rossi",
                email = "mario.rossi@fairball.com",
                role = "referee",
                photoUrl = ""
            ),
            User(
                uid = "luigi_verdi_test_id",
                displayName = "Luigi Verdi",
                email = "luigi.verdi@fairball.com",
                role = "referee",
                photoUrl = ""
            ),
            User(
                uid = adminUid,
                displayName = "Admin FairBall",
                email = auth.currentUser?.email ?: "admin@fairball.com",
                role = "admin",
                photoUrl = ""
            )
        )

        for (user in users) {
            val docRef = db.collection("users").document(user.uid)
            docRef.get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    docRef.set(user)
                }
            }
        }

        val teams = listOf(
            Team(id = "team_a", name = "F.C. Raptors"),
            Team(id = "team_b", name = "Gengis Warriors"),
            Team(id = "team_c", name = "Real Bologna"),
            Team(id = "team_d", name = "Milan Stars"),
            Team(id = "team_e", name = "Virtus Roma"),
            Team(id = "team_f", name = "Athena Women")
        )

        for (team in teams) {
            db.collection("teams").document(team.id).set(team)
        }

        val venues = listOf(
            Venue(
                id = "venue_milan_1",
                name = "Allianz Cloud (PalaLido)",
                university = "Politecnico di Milano",
                address = "Piazza Carlo Stuparich, 1, 20148 Milano MI",
                latitude = 45.4822,
                longitude = 9.1415
            ),
            Venue(
                id = "venue_rome_1",
                name = "Palazzetto dello Sport",
                university = "Sapienza Università di Roma",
                address = "Piazza Apollodoro, 10, 00196 Roma RM",
                latitude = 41.9291,
                longitude = 12.4719
            ),
            Venue(
                id = "venue_vicenza_1",
                name = "Palasport Città di Vicenza",
                university = "Università degli Studi di Verona - Sede di Vicenza",
                address = "Via Goldoni, 12, 36100 Vicenza VI",
                latitude = 45.5532,
                longitude = 11.5348
            )
        )

        for (venue in venues) {
            db.collection("venues").document(venue.id).set(venue)
        }

        val matches = listOf(
            Match(
                id = "match_finished_1",
                code = "101",
                homeTeamId = "team_a",
                awayTeamId = "team_b",
                phase = "Girone A - Giornata 1",
                status = "finished",
                refereeId = "mario_rossi_test_id",
                coRefereeId = "luigi_verdi_test_id",
                homeScore = 3,
                awayScore = 1,
                venueId = "venue_milan_1"
            ),
            Match(
                id = "match_finished_2",
                code = "102",
                homeTeamId = "team_c",
                awayTeamId = "team_d",
                phase = "Girone A - Giornata 2",
                status = "finished",
                refereeId = "mario_rossi_test_id",
                coRefereeId = "",
                homeScore = 2,
                awayScore = 2,
                venueId = "venue_rome_1"
            ),
            Match(
                id = "match_assigned_1",
                code = "201",
                homeTeamId = "team_b",
                awayTeamId = "team_c",
                phase = "Girone A - Giornata 3",
                status = "assigned",
                refereeId = "mario_rossi_test_id",
                coRefereeId = "",
                venueId = "venue_vicenza_1"
            )
        )

        for (match in matches) {
            db.collection("matches").document(match.id).set(match)
        }
    }
}