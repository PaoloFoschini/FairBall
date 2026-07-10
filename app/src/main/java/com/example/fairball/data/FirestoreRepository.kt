package com.example.fairball.data

import com.example.fairball.model.Match
import com.example.fairball.model.MatchStatus
import com.example.fairball.model.Notification
import com.example.fairball.model.NotificationType
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.example.fairball.model.Venue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

object FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    private inline fun <reified T : Any> queryFlow(
        query: Query,
        crossinline idAssigner: (T, String) -> T
    ): Flow<List<T>> = query.snapshots().map { snapshot ->
        snapshot.documents.mapNotNull { doc -> doc.toObject(T::class.java)?.let { idAssigner(it, doc.id) } }
    }

    fun matchesFlow(): Flow<List<Match>> =
        queryFlow(db.collection("matches")) { m, id -> m.copy(id = id) }

    fun teamsFlow(): Flow<List<Team>> =
        queryFlow(db.collection("teams")) { t, id -> t.copy(id = id) }

    fun venuesFlow(): Flow<List<Venue>> =
        queryFlow(db.collection("venues")) { v, id -> v.copy(id = id) }

    fun refereesFlow(): Flow<List<User>> =
        queryFlow(db.collection("users").whereEqualTo("role", "referee")) { u, id -> u.copy(uid = id) }

    fun usersFlow(): Flow<List<User>> =
        queryFlow(db.collection("users")) { u, id -> u.copy(uid = id) }

    fun notificationsFlow(uid: String): Flow<List<Notification>> =
        db.collection("notifications")
            .whereEqualTo("recipientUid", uid)
            // Nota: niente .orderBy() qui. Un whereEqualTo + orderBy su un campo
            // diverso richiede un indice composito su Firestore: se l'indice non
            // esiste la query fallisce silenziosamente e lo schermo resta bloccato
            // sullo spinner per sempre. Ordiniamo quindi lato client.
            .snapshots()
            .map { snapshot ->
                snapshot.documents
                    .mapNotNull { doc -> doc.toObject(Notification::class.java)?.copy(id = doc.id) }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { e ->
                e.printStackTrace()
                emit(emptyList())
            }

    fun unreadNotificationCountFlow(uid: String): Flow<Int> =
        db.collection("notifications")
            .whereEqualTo("recipientUid", uid)
            .whereEqualTo("read", false)
            .snapshots()
            .map { it.size() }
            .catch { e ->
                e.printStackTrace()
                emit(0)
            }

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

    private fun buildNotification(
        recipientUid: String,
        type: String,
        title: String,
        message: String,
        relatedMatchId: String?
    ): Pair<DocumentReference, Notification> {
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
        return docRef to notification
    }

    private suspend fun createNotification(
        recipientUid: String,
        type: String,
        title: String,
        message: String,
        relatedMatchId: String? = null
    ) {
        val (docRef, notification) = buildNotification(recipientUid, type, title, message, relatedMatchId)
        docRef.set(notification).await()
    }

    private suspend fun notifyUsersWithRole(
        role: String,
        type: String,
        title: String,
        message: String,
        relatedMatchId: String?
    ) {
        val recipients = db.collection("users").whereEqualTo("role", role).get().await()
        val batch = db.batch()
        for (doc in recipients.documents) {
            val (docRef, notification) = buildNotification(doc.id, type, title, message, relatedMatchId)
            batch.set(docRef, notification)
        }
        batch.commit().await()
    }

    private suspend fun notifyAdmins(type: String, title: String, message: String, relatedMatchId: String? = null) =
        notifyUsersWithRole("admin", type, title, message, relatedMatchId)

    private suspend fun notifyAllReferees(type: String, title: String, message: String, relatedMatchId: String? = null) =
        notifyUsersWithRole("referee", type, title, message, relatedMatchId)

    private suspend fun fetchNameMap(collectionName: String, field: String): Map<String, String> =
        db.collection(collectionName).get().await()
            .documents.associate { it.id to (it.getString(field) ?: it.id) }

    suspend fun fetchTeamNameMap(): Map<String, String> = fetchNameMap("teams", "name")

    suspend fun fetchUserNameMap(): Map<String, String> = fetchNameMap("users", "displayName")

    /** Risolve i nomi squadra home/away di [match] tramite la mappa [teamNames], con fallback all'id. */
    private fun teamNamesFor(match: Match?, teamNames: Map<String, String>): Pair<String, String> {
        val homeName = match?.let { teamNames[it.homeTeamId] ?: it.homeTeamId } ?: ""
        val awayName = match?.let { teamNames[it.awayTeamId] ?: it.awayTeamId } ?: ""
        return homeName to awayName
    }

    private suspend fun getMatch(matchId: String): Match? =
        db.collection("matches").document(matchId).get().await()
            .toObject(Match::class.java)?.copy(id = matchId)

    suspend fun fetchFinishedMatchesAtVenue(venueId: String): List<Match> =
        db.collection("matches")
            .whereEqualTo("venueId", venueId)
            .whereEqualTo("status", MatchStatus.FINISHED.raw)
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
            updates["status"] = MatchStatus.ASSIGNED.raw
            updates["refereeApplications"] = emptyList<String>()
        }
        db.collection("matches").document(matchId).update(updates).await()

        val teamNames = fetchTeamNameMap()
        val match = getMatch(matchId)
        val (homeName, awayName) = teamNamesFor(match, teamNames)
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
            updates["status"] = MatchStatus.PENDING.raw
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
        db.collection("matches").document(matchId).update("status", MatchStatus.FINISHED.raw).await()

        val match = getMatch(matchId)
        val teamNames = fetchTeamNameMap()
        val (homeName, awayName) = teamNamesFor(match, teamNames)
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
                // "rejected" non ha un caso MatchStatus corrispondente: confronto/valore
                // stringa intenzionale, vedi model/MatchStatus.kt.
                "status" to "rejected",
                "adminComment" to comment
            )
        ).await()

        val match = getMatch(matchId)
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
                "status" to MatchStatus.WAITING_APPROVAL.raw,
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

    /**
     * Scrive nella sotto-collezione legacy `users/{uid}/notifications`, un percorso diverso
     * da quello letto da [notificationsFlow] (collezione root `notifications`). Comportamento
     * preesistente preservato as-is: fire-and-forget, nessun `.await()`, come nell'originale.
     */
    fun writeLegacyAssignedSubcollectionNotification(refereeUid: String, matchId: String, message: String) {
        val notificationId = java.util.UUID.randomUUID().toString()
        val data = mapOf(
            "id" to notificationId,
            "title" to "Gara Assegnata!",
            "message" to message,
            "type" to NotificationType.ASSIGNED,
            "relatedMatchId" to matchId,
            "read" to false
        )
        try {
            db.collection("users").document(refereeUid).collection("notifications")
                .document(notificationId).set(data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Fire-and-forget come nell'originale: nessun `.await()`, il chiamante non attende la scrittura. */
    fun createVenue(name: String, university: String, address: String, latitude: Double, longitude: Double): Venue {
        val newId = db.collection("venues").document().id
        val venue = Venue(
            id = newId,
            name = name,
            university = university,
            address = address,
            latitude = latitude,
            longitude = longitude
        )
        db.collection("venues").document(newId).set(venue)
        return venue
    }
}
