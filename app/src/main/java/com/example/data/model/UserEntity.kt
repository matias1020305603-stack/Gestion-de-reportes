package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

object UserRoles {
    const val PROFESOR = "Profesor"
    const val SERVICIOS_GENERALES = "Servicios Generales"
    const val ADMINISTRADOR = "Administrador"
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val fullName: String,
    val password: String,
    val role: String = UserRoles.PROFESOR, // Profesor, Servicios Generales, Administrador
    val email: String = "",
    val avatarId: Int = 1,
    val avatarColorHex: String = "#00FFFF",
    val createdAt: Long = System.currentTimeMillis()
)
