package com.example.fairball.model

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val recipientUid: String = "",
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val relatedMatchId: String? = null,
    val read: Boolean = false,
    val createdAt: Timestamp? = null
)

object NotificationType {
    const val NEW_MATCH = "new_match"
    const val REFEREE_REQUEST = "referee_request"
    const val APPROVAL_REQUEST = "approval_request"
    const val ASSIGNED = "assigned"
    const val RESULT_PUBLISHED = "result_published"
    const val RESULT_REJECTED = "result_rejected"
}