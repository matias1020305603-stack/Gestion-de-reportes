package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.ui.theme.TechColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val PRIVACY_PDF_ASSET = "politica_privacidad_csr.pdf"

private suspend fun ensureCachedPrivacyPdfFile(context: Context): File = withContext(Dispatchers.IO) {
    val docsDir = File(context.cacheDir, "docs").apply { mkdirs() }
    val outFile = File(docsDir, "Politica_de_Privacidad_CSR.pdf")
    context.assets.open(PRIVACY_PDF_ASSET).use { input ->
        outFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    outFile
}

private suspend fun renderPdfAssetPages(context: Context): List<ImageBitmap> = withContext(Dispatchers.IO) {
    val pdfFile = ensureCachedPrivacyPdfFile(context)
    val pages = mutableListOf<ImageBitmap>()
    val descriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
    descriptor.use { pfd ->
        val renderer = PdfRenderer(pfd)
        renderer.use { pdfRenderer ->
            for (index in 0 until pdfRenderer.pageCount) {
                pdfRenderer.openPage(index).use { page ->
                    val targetWidth = 1240
                    val aspect = page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                    val targetHeight = (targetWidth * aspect).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pages.add(bitmap.asImageBitmap())
                }
            }
        }
    }
    pages
}

private fun openPdfExternally(context: Context) {
    try {
        val docsDir = File(context.cacheDir, "docs").apply { mkdirs() }
        val file = File(docsDir, "Politica_de_Privacidad_CSR.pdf")
        if (!file.exists()) {
            context.assets.open(PRIVACY_PDF_ASSET).use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Abrir Política de Privacidad (PDF)"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "No hay un visor de PDF externo instalado; usa el visor integrado.",
            Toast.LENGTH_SHORT
        ).show()
    } catch (_: Exception) {
        Toast.makeText(
            context,
            "Mostrando el documento en el visor integrado.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
fun PrivacyPolicyPdfDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var renderedPages by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        loadError = null
        try {
            renderedPages = renderPdfAssetPages(context)
        } catch (e: Exception) {
            loadError = e.localizedMessage ?: "No se pudo cargar el documento PDF."
        } finally {
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = TechColors.TechCard,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 8.dp)
                .testTag("privacy_policy_pdf_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TechColors.DeepCharcoal)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Documento PDF",
                            tint = TechColors.NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Política de Privacidad y Gestión de Datos",
                                color = TechColors.TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            Text(
                                text = if (renderedPages.isNotEmpty()) {
                                    "Documento Oficial CSR • ${renderedPages.size} páginas"
                                } else {
                                    "Colegio Seminario Redentorista (PDF)"
                                },
                                color = TechColors.NeonCyan,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { openPdfExternally(context) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Abrir PDF con aplicación externa",
                                tint = TechColors.NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("close_privacy_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar PDF",
                                tint = TechColors.TextSecondary
                            )
                        }
                    }
                }

                // PDF Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF1A1C20))
                ) {
                    when {
                        isLoading -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = TechColors.NeonCyan)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Cargando documento PDF...",
                                    color = TechColors.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        loadError != null -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = loadError ?: "Error cargando PDF",
                                    color = Color(0xFFFF3B30),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { openPdfExternally(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonCyan)
                                ) {
                                    Text("Abrir con visor externo", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(renderedPages) { index, pageBitmap ->
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            shadowElevation = 4.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            Image(
                                                bitmap = pageBitmap,
                                                contentDescription = "Página ${index + 1} de la Política de Privacidad",
                                                contentScale = ContentScale.FillWidth,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Página ${index + 1} de ${renderedPages.size}",
                                            color = TechColors.TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TechColors.DeepCharcoal)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("accept_privacy_pdf_button")
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
}
