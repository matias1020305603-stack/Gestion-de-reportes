package com.example.data.model

data class SocialComment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val authorName: String,
    val authorRole: String,
    val avatarId: Int,
    val avatarColorHex: String,
    val text: String,
    val timestamp: String = "Hace un momento"
)

data class SocialPost(
    val id: String = java.util.UUID.randomUUID().toString(),
    val authorName: String,
    val authorRole: String,
    val avatarId: Int,
    val avatarColorHex: String,
    val content: String,
    val imageUrl: String? = null,
    val timestamp: String = "Hace un momento",
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val comments: List<SocialComment> = emptyList()
)
