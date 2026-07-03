package com.example.fairball.data

/**
 * Configurazione globale dell'app.
 *
 * SUPERADMIN_UID identifica l'account che nessun altro amministratore può modificare
 * o eliminare dall'interfaccia di gestione utenti (protezione solo lato client/UI,
 * come richiesto — non sostituisce eventuali regole di sicurezza lato server).
 *
 * Sostituisci il valore con il tuo uid Firebase (lo trovi in Firestore, nel documento
 * della collection "users" corrispondente al tuo account, oppure in Firebase Authentication
 * > Users, colonna "User UID").
 */
object AppConfig {
    const val SUPERADMIN_UID = "5TgfuRpKrweOzCgco9RPrD2hKMd2"
}