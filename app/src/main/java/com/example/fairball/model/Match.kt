package com.example.fairball.model

import com.google.firebase.Timestamp

data class Match(
    val id: String = "",
    val code: String = "",
    val season: String = "",
    val status: String = "pending",
    val homeTeamId: String = "",
    val awayTeamId: String = "",
    val refereeId: String? = null,
    val coRefereeId: String? = null,
    val refereeApplications: List<String> = emptyList(),
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val scheduledAt: Timestamp? = null,
    val assignedAt: Timestamp? = null,
    val venueId: String = "",
    val phase: String = "Regular Season",
    val category: String = "Misto",
    val photoDistintaA: String? = null,
    val photoDistintaB: String? = null,
    val photoReferto: String? = null,
    val adminComment: String? = null
)