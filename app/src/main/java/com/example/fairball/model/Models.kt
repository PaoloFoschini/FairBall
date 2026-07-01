package com.example.fairball.model

import com.google.firebase.Timestamp

data class Team(
    val id: String = "",
    val name: String = ""
)

/**
 * Statuses:
 * - "pending": Match created, no referee assigned.
 * - "assigned": Referee assigned, match ready to be played.
 * - "waiting_approval": Match played, documents uploaded, waiting for Admin approval.
 * - "finished": Match approved by Admin, results public.
 */
data class Match(
    val id: String = "",
    val code: String = "",
    val season: String = "",
    val status: String = "pending",
    val homeTeamId: String = "",
    val awayTeamId: String = "",
    val refereeId: String? = null,
    val coRefereeId: String? = null,
    val refereeApplications: List<String> = emptyList(), // List of referee UIDs who applied
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val scheduledAt: Timestamp? = null,
    val assignedAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val venueId: String = "",
    val phase: String = "Regular Season",
    val category: String = "Misto",
    val photoDistintaA: String? = null,
    val photoDistintaB: String? = null,
    val photoReferto: String? = null
)

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "referee",
    val photoUrl: String? = null
)
