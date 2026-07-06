package com.example.fairball.data

import com.example.fairball.model.Match
import com.example.fairball.model.Notification
import com.example.fairball.model.NotificationType
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.example.fairball.model.Venue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar

object FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    fun matchesFlow(): Flow<List<Match>> =
        db.collection("matches")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Match::class.java)?.copy(id = doc.id)
                }
            }

    fun teamsFlow(): Flow<List<Team>> =
        db.collection("teams")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Team::class.java)?.copy(id = doc.id)
                }
            }

    fun venuesFlow(): Flow<List<Venue>> =
        db.collection("venues")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Venue::class.java)?.copy(id = doc.id)
                }
            }

    fun refereesFlow(): Flow<List<User>> =
        db.collection("users")
            .whereEqualTo("role", "referee")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(uid = doc.id)
                }
            }

    fun usersFlow(): Flow<List<User>> =
        db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(uid = doc.id)
                }
            }

    fun notificationsFlow(uid: String): Flow<List<Notification>> =
        db.collection("notifications")
            .whereEqualTo("recipientUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)?.copy(id = doc.id)
                }
            }

    fun unreadNotificationCountFlow(uid: String): Flow<Int> =
        db.collection("notifications")
            .whereEqualTo("recipientUid", uid)
            .whereEqualTo("read", false)
            .snapshots()
            .map { it.size() }

    suspend fun markNotificationRead(notificationId: String) {
        db.collection("notifications").document(notificationId).update("read", true).await()
    }

    suspend fun markAllNotificationsRead(uid: String) {
        val unread = db.collection("notifications")
            .whereEqualTo("recipientUid", uid)
            .whereEqualTo("read", false)
            .get().await()
        val batch = db.batch()
        for (doc in unread.documents) {
            batch.update(doc.reference, "read", true)
        }
        batch.commit().await()
    }

    private suspend fun createNotification(
        recipientUid: String,
        type: String,
        title: String,
        message: String,
        relatedMatchId: String? = null
    ) {
        val docRef = db.collection("notifications").document()
        val notification = Notification(
            id = docRef.id,
            recipientUid = recipientUid,
            type = type,
            title = title,
            message = message,
            relatedMatchId = relatedMatchId,
            read = false,
            createdAt = Timestamp.now()
        )
        docRef.set(notification).await()
    }

    private suspend fun notifyAdmins(type: String, title: String, message: String, relatedMatchId: String? = null) {
        val admins = db.collection("users").whereEqualTo("role", "admin").get().await()
        val batch = db.batch()
        for (doc in admins.documents) {
            val docRef = db.collection("notifications").document()
            val notification = Notification(
                id = docRef.id,
                recipientUid = doc.id,
                type = type,
                title = title,
                message = message,
                relatedMatchId = relatedMatchId,
                read = false,
                createdAt = Timestamp.now()
            )
            batch.set(docRef, notification)
        }
        batch.commit().await()
    }

    private suspend fun notifyAllReferees(type: String, title: String, message: String, relatedMatchId: String? = null) {
        val referees = db.collection("users").whereEqualTo("role", "referee").get().await()
        val batch = db.batch()
        for (doc in referees.documents) {
            val docRef = db.collection("notifications").document()
            val notification = Notification(
                id = docRef.id,
                recipientUid = doc.id,
                type = type,
                title = title,
                message = message,
                relatedMatchId = relatedMatchId,
                read = false,
                createdAt = Timestamp.now()
            )
            batch.set(docRef, notification)
        }
        batch.commit().await()
    }

    suspend fun fetchTeamNameMap(): Map<String, String> =
        db.collection("teams").get().await()
            .documents.associate { it.id to (it.getString("name") ?: it.id) }

    suspend fun fetchUserNameMap(): Map<String, String> =
        db.collection("users").get().await()
            .documents.associate { it.id to (it.getString("displayName") ?: it.id) }

    suspend fun fetchFinishedMatchesAtVenue(venueId: String): List<Match> =
        db.collection("matches")
            .whereEqualTo("venueId", venueId)
            .whereEqualTo("status", "finished")
            .get().await()
            .documents.mapNotNull { it.toObject(Match::class.java)?.copy(id = it.id) }

    suspend fun createMatch(match: Match): String {
        val docRef = db.collection("matches").document()
        val newMatch = match.copy(id = docRef.id)
        docRef.set(newMatch).await()

        val teamNames = fetchTeamNameMap()
        val homeName = teamNames[match.homeTeamId] ?: match.homeTeamId
        val awayName = teamNames[match.awayTeamId] ?: match.awayTeamId
        notifyAllReferees(
            type = NotificationType.NEW_MATCH,
            title = "Nuova partita disponibile",
            message = "$homeName vs $awayName è stata pubblicata: candidati se vuoi arbitrarla.",
            relatedMatchId = docRef.id
        )

        return docRef.id
    }

    suspend fun updateMatch(matchId: String, fields: Map<String, Any?>) {
        db.collection("matches").document(matchId).update(fields).await()
    }

    suspend fun deleteMatch(matchId: String) {
        db.collection("matches").document(matchId).delete().await()
    }

    suspend fun assignReferee(matchId: String, refereeUid: String, isCoReferee: Boolean = false) {
        val field = if (isCoReferee) "coRefereeId" else "refereeId"
        val updates = mutableMapOf<String, Any?>(field to refereeUid)
        if (!isCoReferee) {
            updates["assignedAt"] = Timestamp.now()
            updates["status"] = "assigned"
            updates["refereeApplications"] = emptyList<String>()
        }
        db.collection("matches").document(matchId).update(updates).await()

        val teamNames = fetchTeamNameMap()
        val matchSnapshot = db.collection("matches").document(matchId).get().await()
        val match = matchSnapshot.toObject(Match::class.java)
        val homeName = match?.let { teamNames[it.homeTeamId] ?: it.homeTeamId } ?: ""
        val awayName = match?.let { teamNames[it.awayTeamId] ?: it.awayTeamId } ?: ""
        val roleLabel = if (isCoReferee) "co-arbitro" else "arbitro"

        createNotification(
            recipientUid = refereeUid,
            type = NotificationType.ASSIGNED,
            title = "Sei stato assegnato a una partita",
            message = "Sei stato assegnato come $roleLabel per $homeName vs $awayName.",
            relatedMatchId = matchId
        )
    }

    suspend fun removeReferee(matchId: String, isCoReferee: Boolean = false) {
        val updates = mutableMapOf<String, Any?>()

        if (isCoReferee) {
            updates["coRefereeId"] = null
        } else {
            updates["refereeId"] = null
            updates["status"] = "pending"
        }

        db.collection("matches").document(matchId).update(updates).await()
    }

    suspend fun applyForMatch(matchId: String, refereeUid: String) {
        db.collection("matches").document(matchId)
            .update("refereeApplications", FieldValue.arrayUnion(refereeUid)).await()

        val refereeName = db.collection("users").document(refereeUid).get().await()
            .getString("displayName") ?: "Un arbitro"
        notifyAdmins(
            type = NotificationType.REFEREE_REQUEST,
            title = "Nuova richiesta di arbitraggio",
            message = "$refereeName si è candidato per arbitrare una partita.",
            relatedMatchId = matchId
        )
    }

    suspend fun withdrawApplication(matchId: String, refereeUid: String) {
        db.collection("matches").document(matchId)
            .update("refereeApplications", FieldValue.arrayRemove(refereeUid)).await()
    }

    suspend fun approveMatch(matchId: String) {
        db.collection("matches").document(matchId).update("status", "finished").await()

        val matchSnapshot = db.collection("matches").document(matchId).get().await()
        val match = matchSnapshot.toObject(Match::class.java)
        val teamNames = fetchTeamNameMap()
        val homeName = match?.let { teamNames[it.homeTeamId] ?: it.homeTeamId } ?: ""
        val awayName = match?.let { teamNames[it.awayTeamId] ?: it.awayTeamId } ?: ""
        val message = "Il referto di $homeName vs $awayName è stato approvato ed è ora visibile nella pagina del campionato."

        match?.refereeId?.let { uid ->
            createNotification(uid, NotificationType.RESULT_PUBLISHED, "Risultato pubblicato", message, matchId)
        }
        match?.coRefereeId?.takeIf { it.isNotEmpty() }?.let { uid ->
            createNotification(uid, NotificationType.RESULT_PUBLISHED, "Risultato pubblicato", message, matchId)
        }
    }

    suspend fun updateScore(matchId: String, homeScore: Int, awayScore: Int) {
        db.collection("matches").document(matchId).update(
            mapOf("homeScore" to homeScore, "awayScore" to awayScore)
        ).await()
    }

    suspend fun deleteUser(uid: String) {
        db.collection("users").document(uid).delete().await()
    }

    suspend fun updateUserPhoto(uid: String, photoUrl: String) {
        db.collection("users").document(uid).update("photoUrl", photoUrl).await()
    }

    suspend fun updateUserProfile(uid: String, displayName: String, email: String, role: String? = null) {
        val fields = mutableMapOf<String, Any?>(
            "displayName" to displayName,
            "email" to email
        )
        if (role != null) {
            fields["role"] = role
        }
        db.collection("users").document(uid).update(fields).await()
    }

    suspend fun rejectMatchReport(matchId: String, comment: String) {
        db.collection("matches").document(matchId).update(
            mapOf(
                "status" to "rejected",
                "adminComment" to comment
            )
        ).await()

        val match = db.collection("matches").document(matchId).get().await().toObject(Match::class.java)
        match?.refereeId?.let { uid ->
            createNotification(
                recipientUid = uid,
                type = NotificationType.RESULT_REJECTED,
                title = "Modifiche richieste al referto",
                message = comment,
                relatedMatchId = matchId
            )
        }
    }

    suspend fun submitMatchReport(matchId: String, photoA: String?, photoB: String?, photoRef: String?) {
        db.collection("matches").document(matchId).update(
            mapOf(
                "photoDistintaA" to photoA,
                "photoDistintaB" to photoB,
                "photoReferto" to photoRef,
                "status" to "waiting_approval",
                "adminComment" to null
            )
        ).await()

        notifyAdmins(
            type = NotificationType.APPROVAL_REQUEST,
            title = "Referto da verificare",
            message = "Un arbitro ha inviato risultato e documenti da approvare.",
            relatedMatchId = matchId
        )
    }

    suspend fun resetAndSeedMatches() {
        val matchesRef = db.collection("matches")

        try {
            val snapshot = matchesRef.get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        fun getRelativeTimestamp(daysOffset: Int, hour: Int): Timestamp {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, daysOffset)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return Timestamp(cal.time)
        }

        val mockMatches = listOf(
            mapOf(
                "homeTeamId" to "team_milan",
                "awayTeamId" to "team_inter",
                "venueId" to "palestra_centrale",
                "status" to "pending",
                "scheduledAt" to getRelativeTimestamp(2, 18),
                "refereeId" to null,
                "coRefereeId" to null,
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 0,
                "awayScore" to 0
            ),
            mapOf(
                "homeTeamId" to "team_juve",
                "awayTeamId" to "team_torino",
                "venueId" to "palestra_nord",
                "status" to "pending",
                "scheduledAt" to getRelativeTimestamp(4, 20),
                "refereeId" to null,
                "coRefereeId" to null,
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 0,
                "awayScore" to 0
            ),

            mapOf(
                "homeTeamId" to "team_roma",
                "awayTeamId" to "team_lazio",
                "venueId" to "palestra_sud",
                "status" to "assigned",
                "scheduledAt" to getRelativeTimestamp(1, 19),
                "refereeId" to "ID_DI_UN_ARBITRO_ESISTENTE",
                "coRefereeId" to null,
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 0,
                "awayScore" to 0
            ),

            mapOf(
                "homeTeamId" to "team_napoli",
                "awayTeamId" to "team_fiorentina",
                "venueId" to "palestra_est",
                "status" to "finished",
                "scheduledAt" to getRelativeTimestamp(-3, 15),
                "refereeId" to "ID_DI_UN_ARBITRO_ESISTENTE",
                "coRefereeId" to null,
                "refereeApplications" to emptyList<String>(),
                "homeScore" to 3,
                "awayScore" to 1
            )
        )

        for (matchData in mockMatches) {
            matchesRef.add(matchData).await()
        }
    }
}