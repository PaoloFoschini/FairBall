package com.example.fairball.model

/**
 * Vedi [UserRole] per i valori ammessi del campo [role].
 */
data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = UserRole.REFEREE.raw,
    val photoUrl: String? = null
)
