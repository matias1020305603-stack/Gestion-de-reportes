package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.TechColors
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allReports by viewModel.allReports.collectAsStateWithLifecycle()
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Usuarios, 1: Reportes

    var passwordDialogUser by remember { mutableStateOf<com.example.data.model.UserEntity?>(null) }
    var newPasswordInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TechColors.DeepCharcoal)
            .padding(16.dp)
    ) {
        Text(
            text = "PANEL DE CONTROL DE ADMINISTRADOR",
            color = TechColors.NeonCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = "Control total: Gestión de usuarios, contraseñas, eliminación de cuentas y regulación de reportes.",
            color = TechColors.TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { selectedSubTab = 0 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSubTab == 0) TechColors.NeonCyan else TechColors.TechCard
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Usuarios (${allUsers.size})",
                    fontSize = 12.sp,
                    color = if (selectedSubTab == 0) TechColors.DeepCharcoal else TechColors.TextPrimary
                )
            }
            Button(
                onClick = { selectedSubTab = 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSubTab == 1) TechColors.NeonCyan else TechColors.TechCard
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Reportes (${allReports.size})",
                    fontSize = 12.sp,
                    color = if (selectedSubTab == 1) TechColors.DeepCharcoal else TechColors.TextPrimary
                )
            }
            Button(
                onClick = { selectedSubTab = 2 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSubTab == 2) TechColors.NeonCyan else TechColors.TechCard
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Soluciones",
                    fontSize = 12.sp,
                    color = if (selectedSubTab == 2) TechColors.DeepCharcoal else TechColors.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedSubTab == 0) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allUsers) { user ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TechColors.TechCard),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${user.fullName} (@${user.username})",
                                        color = TechColors.TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Rol: ${user.role} | Email: ${user.email.ifBlank { "N/A" }}",
                                        color = TechColors.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Row {
                                    IconButton(onClick = {
                                        passwordDialogUser = user
                                        newPasswordInput = ""
                                    }) {
                                        Icon(Icons.Default.Key, contentDescription = "Cambiar Clave", tint = TechColors.NeonCyan)
                                    }
                                    if (user.role != com.example.data.model.UserRoles.ADMINISTRADOR) {
                                        IconButton(onClick = {
                                            viewModel.adminDeleteUser(user.id)
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar Cuenta", tint = TechColors.NeonMagenta)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedSubTab == 1) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allReports) { report ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TechColors.TechCard),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${report.code}: ${report.title}",
                                        color = TechColors.TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Reportado por: ${report.reportedBy} | Estado: ${report.status} ${if (report.assignedTo.isNotBlank()) "| Atendido por: ${report.assignedTo}" else ""}",
                                        color = TechColors.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel.adminDeleteReport(report.id)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Borrar Reporte", tint = TechColors.NeonMagenta)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val resolvedReports = allReports.filter { it.status == com.example.data.model.ReportStatus.RESUELTO.name }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(resolvedReports) { report ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TechColors.TechCard),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${report.code}: ${report.title}",
                                color = TechColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ubicación: ${report.location}",
                                color = TechColors.TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Solucionado por: ${report.assignedTo.ifBlank { "Personal Autorizado" }}",
                                color = TechColors.NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            if (report.resolutionNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Notas: ${report.resolutionNotes}",
                                    color = TechColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (passwordDialogUser != null) {
        AlertDialog(
            onDismissRequest = { passwordDialogUser = null },
            title = { Text("Cambiar Contraseña de ${passwordDialogUser?.fullName}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("Nueva Contraseña") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    passwordDialogUser?.let { u ->
                        if (newPasswordInput.isNotBlank()) {
                            viewModel.adminUpdateUserPassword(u.id, newPasswordInput.trim())
                            passwordDialogUser = null
                        }
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { passwordDialogUser = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
