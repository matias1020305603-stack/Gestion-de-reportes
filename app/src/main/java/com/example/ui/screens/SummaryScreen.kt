package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ReportSeverity
import com.example.data.model.ReportStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SummaryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allReports by viewModel.allReports.collectAsStateWithLifecycle()

    val openReports = allReports.count { it.status == ReportStatus.ABIERTO.name }
    val inProcessReports = allReports.count { it.status == ReportStatus.EN_PROCESO.name }
    val resolvedReports = allReports.count { it.status == ReportStatus.RESUELTO.name }
    val totalReports = allReports.size

    val resolutionRate = if (totalReports > 0) resolvedReports.toFloat() / totalReports else 0f
    val animatedResolutionRate by animateFloatAsState(targetValue = resolutionRate, label = "resolutionRate")

    val criticalCount = allReports.count { it.severity == ReportSeverity.CRITICA.name && it.status != ReportStatus.RESUELTO.name }
    val highCount = allReports.count { it.severity == ReportSeverity.ALTA.name && it.status != ReportStatus.RESUELTO.name }

    val summaryText = viewModel.generateExecutiveSummary()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tasa de Resolución de Reportes
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Efectividad de Atención",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Porcentaje de incidencias resueltas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${(resolutionRate * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = StatusResuelto
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress = { animatedResolutionRate },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = StatusResuelto,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricMiniStat(
                        count = totalReports,
                        label = "Total Reportes",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Divider(modifier = Modifier.height(30.dp).width(1.dp))
                    MetricMiniStat(
                        count = openReports,
                        label = "Sin Iniciar",
                        color = StatusAbierto
                    )
                    Divider(modifier = Modifier.height(30.dp).width(1.dp))
                    MetricMiniStat(
                        count = inProcessReports,
                        label = "En Proceso",
                        color = StatusEnProceso
                    )
                    Divider(modifier = Modifier.height(30.dp).width(1.dp))
                    MetricMiniStat(
                        count = resolvedReports,
                        label = "Resueltos",
                        color = StatusResuelto
                    )
                }
            }
        }

        // Estado y Severidad de Daños
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Estado de Daños e Incidencias",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusIndicatorBox(
                        title = "Abiertos",
                        count = openReports,
                        color = StatusAbierto,
                        modifier = Modifier.weight(1f)
                    )
                    StatusIndicatorBox(
                        title = "En Proceso",
                        count = inProcessReports,
                        color = StatusEnProceso,
                        modifier = Modifier.weight(1f)
                    )
                    StatusIndicatorBox(
                        title = "Resueltos",
                        count = resolvedReports,
                        color = StatusResuelto,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (criticalCount > 0 || highCount > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = SeverityCritica.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = SeverityCritica,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Prioridad inmediata para Servicios Generales: $criticalCount críticas y $highCount altas pendientes de resolución.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = SeverityCritica
                            )
                        }
                    }
                }
            }
        }

        // Informe Institucional para Compartir
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Informe Institucional de Reportes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Genera un consolidado de novedades y daños reportados en el colegio para enviar a directivos o archivar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp)
                ) {
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Informe Institucional de Reportes", summaryText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Informe copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_summary_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copiar")
                    }

                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, summaryText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Enviar Informe Institucional")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_summary_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniStat(
    count: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatusIndicatorBox(
    title: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
