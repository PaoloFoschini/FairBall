package com.example.fairball.ui

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 */
fun Timestamp.toFormattedDate(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
    return sdf.format(this.toDate())
}

/**
 *
 */
fun Timestamp.toFormattedTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.ITALY)
    return sdf.format(this.toDate())
}
