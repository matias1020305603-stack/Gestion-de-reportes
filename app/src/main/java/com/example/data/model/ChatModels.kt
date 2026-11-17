package com.example.data.model

data class FriendContact(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val role: String, // Estudiante, Profesor, Serv. Generales
    val avatarId: Int,
    val avatarColorHex: String,
    val lastMessage: String = "¡Hola! ¿Cómo va el reporte?",
    val isOnline: Boolean = true
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderId: String,
    val text: String,
    val timestamp: String = "Hace un momento",
    val isFromMe: Boolean
)
