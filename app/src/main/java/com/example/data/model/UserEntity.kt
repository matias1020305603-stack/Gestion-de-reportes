package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

object UserRoles {
    const val PROFESOR = "Profesor"
    const val ESTUDIANTE = "Estudiante"
    const val SERVICIOS_GENERALES = "Servicios Generales"
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val fullName: String,
    val password: String,
    val role: String = UserRoles.ESTUDIANTE, // Profesor, Estudiante, Servicios Generales
    val email: String = "",
    val avatarId: Int = 1,
    val avatarColorHex: String = "#00FFFF",
    val createdAt: Long = System.currentTimeMillis()
)
