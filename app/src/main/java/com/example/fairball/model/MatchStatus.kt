package com.example.fairball.model

/**
 * Stato del ciclo di vita di una partita.
 *
 * IMPORTANTE: [raw] deve rimanere identico ai valori già salvati nel campo
 * "status" su Firestore. Non è un enum "nativo" Firestore proprio per questo:
 * usare toObjects()/set() con un enum Kotlin serializzerebbe il *nome*
 * dell'enum (es. "FINISHED"), diverso dai valori minuscoli già presenti nel
 * database, rompendo la lettura dei documenti esistenti.
 *
 * Il modello [Match] continua quindi a esporre `status: String` per restare
 * compatibile con Firestore; questo enum va usato nel codice per confronti,
 * `when` esaustivi e per evitare refusi nelle stringhe.
 */
enum class MatchStatus(val raw: String) {
    /** Partita creata, nessun arbitro assegnato. */
    PENDING("pending"),

    /** Arbitro assegnato, partita pronta per essere giocata. */
    ASSIGNED("assigned"),

    /** Partita giocata, documenti caricati, in attesa di approvazione admin. */
    WAITING_APPROVAL("waiting_approval"),

    /** Partita approvata dall'admin, risultato pubblico. */
    FINISHED("finished");

    companion object {
        fun fromRaw(raw: String): MatchStatus = entries.find { it.raw == raw } ?: PENDING
    }
}

/** Legge il campo status di [Match] come [MatchStatus] type-safe. */
val Match.statusEnum: MatchStatus
    get() = MatchStatus.fromRaw(status)
