package com.example.fairball.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

object FirebaseDataSeeder {

    private val db = FirebaseFirestore.getInstance()

    fun seedData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Evitiamo duplicati se il database è già popolato
                val venuesSnapshot = db.collection("venues").get().await()
                if (!venuesSnapshot.isEmpty) {
                    return@launch
                }

                // 1. POPOLAMENTO UTENTI/ARBITRI (Senza campo badges, come d'accordo)
                val users = mapOf(
                    // TODO: Sostituisci "IL_TUO_VERO_UID_DI_FIREBASE" con il tuo UID reale
                    "IL_TUO_VERO_UID_DI_FIREBASE" to mapOf(
                        "displayName" to "Mio Profilo Arbitro",
                        "email" to "tuaminamail@test.com",
                        "role" to "referee",
                        "photoUrl" to null
                    ),
                    "ref_01" to mapOf("displayName" to "Alessandro Riva", "email" to "alessandro.riva@fairball.com", "role" to "referee", "photoUrl" to null),
                    "ref_02" to mapOf("displayName" to "Marco Visentin", "email" to "marco.vise@fairball.com", "role" to "referee", "photoUrl" to null),
                    "ref_03" to mapOf("displayName" to "Luca Gatti", "email" to "luca.gatti@fairball.com", "role" to "referee", "photoUrl" to null),
                    "ref_04" to mapOf("displayName" to "Davide Longo", "email" to "davide.longo@fairball.com", "role" to "referee", "photoUrl" to null)
                )

                for ((id, data) in users) {
                    db.collection("users").document(id).set(data).await()
                }

                // 2. CREAZIONE PALESTRE (Almeno 4 diverse per sbloccare i badge di movimento)
                val venues = mapOf(
                    "venue_01" to mapOf("name" to "PalaRuffini - Torino", "address" to "Viale Bistolfi 10, Torino", "latitude" to 45.0632, "longitude" to 7.6258),
                    "venue_02" to mapOf("name" to "Centro Sportivo Crespi - Milano", "address" to "Via Valvassori Peroni 48, Milano", "latitude" to 45.4795, "longitude" to 9.2327),
                    "venue_03" to mapOf("name" to "PalaDozza - Bologna", "address" to "Piazza Azzarita 3, Bologna", "latitude" to 44.5005, "longitude" to 11.3323),
                    "venue_04" to mapOf("name" to "Palasport Mens Sana - Siena", "address" to "Viale Sclavo 12, Siena", "latitude" to 43.3364, "longitude" to 11.3142)
                )
                for ((id, data) in venues) {
                    db.collection("venues").document(id).set(data).await()
                }

                // 3. SQUADRE DI DODGEBALL
                val teams = mapOf(
                    "team_01" to mapOf("name" to "Torino Vipers Dodgeball", "city" to "Torino"),
                    "team_02" to mapOf("name" to "Milano Fireballs", "city" to "Milano"),
                    "team_03" to mapOf("name" to "Bologna Ball Busters", "city" to "Bologna"),
                    "team_04" to mapOf("name" to "Toscana Titans", "city" to "Siena"),
                    "team_05" to mapOf("name" to "Roma Gladiators Dodgeball", "city" to "Roma"),
                    "team_06" to mapOf("name" to "Vesuvio Kraken", "city" to "Napoli")
                )
                for ((id, data) in teams) {
                    db.collection("teams").document(id).set(data).await()
                }

                // Generatore di date coerenti
                val cal = Calendar.getInstance()
                fun getOffsetTimestamp(days: Int, hour: Int): Timestamp {
                    val c = cal.clone() as Calendar
                    c.add(Calendar.DAY_OF_YEAR, days)
                    c.set(Calendar.HOUR_OF_DAY, hour)
                    c.set(Calendar.MINUTE, 0)
                    return Timestamp(c.time)
                }

                // 4. STORICO PARTITE STUDIATO A TAVOLINO PER ATTIVARE I BADGE REALI
                val mockMatches = listOf(

                    // --- COMBO PER IL TUO ACCOUNT (Sbloccherà: Il Grande Palco, Salvatore della Patria, Arbitro Eclettico) ---
                    mapOf(
                        "code" to "DB-ITA-201", "phase" to "Finale", "homeTeamId" to "team_01", "awayTeamId" to "team_02", "venueId" to "venue_01",
                        "status" to "finished", "refereeId" to "IL_TUO_VERO_UID_DI_FIREBASE", "coRefereeId" to "ref_01",
                        "homeScore" to 14, "awayScore" to 12, "category" to "Maschile",
                        "scheduledAt" to getOffsetTimestamp(-10, 16), "assignedAt" to getOffsetTimestamp(-10, 15) // <24 ore! (Salvatore della patria)
                    ),
                    mapOf(
                        "code" to "DB-ITA-202", "phase" to "Regular Season", "homeTeamId" to "team_03", "awayTeamId" to "team_04", "venueId" to "venue_02",
                        "status" to "finished", "refereeId" to "IL_TUO_VERO_UID_DI_FIREBASE", "coRefereeId" to "ref_01",
                        "homeScore" to 8, "awayScore" to 16, "category" to "Femminile",
                        "scheduledAt" to getOffsetTimestamp(-9, 18), "assignedAt" to getOffsetTimestamp(-15, 12)
                    ),
                    mapOf(
                        "code" to "DB-ITA-203", "phase" to "Regular Season", "homeTeamId" to "team_05", "awayTeamId" to "team_06", "venueId" to "venue_03",
                        "status" to "finished", "refereeId" to "IL_TUO_VERO_UID_DI_FIREBASE", "coRefereeId" to "ref_01",
                        "homeScore" to 10, "awayScore" to 10, "category" to "Misto", // Terza categoria (Arbitro Eclettico) in terza palestra (Pioniere dei campi)
                        "scheduledAt" to getOffsetTimestamp(-8, 20), "assignedAt" to getOffsetTimestamp(-14, 10)
                    ),

                    // Nota sulle prime 3 partite sopra: Hai arbitrato 3 volte di fila con "ref_01",
                    // sbloccando istantaneamente anche il badge "Coppia Fissa"! Inoltre, essendo avvenute
                    // a -10, -9 e -8 giorni da oggi, ricadono nella stessa settimana solare sbloccando "Stakanovista".

                    // --- COMBO PER L'ARBITRO CLONATO "ref_02" (Sbloccherà solo "Pioniere dei Campi") ---
                    mapOf(
                        "code" to "DB-ITA-301", "phase" to "Regular Season", "homeTeamId" to "team_02", "awayTeamId" to "team_04", "venueId" to "venue_01",
                        "status" to "finished", "refereeId" to "ref_02", "coRefereeId" to "ref_03",
                        "homeScore" to 6, "awayScore" to 14, "category" to "Maschile",
                        "scheduledAt" to getOffsetTimestamp(-20, 15), "assignedAt" to getOffsetTimestamp(-25, 15)
                    ),
                    mapOf(
                        "code" to "DB-ITA-302", "phase" to "Regular Season", "homeTeamId" to "team_01", "awayTeamId" to "team_06", "venueId" to "venue_02",
                        "status" to "finished", "refereeId" to "ref_02", "coRefereeId" to "ref_04",
                        "homeScore" to 12, "awayScore" to 12, "category" to "Maschile",
                        "scheduledAt" to getOffsetTimestamp(-18, 17), "assignedAt" to getOffsetTimestamp(-25, 15)
                    ),
                    mapOf(
                        "code" to "DB-ITA-303", "phase" to "Regular Season", "homeTeamId" to "team_03", "awayTeamId" to "team_05", "venueId" to "venue_04",
                        "status" to "finished", "refereeId" to "ref_02", "coRefereeId" to "ref_03",
                        "homeScore" to 18, "awayScore" to 6, "category" to "Maschile",
                        "scheduledAt" to getOffsetTimestamp(-5, 14), "assignedAt" to getOffsetTimestamp(-10, 15)
                    ),

                    // --- PARTITE FUTURE / DA FARE (Non influiscono sui badge) ---
                    mapOf(
                        "code" to "DB-ITA-401", "phase" to "Regular Season", "homeTeamId" to "team_01", "awayTeamId" to "team_04", "venueId" to "venue_01",
                        "status" to "assigned", "refereeId" to "IL_TUO_VERO_UID_DI_FIREBASE", "coRefereeId" to null, "refereeApplications" to emptyList<String>(),
                        "homeScore" to 0, "awayScore" to 0, "category" to "Misto",
                        "scheduledAt" to getOffsetTimestamp(2, 18), "assignedAt" to Timestamp.now()
                    )
                )

                for ((index, matchMap) in mockMatches.withIndex()) {
                    val idSuffix = if (index + 1 < 10) "0${index + 1}" else "${index + 1}"
                    db.collection("matches").document("match_$idSuffix").set(matchMap).await()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}