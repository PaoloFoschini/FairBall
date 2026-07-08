package com.example.fairball.ui

/**
 * Stato di sessione globale.
 * Viene valorizzato una sola volta, al login.
 */
object Session {
    var uid: String? = null
    var role: String? = null

    fun clear() {
        uid = null
        role = null
    }
}