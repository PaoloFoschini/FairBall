package com.example.fairball.ui

import com.example.fairball.model.Team

/** Nome della squadra con [id], con fallback all'id stesso se non trovata. */
fun List<Team>.nameOf(id: String): String = find { it.id == id }?.name ?: id

/** Valore associato a [id] nella mappa, con fallback all'id stesso se assente. */
fun Map<String, String>.nameOf(id: String): String = this[id] ?: id
