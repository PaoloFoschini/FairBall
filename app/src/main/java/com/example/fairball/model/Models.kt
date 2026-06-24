package com.example.fairball.model

import com.google.firebase.Timestamp
import java.util.*

data class Team(
    val id: String = "",
    val name: String = ""
)

data class Match(
    val id: String = "",
    val code: String = "",
    val season: String = "",
    val status: String = "pending",
    val homeTeamId: String = "",
    val awayTeamId: String = "",
    val refereeId: String? = null,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val scheduledAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
