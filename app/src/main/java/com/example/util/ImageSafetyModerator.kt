package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.max

private const val TAG = "ImageSafetyModerator"

sealed interface ImageModerationResult {
    data object Idle : ImageModerationResult
    data object Checking : ImageModerationResult
    data object Approved : ImageModerationResult
    data class Rejected(val reason: String) : ImageModerationResult
    data class Error(val message: String) : ImageModerationResult
}

object ImageSafetyModerator {

    private val moderationSchema = Schema.obj(
        properties = mapOf(
            "isAppropriate" to Schema.boolean(
                description = "true ONLY if the photo is completely safe and appropriate for a school campus report or educational community; false if it depicts nudity, sensual or sexually suggestive poses, provocative clothing/lingerie/swimwear, erotica, pornography, graphic violence, self-harm, hate symbols, or inappropriate explicit gestures."
            ),
            "category" to Schema.string(
                description = "Short classification: safe, sensual, nudity, suggestive, violence, or offensive"
            ),
            "reason" to Schema.string(
                description = "Clear explanation in Spanish for the student/teacher/staff why the image was approved or blocked."
            )
        )
    )

    private val moderationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = moderationSchema
            }
        )
    }

    suspend fun moderateImageUri(context: Context, uri: Uri): ImageModerationResult {
        return try {
            val bitmap = withContext(Dispatchers.IO) {
                decodeScaledBitmap(context, uri, maxDimension = 1024)
            } ?: return ImageModerationResult.Error(
                "No se pudo leer la imagen seleccionada. Intenta con otra fotografía."
            )

            val prompt = """
                Eres el moderador de seguridad visual de una aplicación escolar (Colegio Seminario Redentorista)
                usada para reportar daños de infraestructura y compartir novedades institucionales.
                Analiza esta imagen con criterio estricto de entorno educativo:
                - RECHAZA (isAppropriate = false) cualquier imagen con contenido sensual, erótico, sexualmente sugestivo,
                  poses provocativas, desnudez parcial o total, ropa interior/lencería, contenido para adultos (NSFW),
                  violencia explícita o gestos/símbolos ofensivos.
                - APRUEBA (isAppropriate = true) fotos normales de instalaciones, salones, daños, objetos, personas vestidas
                  de forma adecuada y evidencias escolares cotidianas.
                Responde estrictamente según el esquema JSON configurado.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                moderationModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
            }

            val jsonString = response.text
                ?: return ImageModerationResult.Error("El verificador de seguridad no devolvió respuesta.")

            val json = JSONObject(jsonString)
            val isAppropriate = json.getBoolean("isAppropriate")
            val category = json.optString("category", "safe")
            val reason = json.optString("reason", "")

            if (isAppropriate) {
                ImageModerationResult.Approved
            } else {
                val cleanReason = reason.ifBlank {
                    "La imagen fue bloqueada ($category) por contener material sensual o no apto para el entorno escolar."
                }
                ImageModerationResult.Rejected(cleanReason)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image moderation timed out", e)
            ImageModerationResult.Error(
                "La verificación de seguridad tardó demasiado (60s). Verifica tu conexión e inténtalo nuevamente."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image moderation failed", e)
            ImageModerationResult.Error(
                e.localizedMessage ?: "No se pudo completar la verificación de seguridad de la imagen."
            )
        }
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val resolver = context.contentResolver
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight
        if (origWidth <= 0 || origHeight <= 0) {
            return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }

        var sampleSize = 1
        while (origWidth / sampleSize > maxDimension * 2 || origHeight / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val largestSide = max(decoded.width, decoded.height)
        if (largestSide <= maxDimension) {
            return decoded
        }

        val scale = maxDimension.toFloat() / largestSide.toFloat()
        val targetW = max(1, (decoded.width * scale).toInt())
        val targetH = max(1, (decoded.height * scale).toInt())
        val scaled = Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
        if (scaled !== decoded) {
            decoded.recycle()
        }
        return scaled
    }
}
