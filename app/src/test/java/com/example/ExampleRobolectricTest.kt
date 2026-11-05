package com.example

import com.example.data.model.ReportEntity
import com.example.data.model.ReportSeverity
import com.example.data.model.ReportStatus
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun `task entity initial state and completion toggle`() {
    val task = TaskEntity(
      title = "Calibrar sensor",
      priority = TaskPriority.ALTA.name,
      isCompleted = false
    )
    assertFalse(task.isCompleted)
    val completedTask = task.copy(isCompleted = true)
    assertTrue(completedTask.isCompleted)
    assertEquals("Calibrar sensor", completedTask.title)
  }

  @Test
  fun `report entity status transition`() {
    val report = ReportEntity(
      code = "REP-201",
      title = "Fuga detectada",
      description = "Fuga menor en manguera hidráulica",
      severity = ReportSeverity.ALTA.name,
      status = ReportStatus.ABIERTO.name
    )
    assertEquals(ReportStatus.ABIERTO.name, report.status)
    val inProcess = report.copy(status = ReportStatus.EN_PROCESO.name)
    assertEquals("EN_PROCESO", inProcess.status)
    val resolved = inProcess.copy(status = ReportStatus.RESUELTO.name, resolutionNotes = "Manguera cambiada")
    assertEquals("RESUELTO", resolved.status)
    assertEquals("Manguera cambiada", resolved.resolutionNotes)
  }

  @Test
  fun `user entity creation and properties`() {
    val serviciosUser = com.example.data.model.UserEntity(
      username = "servicios",
      fullName = "Mario Gómez",
      password = "123",
      role = com.example.data.model.UserRoles.SERVICIOS_GENERALES,
      email = "servicios@colegio.edu.co"
    )
    assertEquals("servicios", serviciosUser.username)
    assertEquals("Servicios Generales", serviciosUser.role)
    assertEquals("Mario Gómez", serviciosUser.fullName)

    val profesorUser = com.example.data.model.UserEntity(
      username = "profesor",
      fullName = "Prof. Andrés Castro",
      password = "123",
      role = com.example.data.model.UserRoles.PROFESOR,
      avatarId = 5,
      avatarColorHex = "#FF00FF"
    )
    assertEquals("Profesor", profesorUser.role)
    assertEquals(5, profesorUser.avatarId)
    assertEquals("#FF00FF", profesorUser.avatarColorHex)

    val avatarItem = com.example.ui.model.AvatarItem(1, "Especialista IT", R.drawable.avatar_1)
    assertEquals(1, avatarItem.id)
    assertEquals("Especialista IT", avatarItem.nombreRol)
  }
}
