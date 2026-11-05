package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskPriority(val label: String, val level: Int) {
    ALTA("Alta", 3),
    MEDIA("Media", 2),
    BAJA("Baja", 1)
}

enum class TaskCategory(val label: String) {
    OPERACION("Operación"),
    MANTENIMIENTO("Mantenimiento"),
    ADMINISTRACION("Administración"),
    SEGURIDAD("Seguridad"),
    GENERAL("General")
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: String = TaskPriority.MEDIA.name,
    val category: String = TaskCategory.GENERAL.name,
    val dueDate: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
