package com.example.fairball.model

/**
 * Ruolo di un utente. Vedi il commento in [MatchStatus] per il motivo per
 * cui [User.role] resta una String a livello di modello/Firestore: questo
 * enum è lo strato type-safe da usare nel codice.
 */
enum class UserRole(val raw: String) {
    ADMIN("admin"),
    REFEREE("referee");

    companion object {
        fun fromRaw(raw: String): UserRole = entries.find { it.raw == raw } ?: REFEREE
    }
}

/** Legge il campo role di [User] come [UserRole] type-safe. */
val User.roleEnum: UserRole
    get() = UserRole.fromRaw(role)
