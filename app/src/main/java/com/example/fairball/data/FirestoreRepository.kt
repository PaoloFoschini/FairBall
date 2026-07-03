package com.example.fairball.data

import com.example.fairball.model.Match
import com.example.fairball.model.Team
import com.example.fairball.model.User
import com.example.fairball.model.Venue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

object FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    // ---------- Flows ----------
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

    // ---------- One‑time fetches (per mappe nome) ----------
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

    // ---------- Scritture ----------
    suspend fun createMatch(match: Match): String {
        val docRef = db.collection("matches").document()
        val newMatch = match.copy(id = docRef.id)
        docRef.set(newMatch).await()
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
        }
        db.collection("matches").document(matchId).update(updates).await()
    }

    suspend fun removeReferee(matchId: String, isCoReferee: Boolean = false) {
        val field = if (isCoReferee) "coRefereeId" else "refereeId"
        db.collection("matches").document(matchId).update(mapOf(field to null)).await()
    }

    suspend fun applyForMatch(matchId: String, refereeUid: String) {
        db.collection("matches").document(matchId)
            .update("refereeApplications", FieldValue.arrayUnion(refereeUid)).await()
    }

    suspend fun withdrawApplication(matchId: String, refereeUid: String) {
        db.collection("matches").document(matchId)
            .update("refereeApplications", FieldValue.arrayRemove(refereeUid)).await()
    }

    suspend fun submitMatchReport(matchId: String, photoA: String?, photoB: String?, photoRef: String?) {
        db.collection("matches").document(matchId).update(
            mapOf(
                "photoDistintaA" to photoA,
                "photoDistintaB" to photoB,
                "photoReferto" to photoRef,
                "status" to "waiting_approval"
            )
        ).await()
    }

    suspend fun approveMatch(matchId: String) {
        db.collection("matches").document(matchId).update("status", "finished").await()
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
}