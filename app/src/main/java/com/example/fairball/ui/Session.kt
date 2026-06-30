package com.example.fairball.ui

/**
 * Stato di sessione globale e leggero.
 *
 * Necessario perché in modalità "Debug" (vedi LoginScreen) l'utente NON
 * effettua un vero login su FirebaseAuth, quindi `FirebaseAuth.currentUser`
 * resta null. Senza questo oggetto, ProfileScreen non sarebbe in grado di
 * capire "chi sono io" e tratterebbe il proprio profilo come quello di
 * un altro arbitro.
 *
 * Viene valorizzato una sola volta, al login (vedi MainActivity.onLoginSuccess).
 */
object Session {
    var uid: String? = null
    var role: String? = null

    fun clear() {
        uid = null
        role = null
    }
}