package com.example.fairball.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * FirebaseDataSeeder
 * ---------------
 * Seeder "da presentazione finale": popola Firestore con un dataset ricco e coerente,
 * pensato per mostrare TUTTI gli stati e i flussi dell'app durante una demo live.
 *
 * A differenza di FirebaseDataSeeder (dati minimi di sviluppo), questo:
 *  - crea un account arbitro DEMO dedicato (non il tuo account reale) da usare per il login
 *    durante la presentazione, così puoi mostrare "il mio profilo" senza toccare i tuoi dati;
 *  - copre ogni stato di partita (pending con candidature, assigned, waiting_approval,
 *    finished, rejected) così ogni schermata dell'app ha qualcosa di significativo da mostrare;
 *  - pre-popola anche le notifiche, così il badge "non lette" e il centro notifiche
 *    non sono vuoti al primo avvio della demo;
 *  - include account reali aggiuntivi (2 amministratori + 2 arbitri) con partite già
 *    arbitrate e notifiche correlate, per test multi-utente oltre alla demo.
 *
 * COME USARLO
 *  1. Crea in Firebase Authentication un utente di test con email/password a piacere
 *     per ciascuno degli UID elencati sotto (DEMO_REFEREE_UID + i nuovi account reali).
 *  2. Copia gli UID reali e sostituiscili nelle costanti sotto (o negli id dei documenti
 *     "users", che devono coincidere con l'UID Firebase Auth).
 *  3. Chiama DemoDataSeeder.seedPresentationData() una volta (es. da un bottone di debug
 *     o da Application.onCreate in build debug). Di default CANCELLA e ricrea tutto,
 *     quindi è ripetibile ad ogni prova della presentazione.
 *  4. Fai login con l'utente desiderato per mostrare la demo dal relativo punto di vista.
 */
object FirebaseDataSeeder {

    private val db = FirebaseFirestore.getInstance()

    private const val DEMO_REFEREE_UID = "demo_arbitro_presentazione"

    private const val ADMIN_PAOLO_UID = "5TgfuRpKrweOzCgco9RPrD2hKMd2"    // paolofoschini04@gmail.com
    private const val ADMIN_DYNAMO_UID = "ZgsKN7utHPOpC76XHOlNDi1nbY52"  // dynamo.grizzly@gmail.com
    private const val REF_POL_UID = "VwSFtWVbqsP6V5rg5pDNND8XFjy2"       // pol.foschini@gmail.com
    private const val REF_VALERII_UID = "DHPllkiADaMBMz3Cb2QKUR5SCt83"  // valerii.sargov@gmail.com

    private lateinit var cal: Calendar

    /**
     * Cancella tutti i documenti di una collection, gestendo il limite di 500
     * operazioni per batch di Firestore.
     */
    private suspend fun clearCollection(collectionName: String) {
        val snapshot = db.collection(collectionName).get().await()
        if (snapshot.isEmpty) return
        snapshot.documents.chunked(450).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
        }
    }

    private fun ts(daysOffset: Int, hour: Int, minute: Int = 0): Timestamp {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_YEAR, daysOffset)
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, minute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return Timestamp(c.time)
    }

    /**
     * Entry point "fire and forget", comodo da chiamare da un bottone UI.
     */
    fun seedData(clearExisting: Boolean = true) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                seedPresentationData(clearExisting)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun seedPresentationData(clearExisting: Boolean = true) {
        if (clearExisting) {
            clearCollection("notifications")
            clearCollection("matches")
            clearCollection("teams")
            clearCollection("venues")
            clearCollection("users")
        }

        cal = Calendar.getInstance()

        seedUsers()
        seedVenues()
        seedTeams()
        seedMatches()
        seedNotifications()
    }

    // ---------------------------------------------------------------
    // 1. UTENTI: admin, arbitro demo (per il login) + roster di arbitri
    //    + account reali aggiuntivi (2 admin, 2 arbitri)
    // ---------------------------------------------------------------
    private suspend fun seedUsers() {
        val users = mapOf(
            "admin_01" to mapOf(
                "displayName" to "Giulia Bianchi",
                "email" to "giulia.bianchi@fairball.com",
                "role" to "admin",
                "photoUrl" to null
            ),
            "admin_02" to mapOf(
                "displayName" to "Marco Ferri",
                "email" to "marco.ferri@fairball.com",
                "role" to "admin",
                "photoUrl" to null
            ),

            // --- Nuovi account amministratori reali ---
            ADMIN_PAOLO_UID to mapOf(
                "displayName" to "Paolo Foschini",
                "email" to "paolofoschini04@gmail.com",
                "role" to "admin",
                "photoUrl" to null
            ),
            ADMIN_DYNAMO_UID to mapOf(
                "displayName" to "Dynamo Grizzly",
                "email" to "dynamo.grizzly@gmail.com",
                "role" to "admin",
                "photoUrl" to null
            ),

            DEMO_REFEREE_UID to mapOf(
                "displayName" to "Arbitro Demo",
                "email" to "demo.arbitro@fairball.com",
                "role" to "referee",
                "photoUrl" to null
            ),
            "ref_01" to mapOf("displayName" to "Alessandro Riva", "email" to "alessandro.riva@fairball.com", "role" to "referee", "photoUrl" to null),
            "ref_02" to mapOf("displayName" to "Marco Visentin", "email" to "marco.vise@fairball.com", "role" to "referee", "photoUrl" to null),
            "ref_03" to mapOf("displayName" to "Luca Gatti", "email" to "luca.gatti@fairball.com", "role" to "referee", "photoUrl" to null),
            "ref_04" to mapOf("displayName" to "Davide Longo", "email" to "davide.longo@fairball.com", "role" to "referee", "photoUrl" to null),
            "ref_05" to mapOf("displayName" to "Sara Conti", "email" to "sara.conti@fairball.com", "role" to "referee", "photoUrl" to null),
            "ref_06" to mapOf("displayName" to "Elena Moretti", "email" to "elena.moretti@fairball.com", "role" to "referee", "photoUrl" to null),
            "ref_07" to mapOf("displayName" to "Paolo Serra", "email" to "paolo.serra@fairball.com", "role" to "referee", "photoUrl" to null),

            // --- Nuovi account arbitri reali ---
            REF_POL_UID to mapOf(
                "displayName" to "Pol Foschini",
                "email" to "pol.foschini@gmail.com",
                "role" to "referee",
                "photoUrl" to null
            ),
            REF_VALERII_UID to mapOf(
                "displayName" to "Valerii Sargov",
                "email" to "valerii.sargov@gmail.com",
                "role" to "referee",
                "photoUrl" to null
            )
        )
        for ((id, data) in users) {
            db.collection("users").document(id).set(data).await()
        }
    }

    // ---------------------------------------------------------------
    // 2. PALESTRE (6, città diverse: utile per mostrare mappe/filtri)
    // ---------------------------------------------------------------
    private suspend fun seedVenues() {
        val venues = mapOf(
            "venue_01" to mapOf("name" to "PalaRuffini - Torino", "address" to "Viale Bistolfi 10, Torino", "latitude" to 45.0632, "longitude" to 7.6258),
            "venue_02" to mapOf("name" to "Centro Sportivo Crespi - Milano", "address" to "Via Valvassori Peroni 48, Milano", "latitude" to 45.4795, "longitude" to 9.2327),
            "venue_03" to mapOf("name" to "PalaDozza - Bologna", "address" to "Piazza Azzarita 3, Bologna", "latitude" to 44.5005, "longitude" to 11.3323),
            "venue_04" to mapOf("name" to "Palasport Mens Sana - Siena", "address" to "Viale Sclavo 12, Siena", "latitude" to 43.3364, "longitude" to 11.3142),
            "venue_05" to mapOf("name" to "PalaFlorio - Bari", "address" to "Via Giulio Petroni 15, Bari", "latitude" to 41.1256, "longitude" to 16.8623),
            "venue_06" to mapOf("name" to "PalaEur - Roma", "address" to "Piazzale dello Sport 1, Roma", "latitude" to 41.8368, "longitude" to 12.4707)
        )
        for ((id, data) in venues) {
            db.collection("venues").document(id).set(data).await()
        }
    }

    // ---------------------------------------------------------------
    // 3. SQUADRE (8, città diverse)
    // ---------------------------------------------------------------
    private suspend fun seedTeams() {
        val teams = mapOf(
            "team_01" to mapOf("name" to "Torino Vipers Dodgeball", "city" to "Torino"),
            "team_02" to mapOf("name" to "Milano Fireballs", "city" to "Milano"),
            "team_03" to mapOf("name" to "Bologna Ball Busters", "city" to "Bologna"),
            "team_04" to mapOf("name" to "Toscana Titans", "city" to "Siena"),
            "team_05" to mapOf("name" to "Roma Gladiators Dodgeball", "city" to "Roma"),
            "team_06" to mapOf("name" to "Vesuvio Kraken", "city" to "Napoli"),
            "team_07" to mapOf("name" to "Bari Squali", "city" to "Bari"),
            "team_08" to mapOf("name" to "Palermo Falchi", "city" to "Palermo")
        )
        for ((id, data) in teams) {
            db.collection("teams").document(id).set(data).await()
        }
    }

    // ---------------------------------------------------------------
    // 4. PARTITE: una per ogni stato/flusso rilevante dell'app
    //    + partite già arbitrate dai 2 nuovi arbitri reali
    // ---------------------------------------------------------------
    private suspend fun seedMatches() {
        val matches = linkedMapOf(

            // --- PENDING con candidature aperte: mostra la coda di approvazione admin ---
            "match_01" to mapOf(
                "code" to "DB-ITA-101", "phase" to "Regular Season", "category" to "Maschile",
                "homeTeamId" to "team_01", "awayTeamId" to "team_02", "venueId" to "venue_01",
                "status" to "pending", "scheduledAt" to ts(5, 18),
                "refereeId" to null, "coRefereeId" to null,
                "refereeApplications" to listOf("ref_05", "ref_06", DEMO_REFEREE_UID),
                "homeScore" to 0, "awayScore" to 0
            ),

            // --- PENDING senza candidature: partita "nuova", appena pubblicata ---
            "match_02" to mapOf(
                "code" to "DB-ITA-102", "phase" to "Regular Season", "category" to "Femminile",
                "homeTeamId" to "team_07", "awayTeamId" to "team_08", "venueId" to "venue_05",
                "status" to "pending", "scheduledAt" to ts(7, 20),
                "refereeId" to null, "coRefereeId" to null,
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 0, "awayScore" to 0
            ),

            // --- ASSIGNED all'arbitro demo: la "prossima partita" da mostrare in home ---
            "match_03" to mapOf(
                "code" to "DB-ITA-103", "phase" to "Regular Season", "category" to "Misto",
                "homeTeamId" to "team_03", "awayTeamId" to "team_04", "venueId" to "venue_03",
                "status" to "assigned", "scheduledAt" to ts(2, 19),
                "refereeId" to DEMO_REFEREE_UID, "coRefereeId" to "ref_01",
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 0, "awayScore" to 0,
                "assignedAt" to ts(-1, 9)
            ),

            // --- ASSIGNED ad altri arbitri: riempie il calendario generale ---
            "match_04" to mapOf(
                "code" to "DB-ITA-104", "phase" to "Regular Season", "category" to "Maschile",
                "homeTeamId" to "team_05", "awayTeamId" to "team_06", "venueId" to "venue_06",
                "status" to "assigned", "scheduledAt" to ts(3, 17),
                "refereeId" to "ref_02", "coRefereeId" to "ref_03",
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 0, "awayScore" to 0,
                "assignedAt" to ts(-2, 10)
            ),

            // --- WAITING_APPROVAL per l'arbitro demo: mostra lo schermo "invia referto" già fatto ---
            "match_05" to mapOf(
                "code" to "DB-ITA-105", "phase" to "Semifinale", "category" to "Maschile",
                "homeTeamId" to "team_01", "awayTeamId" to "team_05", "venueId" to "venue_01",
                "status" to "waiting_approval", "scheduledAt" to ts(-1, 18),
                "refereeId" to DEMO_REFEREE_UID, "coRefereeId" to "ref_04",
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 15, "awayScore" to 13,
                "assignedAt" to ts(-6, 10),
                "photoDistintaA" to "https://placehold.co/600x800?text=Distinta+A",
                "photoDistintaB" to "https://placehold.co/600x800?text=Distinta+B",
                "photoReferto" to "https://placehold.co/600x800?text=Referto",
                "adminComment" to null
            ),

            // --- WAITING_APPROVAL di un altro arbitro: coda approvazioni admin non vuota ---
            "match_06" to mapOf(
                "code" to "DB-ITA-106", "phase" to "Regular Season", "category" to "Femminile",
                "homeTeamId" to "team_02", "awayTeamId" to "team_08", "venueId" to "venue_02",
                "status" to "waiting_approval", "scheduledAt" to ts(-2, 20),
                "refereeId" to "ref_05", "coRefereeId" to null,
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 9, "awayScore" to 12,
                "assignedAt" to ts(-7, 9),
                "photoDistintaA" to "https://placehold.co/600x800?text=Distinta+A",
                "photoDistintaB" to "https://placehold.co/600x800?text=Distinta+B",
                "photoReferto" to "https://placehold.co/600x800?text=Referto",
                "adminComment" to null
            ),

            // --- REJECTED: mostra il flusso di correzione referto ---
            "match_07" to mapOf(
                "code" to "DB-ITA-107", "phase" to "Regular Season", "category" to "Maschile",
                "homeTeamId" to "team_06", "awayTeamId" to "team_07", "venueId" to "venue_05",
                "status" to "rejected", "scheduledAt" to ts(-4, 18),
                "refereeId" to "ref_04", "coRefereeId" to null,
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 11, "awayScore" to 11,
                "assignedAt" to ts(-9, 9),
                "photoDistintaA" to "https://placehold.co/600x800?text=Distinta+A",
                "photoDistintaB" to "https://placehold.co/600x800?text=Distinta+B",
                "photoReferto" to "https://placehold.co/600x800?text=Referto+illeggibile",
                "adminComment" to "La foto del referto è sfocata, per favore ricaricala leggibile."
            ),

            // --- FINISHED (storico) per l'arbitro demo: 3 partite ravvicinate con lo stesso
            //     co-arbitro, in 3 palestre e 3 categorie diverse, una assegnata a <24h dalla
            //     partita: dataset pensato per attivare visivamente più badge/statistiche insieme.
            "match_08" to mapOf(
                "code" to "DB-ITA-201", "phase" to "Finale", "category" to "Maschile",
                "homeTeamId" to "team_01", "awayTeamId" to "team_02", "venueId" to "venue_01",
                "status" to "finished", "scheduledAt" to ts(-10, 16),
                "refereeId" to DEMO_REFEREE_UID, "coRefereeId" to "ref_01",
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 14, "awayScore" to 12,
                "assignedAt" to ts(-10, 15)
            ),
            "match_09" to mapOf(
                "code" to "DB-ITA-202", "phase" to "Regular Season", "category" to "Femminile",
                "homeTeamId" to "team_03", "awayTeamId" to "team_04", "venueId" to "venue_02",
                "status" to "finished", "scheduledAt" to ts(-9, 18),
                "refereeId" to DEMO_REFEREE_UID, "coRefereeId" to "ref_01",
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 8, "awayScore" to 16,
                "assignedAt" to ts(-15, 12)
            ),
            "match_10" to mapOf(
                "code" to "DB-ITA-203", "phase" to "Regular Season", "category" to "Misto",
                "homeTeamId" to "team_05", "awayTeamId" to "team_06", "venueId" to "venue_03",
                "status" to "finished", "scheduledAt" to ts(-8, 20),
                "refereeId" to DEMO_REFEREE_UID, "coRefereeId" to "ref_01",
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 10, "awayScore" to 10,
                "assignedAt" to ts(-14, 10)
            ),

            // --- FINISHED per altri arbitri: storico generale del campionato ---
            "match_11" to mapOf(
                "code" to "DB-ITA-301", "phase" to "Regular Season", "category" to "Maschile",
                "homeTeamId" to "team_02", "awayTeamId" to "team_04", "venueId" to "venue_01",
                "status" to "finished", "scheduledAt" to ts(-20, 15),
                "refereeId" to "ref_02", "coRefereeId" to "ref_03",
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 6, "awayScore" to 14,
                "assignedAt" to ts(-25, 15)
            ),
            "match_12" to mapOf(
                "code" to "DB-ITA-302", "phase" to "Regular Season", "category" to "Maschile",
                "homeTeamId" to "team_01", "awayTeamId" to "team_06", "venueId" to "venue_02",
                "status" to "finished", "scheduledAt" to ts(-18, 17),
                "refereeId" to "ref_02", "coRefereeId" to "ref_04",
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 12, "awayScore" to 12,
                "assignedAt" to ts(-25, 15)
            ),
            "match_13" to mapOf(
                "code" to "DB-ITA-303", "phase" to "Regular Season", "category" to "Femminile",
                "homeTeamId" to "team_07", "awayTeamId" to "team_08", "venueId" to "venue_04",
                "status" to "finished", "scheduledAt" to ts(-5, 14),
                "refereeId" to "ref_06", "coRefereeId" to "ref_03",
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 18, "awayScore" to 6,
                "assignedAt" to ts(-10, 15)
            ),

            // --- Partita lontana nel futuro, ancora del tutto libera ---
            "match_14" to mapOf(
                "code" to "DB-ITA-401", "phase" to "Regular Season", "category" to "Misto",
                "homeTeamId" to "team_04", "awayTeamId" to "team_08", "venueId" to "venue_04",
                "status" to "pending", "scheduledAt" to ts(12, 19),
                "refereeId" to null, "coRefereeId" to null,
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 0, "awayScore" to 0
            ),

            // =========================================================
            // Partite dedicate ai nuovi arbitri reali (Pol Foschini e
            // Valerii Sargov): storico "finished" + una in corso di
            // approvazione + una assegnata, così i loro profili non
            // sono vuoti al primo login.
            // =========================================================

            // --- FINISHED: Pol Foschini arbitro, Valerii Sargov co-arbitro ---
            "match_15" to mapOf(
                "code" to "DB-ITA-401", "phase" to "Regular Season", "category" to "Maschile",
                "homeTeamId" to "team_01", "awayTeamId" to "team_02", "venueId" to "venue_01",
                "status" to "finished", "scheduledAt" to ts(-12, 18),
                "refereeId" to REF_POL_UID, "coRefereeId" to REF_VALERII_UID,
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 13, "awayScore" to 9,
                "assignedAt" to ts(-18, 11)
            ),

            // --- FINISHED: Valerii Sargov arbitro, Pol Foschini co-arbitro ---
            "match_16" to mapOf(
                "code" to "DB-ITA-402", "phase" to "Regular Season", "category" to "Misto",
                "homeTeamId" to "team_05", "awayTeamId" to "team_06", "venueId" to "venue_06",
                "status" to "finished", "scheduledAt" to ts(-11, 20),
                "refereeId" to REF_VALERII_UID, "coRefereeId" to REF_POL_UID,
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 16, "awayScore" to 14,
                "assignedAt" to ts(-17, 10)
            ),

            // --- FINISHED: Pol Foschini da solo (senza co-arbitro) ---
            "match_17" to mapOf(
                "code" to "DB-ITA-403", "phase" to "Regular Season", "category" to "Femminile",
                "homeTeamId" to "team_07", "awayTeamId" to "team_08", "venueId" to "venue_05",
                "status" to "finished", "scheduledAt" to ts(-6, 17),
                "refereeId" to REF_POL_UID, "coRefereeId" to null,
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 17, "awayScore" to 15,
                "assignedAt" to ts(-11, 9)
            ),

            // --- FINISHED: Valerii Sargov da solo (senza co-arbitro) ---
            "match_18" to mapOf(
                "code" to "DB-ITA-404", "phase" to "Regular Season", "category" to "Maschile",
                "homeTeamId" to "team_03", "awayTeamId" to "team_04", "venueId" to "venue_03",
                "status" to "finished", "scheduledAt" to ts(-3, 19),
                "refereeId" to REF_VALERII_UID, "coRefereeId" to null,
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 7, "awayScore" to 19,
                "assignedAt" to ts(-8, 9)
            ),

            // --- WAITING_APPROVAL: Pol Foschini in attesa di approvazione admin ---
            "match_19" to mapOf(
                "code" to "DB-ITA-405", "phase" to "Semifinale", "category" to "Maschile",
                "homeTeamId" to "team_02", "awayTeamId" to "team_05", "venueId" to "venue_02",
                "status" to "waiting_approval", "scheduledAt" to ts(-1, 20),
                "refereeId" to REF_POL_UID, "coRefereeId" to REF_VALERII_UID,
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 12, "awayScore" to 15,
                "assignedAt" to ts(-6, 9),
                "photoDistintaA" to "https://placehold.co/600x800?text=Distinta+A",
                "photoDistintaB" to "https://placehold.co/600x800?text=Distinta+B",
                "photoReferto" to "https://placehold.co/600x800?text=Referto",
                "adminComment" to null
            ),

            // --- ASSIGNED: Valerii Sargov, prossima partita in calendario ---
            "match_20" to mapOf(
                "code" to "DB-ITA-406", "phase" to "Regular Season", "category" to "Misto",
                "homeTeamId" to "team_06", "awayTeamId" to "team_08", "venueId" to "venue_06",
                "status" to "assigned", "scheduledAt" to ts(4, 18),
                "refereeId" to REF_VALERII_UID, "coRefereeId" to REF_POL_UID,
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 0, "awayScore" to 0,
                "assignedAt" to ts(-1, 10)
            )
        )

        for ((docId, matchData) in matches) {
            db.collection("matches").document(docId).set(matchData).await()
        }
    }

    // ---------------------------------------------------------------
    // 5. NOTIFICHE: pre-popolate così il centro notifiche non è vuoto
    //    all'apertura della demo (sia lato arbitro demo che lato admin,
    //    sia lato nuovi account reali).
    // ---------------------------------------------------------------
    private suspend fun seedNotifications() {
        val notifications = listOf(
            mapOf(
                "recipientUid" to DEMO_REFEREE_UID,
                "type" to "new_match",
                "title" to "Nuova partita disponibile",
                "message" to "Bari Squali vs Palermo Falchi è stata pubblicata: candidati se vuoi arbitrarla.",
                "relatedMatchId" to "match_02",
                "read" to true,
                "createdAt" to ts(-1, 9, 30)
            ),
            mapOf(
                "recipientUid" to DEMO_REFEREE_UID,
                "type" to "assigned",
                "title" to "Sei stato assegnato a una partita",
                "message" to "Sei stato assegnato come arbitro per Bologna Ball Busters vs Toscana Titans.",
                "relatedMatchId" to "match_03",
                "read" to true,
                "createdAt" to ts(-1, 9)
            ),
            mapOf(
                "recipientUid" to DEMO_REFEREE_UID,
                "type" to "result_published",
                "title" to "Risultato pubblicato",
                "message" to "Il referto di Roma Gladiators vs Vesuvio Kraken è stato approvato ed è ora visibile nella pagina del campionato.",
                "relatedMatchId" to "match_10",
                "read" to true,
                "createdAt" to ts(-7, 12)
            ),
            mapOf(
                "recipientUid" to "admin_01",
                "type" to "referee_request",
                "title" to "Nuova richiesta di arbitraggio",
                "message" to "Arbitro Demo si è candidato per arbitrare una partita.",
                "relatedMatchId" to "match_01",
                "read" to true,
                "createdAt" to ts(-1, 9, 15)
            ),
            mapOf(
                "recipientUid" to "admin_01",
                "type" to "approval_request",
                "title" to "Referto da verificare",
                "message" to "Un arbitro ha inviato risultato e documenti da approvare.",
                "relatedMatchId" to "match_05",
                "read" to true,
                "createdAt" to ts(-1, 18, 30)
            ),
            mapOf(
                "recipientUid" to "ref_04",
                "type" to "result_rejected",
                "title" to "Modifiche richieste al referto",
                "message" to "La foto del referto è sfocata, per favore ricaricala leggibile.",
                "relatedMatchId" to "match_07",
                "read" to true,
                "createdAt" to ts(-4, 20)
            ),

            // --- Notifiche per i nuovi arbitri reali ---
            mapOf(
                "recipientUid" to REF_POL_UID,
                "type" to "result_published",
                "title" to "Risultato pubblicato",
                "message" to "Il referto di Torino Vipers vs Milano Fireballs è stato approvato ed è ora visibile nella pagina del campionato.",
                "relatedMatchId" to "match_15",
                "read" to true,
                "createdAt" to ts(-11, 12)
            ),
            mapOf(
                "recipientUid" to REF_POL_UID,
                "type" to "assigned",
                "title" to "Sei stato assegnato a una partita",
                "message" to "Sei stato assegnato come arbitro per Milano Fireballs vs Roma Gladiators Dodgeball.",
                "relatedMatchId" to "match_19",
                "read" to true,
                "createdAt" to ts(-6, 9, 5)
            ),
            mapOf(
                "recipientUid" to REF_VALERII_UID,
                "type" to "result_published",
                "title" to "Risultato pubblicato",
                "message" to "Il referto di Roma Gladiators Dodgeball vs Vesuvio Kraken è stato approvato ed è ora visibile nella pagina del campionato.",
                "relatedMatchId" to "match_16",
                "read" to true,
                "createdAt" to ts(-10, 13)
            ),
            mapOf(
                "recipientUid" to REF_VALERII_UID,
                "type" to "assigned",
                "title" to "Sei stato assegnato a una partita",
                "message" to "Sei stato assegnato come arbitro per Vesuvio Kraken vs Palermo Falchi.",
                "relatedMatchId" to "match_20",
                "read" to true,
                "createdAt" to ts(-1, 10, 5)
            ),

            // --- Notifiche per i nuovi amministratori reali ---
            mapOf(
                "recipientUid" to ADMIN_PAOLO_UID,
                "type" to "approval_request",
                "title" to "Referto da verificare",
                "message" to "Pol Foschini ha inviato risultato e documenti da approvare per Milano Fireballs vs Roma Gladiators Dodgeball.",
                "relatedMatchId" to "match_19",
                "read" to true,
                "createdAt" to ts(-1, 20, 15)
            ),
            mapOf(
                "recipientUid" to ADMIN_PAOLO_UID,
                "type" to "referee_request",
                "title" to "Nuova richiesta di arbitraggio",
                "message" to "Arbitro Demo si è candidato per arbitrare Torino Vipers vs Milano Fireballs.",
                "relatedMatchId" to "match_01",
                "read" to true,
                "createdAt" to ts(-1, 9, 20)
            ),
            mapOf(
                "recipientUid" to ADMIN_DYNAMO_UID,
                "type" to "approval_request",
                "title" to "Referto da verificare",
                "message" to "Un arbitro ha inviato risultato e documenti da approvare per Bologna Ball Busters vs Toscana Titans.",
                "relatedMatchId" to "match_06",
                "read" to true,
                "createdAt" to ts(-2, 20, 30)
            ),
            mapOf(
                "recipientUid" to ADMIN_DYNAMO_UID,
                "type" to "new_match_published",
                "title" to "Nuova partita pubblicata",
                "message" to "La partita Toscana Titans vs Palermo Falchi è stata pubblicata correttamente.",
                "relatedMatchId" to "match_14",
                "read" to true,
                "createdAt" to ts(-2, 11)
            )
        )

        for (data in notifications) {
            db.collection("notifications").document().set(data).await()
        }
    }
}