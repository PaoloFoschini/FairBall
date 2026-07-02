package com.example.fairball.model

/**
 * Rappresenta un impianto/palestra dove si possono giocare le partite.
 * Salvato nella collection "venues" di Firestore.
 */
data class Venue(
    val id: String = "",
    val name: String = "",
    val university: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
