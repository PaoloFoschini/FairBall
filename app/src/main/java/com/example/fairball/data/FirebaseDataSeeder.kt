package com.example.fairball.data

import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseDataSeeder {
    fun seedData() {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        // Recuperiamo l'UID dell'utente attualmente loggato in Firebase (se presente)
        val currentUid = auth.currentUser?.uid

        // 1. SEED UTENTI
        // Se l'utente corrente è l'admin loggato con Google, usiamo il suo UID reale,
        // altrimenti usiamo l'ID di fallback "admin_test_id"
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
            db.collection("users").document(user.uid).set(user)
        }

        // 2. SEED SQUADRE
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

        // 3. SEED PARTITE (Senza il parametro timestamp per non generare errori nel modello Match)
        val matches = listOf(
            // Partite FINISHED (Approvate -> Caricano lo storico ed i badge di Mario Rossi)
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
                awayScore = 1
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
                awayScore = 2
            ),
            Match(
                id = "match_finished_3",
                code = "103",
                homeTeamId = "team_e",
                awayTeamId = "team_f",
                phase = "Girone B - Giornata 1",
                status = "finished",
                refereeId = "mario_rossi_test_id",
                coRefereeId = "luigi_verdi_test_id",
                homeScore = 0,
                awayScore = 4
            ),
            // Partite ASSIGNED (Visualizzate nella Home dell'arbitro)
            Match(
                id = "match_assigned_1",
                code = "201",
                homeTeamId = "team_b",
                awayTeamId = "team_c",
                phase = "Girone A - Giornata 3",
                status = "assigned",
                refereeId = "mario_rossi_test_id",
                coRefereeId = ""
            ),
            // Partite UNASSIGNED (Disponibili per l'Admin nella sezione da approvare)
            Match(
                id = "match_unassigned_1",
                code = "301",
                homeTeamId = "team_d",
                awayTeamId = "team_a",
                phase = "Girone A - Giornata 4",
                status = "unassigned"
            )
        )

        for (match in matches) {
            db.collection("matches").document(match.id).set(match)
        }
    }
}