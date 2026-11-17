package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReportSeverity(val label: String) {
    CRITICA("Crítica"),
    ALTA("Alta"),
    MEDIA("Media"),
    BAJA("Baja")
}

enum class ReportStatus(val label: String) {
    ABIERTO("Abierto"),
    EN_PROCESO("En Proceso"),
    RESUELTO("Resuelto"),
    CERRADO("Cerrado")
}

enum class ReportCategory(val label: String) {
    INCIDENCIA("Incidencia"),
    AVANCE("Avance"),
    MANTENIMIENTO("Mantenimiento"),
    SEGURIDAD("Seguridad"),
    AUDITORIA("Auditoría")
}

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val title: String,
    val description: String,
    val category: String = ReportCategory.INCIDENCIA.name,
    val severity: String = ReportSeverity.MEDIA.name,
    val status: String = ReportStatus.ABIERTO.name,
    val location: String = "Planta Principal",
    val reportedBy: String = "Operador",
    val resolutionNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
