package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.TechColors

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    var showFullPdf by remember { mutableStateOf(false) }

    if (showFullPdf) {
        PrivacyPolicyPdfDialog(onDismiss = { showFullPdf = false })
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = TechColors.TechCard),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .padding(vertical = 12.dp)
                .testTag("privacy_policy_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PrivacyTip,
                        contentDescription = null,
                        tint = TechColors.NeonCyan,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Política de Privacidad y Gestión de Datos",
                            color = TechColors.TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Colegio Seminario Redentorista",
                            color = TechColors.NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = TechColors.DeepCharcoal, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PrivacySectionItem(
                        icon = Icons.Default.CameraAlt,
                        title = "1. Acceso a Cámara y Galería",
                        body = "La aplicación solicita permiso para usar la cámara del dispositivo y acceder al selector seguro de galería (Photo Picker) únicamente cuando decides capturar o adjuntar una fotografía como evidencia en un reporte escolar o en el muro comunitario. Nunca capturamos fotos en segundo plano ni exploramos otros archivos personales de tu teléfono."
                    )

                    PrivacySectionItem(
                        icon = Icons.Default.Shield,
                        title = "2. Moderación y Restricción de Imágenes",
                        body = "Para proteger el entorno escolar, toda fotografía adjuntada es analizada automáticamente antes de guardarse o publicarse mediante inteligencia artificial de seguridad (Firebase AI Logic). Está estrictamente prohibido subir imágenes sensuales, eróticas, provocativas, de desnudez o contenido inapropiado; dichas imágenes serán bloqueadas automáticamente."
                    )

                    PrivacySectionItem(
                        icon = Icons.Default.Groups,
                        title = "3. Privacidad de Grupos en el Chat",
                        body = "Los mensajes directos están separados por grupo institucional: los Estudiantes solo pueden agregar y chatear con Estudiantes, los Profesores solo con Profesores, y Servicios Generales solo con Servicios Generales. Los mensajes son verificados contra lenguaje vulgar o sensual."
                    )

                    PrivacySectionItem(
                        icon = Icons.Default.Storage,
                        title = "4. Uso y Resguardo de Datos",
                        body = "Tu nombre, correo institucional, rol y las evidencias cargadas se utilizan exclusivamente para gestionar reportes de infraestructura, mantenimiento y convivencia dentro de la institución educativa."
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showFullPdf = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_official_privacy_pdf_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = TechColors.NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ver Documento Oficial (PDF)",
                        color = TechColors.NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("privacy_policy_accept_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonCyan)
                ) {
                    Text(
                        text = "Entendido",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacySectionItem(
    icon: ImageVector,
    title: String,
    body: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = TechColors.DeepCharcoal,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TechColors.NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = TechColors.NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                color = TechColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}
