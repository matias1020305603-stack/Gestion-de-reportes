package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.ui.theme.TechColors
import com.example.util.ImageModerationResult
import java.io.File

fun createCameraCaptureUri(context: Context): Uri {
    val capturesDir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
    val imageFile = File.createTempFile("cam_${System.currentTimeMillis()}_", ".jpg", capturesDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}

@Composable
fun rememberCameraAndGalleryLaunchers(
    onImageCapturedOrPicked: (Uri) -> Unit,
    onPermissionDenied: (String) -> Unit
): Pair<() -> Unit, () -> Unit> {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onImageCapturedOrPicked(uri)
            }
        }
    )

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            val capturedUri = pendingCameraUri
            if (success && capturedUri != null) {
                onImageCapturedOrPicked(capturedUri)
            }
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val uri = createCameraCaptureUri(context)
                pendingCameraUri = uri
                takePictureLauncher.launch(uri)
            } else {
                onPermissionDenied("Permiso de cámara denegado. Puedes otorgarlo en Ajustes o elegir una imagen desde la galería.")
            }
        }
    )

    val launchCamera: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = createCameraCaptureUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val launchGallery: () -> Unit = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    return launchCamera to launchGallery
}

@Composable
fun ImageSourceChooserDialog(
    onDismiss: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenPrivacyTerms: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = TechColors.TechCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("image_source_chooser_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Añadir Evidencia Fotográfica",
                    color = TechColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Elige si deseas tomar una fotografía ahora con la cámara o seleccionarla desde tu galería.",
                    color = TechColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onDismiss()
                        onOpenCamera()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("choose_camera_button")
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Abrir Cámara", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenGallery()
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TechColors.NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("choose_gallery_button")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TechColors.NeonCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Abrir Galería", color = TechColors.NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TechColors.DeepCharcoal,
                    onClick = onOpenPrivacyTerms,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = TechColors.NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Las imágenes son verificadas automáticamente (sin contenido sensual o explícito). Ver Términos de Privacidad.",
                            color = TechColors.TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancelar", color = TechColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
fun ImageModerationStatusBanner(
    state: ImageModerationResult,
    modifier: Modifier = Modifier
) {
    when (state) {
        is ImageModerationResult.Idle -> Unit
        is ImageModerationResult.Checking -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TechColors.NeonCyan.copy(alpha = 0.12f),
                modifier = modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = TechColors.NeonCyan,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Verificando seguridad de la imagen con IA...",
                        color = TechColors.NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        is ImageModerationResult.Approved -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF34C759).copy(alpha = 0.14f),
                modifier = modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Imagen aprobada: apta para el entorno escolar.",
                        color = Color(0xFF34C759),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        is ImageModerationResult.Rejected -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFF3B30).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFFFF3B30)),
                modifier = modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Imagen bloqueada: ${state.reason}",
                        color = Color(0xFFFF3B30),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        is ImageModerationResult.Error -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFF9500).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFFFF9500)),
                modifier = modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = null,
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.message,
                        color = Color(0xFFFF9500),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
