package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ReportEntity
import com.example.data.model.ReportSeverity
import com.example.data.model.ReportStatus
import com.example.ui.components.ReportCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    onSelectReport: (ReportEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val reports by viewModel.filteredReports.collectAsStateWithLifecycle()
    val allReports by viewModel.allReports.collectAsStateWithLifecycle()
    val searchQuery by viewModel.reportSearchQuery.collectAsStateWithLifecycle()
    val filterStatus by viewModel.reportFilterStatus.collectAsStateWithLifecycle()
    val filterSeverity by viewModel.reportFilterSeverity.collectAsStateWithLifecycle()
    val isCompactView by viewModel.isCompactView.collectAsStateWithLifecycle()

    var showClearConfirmation by remember { mutableStateOf(false) }

    val openCount = allReports.count { it.status == ReportStatus.ABIERTO.name }
    val inProcessCount = allReports.count { it.status == ReportStatus.EN_PROCESO.name }
    val resolvedCount = allReports.count { it.status == ReportStatus.RESUELTO.name }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Status KPI Counters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ReportStatusMetricCard(
                label = "Abiertos",
                count = openCount,
                isSelected = filterStatus == ReportStatus.ABIERTO.name,
                accentColor = StatusAbierto,
                onClick = { viewModel.setReportFilterStatus(ReportStatus.ABIERTO.name) },
                modifier = Modifier.weight(1f)
            )

            ReportStatusMetricCard(
                label = "En Proceso",
                count = inProcessCount,
                isSelected = filterStatus == ReportStatus.EN_PROCESO.name,
                accentColor = StatusEnProceso,
                onClick = { viewModel.setReportFilterStatus(ReportStatus.EN_PROCESO.name) },
                modifier = Modifier.weight(1f)
            )

            ReportStatusMetricCard(
                label = "Resueltos",
                count = resolvedCount,
                isSelected = filterStatus == ReportStatus.RESUELTO.name,
                accentColor = StatusResuelto,
                onClick = { viewModel.setReportFilterStatus(ReportStatus.RESUELTO.name) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar with optional Clear All action
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setReportSearchQuery(it) },
                placeholder = { Text("Buscar reporte, código o área...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = if (searchQuery.isNotBlank()) {
                    {
                        IconButton(onClick = { viewModel.setReportSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("report_search_field")
            )

            if (allReports.isNotEmpty()) {
                IconButton(
                    onClick = { showClearConfirmation = true },
                    modifier = Modifier.testTag("clear_reports_button")
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Limpiar todos los reportes",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Severity Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = filterSeverity == null,
                    onClick = { viewModel.setReportFilterSeverity(null) },
                    label = { Text("Todas") }
                )
            }
            item {
                FilterChip(
                    selected = filterSeverity == ReportSeverity.CRITICA.name,
                    onClick = { viewModel.setReportFilterSeverity(ReportSeverity.CRITICA.name) },
                    label = { Text("Crítica") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SeverityCritica.copy(alpha = 0.2f),
                        selectedLabelColor = SeverityCritica
                    )
                )
            }
            item {
                FilterChip(
                    selected = filterSeverity == ReportSeverity.ALTA.name,
                    onClick = { viewModel.setReportFilterSeverity(ReportSeverity.ALTA.name) },
                    label = { Text("Alta") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SeverityAlta.copy(alpha = 0.2f),
                        selectedLabelColor = SeverityAlta
                    )
                )
            }
            item {
                FilterChip(
                    selected = filterSeverity == ReportSeverity.MEDIA.name,
                    onClick = { viewModel.setReportFilterSeverity(ReportSeverity.MEDIA.name) },
                    label = { Text("Media") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SeverityMedia.copy(alpha = 0.2f),
                        selectedLabelColor = SeverityMedia
                    )
                )
            }
            item {
                FilterChip(
                    selected = filterSeverity == ReportSeverity.BAJA.name,
                    onClick = { viewModel.setReportFilterSeverity(ReportSeverity.BAJA.name) },
                    label = { Text("Baja") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SeverityBaja.copy(alpha = 0.2f),
                        selectedLabelColor = SeverityBaja
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Report List or Empty State
        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "No se encontraron reportes con ese criterio"
                               else "No hay reportes de daños o incidencias registrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Toca el botón '+' para registrar una nueva incidencia o reporte",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(if (isCompactView) 6.dp else 10.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(reports, key = { it.id }) { report ->
                    ReportCard(
                        report = report,
                        onClick = { onSelectReport(report) },
                        isCompact = isCompactView
                    )
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Vaciar lista de reportes") },
            text = { Text("¿Deseas eliminar todos los reportes e incidencias registrados? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllReports()
                        showClearConfirmation = false
                    }
                ) {
                    Text("Vaciar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ReportStatusMetricCard(
    label: String,
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(accentColor)
        ) else null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
