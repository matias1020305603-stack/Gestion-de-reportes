package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfileDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "Estudiante",
    val avatarId: Int = 1,
    val avatarColorHex: String = "#00FFFF",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReportDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val code: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val category: String = "General",
    val severity: String = "MEDIA",
    val status: String = "ABIERTO",
    val authorName: String = "",
    val authorRole: String = "Estudiante",
    val authorEmail: String = "",
    val assignedTo: String? = null,
    val resolutionNotes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
