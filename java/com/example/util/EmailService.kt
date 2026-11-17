package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class EmailSendResult {
    data class RealEmailSent(val message: String) : EmailSendResult()
    data class SimulatedSent(val code: String, val message: String) : EmailSendResult()
    data class Error(val message: String) : EmailSendResult()
}

object EmailService {

    private const val CHANNEL_ID = "csr_verification_channel"
    private const val CHANNEL_NAME = "Verificación de Cuentas CSR"

    private const val PREFS_NAME = "csr_email_prefs"
    private const val KEY_WEBHOOK_URL = "custom_gmail_webhook_url"

    const val SAMPLE_APPS_SCRIPT_CODE = """function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    MailApp.sendEmail({
      to: data.to,
      subject: data.subject || ("Código de verificación: " + data.code),
      htmlBody: data.htmlBody,
      body: data.body || ("Tu código es: " + data.code)
    });
    return ContentService.createTextOutput(JSON.stringify({ status: "success" }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", error: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet(e) {
  try {
    var to = e.parameter.to;
    var subject = e.parameter.subject || ("Código de verificación: " + e.parameter.code);
    var body = e.parameter.body || ("Tu código es: " + e.parameter.code);
    var htmlBody = e.parameter.htmlBody;
    MailApp.sendEmail({
      to: to,
      subject: subject,
      body: body,
      htmlBody: htmlBody || undefined
    });
    return ContentService.createTextOutput(JSON.stringify({ status: "success" }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", error: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}"""

    fun getCustomWebhookUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_WEBHOOK_URL, "") ?: ""
        if (saved.isNotBlank()) return saved
        return try {
            val field = BuildConfig::class.java.getField("GMAIL_WEBHOOK_URL")
            field.get(null) as? String ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun saveCustomWebhookUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_WEBHOOK_URL, url.trim()).apply()
    }

    suspend fun testWebhookConnection(context: Context, url: String, testEmail: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!url.startsWith("http")) {
            return@withContext Pair(false, "La URL debe comenzar con https://")
        }
        try {
            val testCode = "999888"
            val html = generateTechVerificationHtml(testEmail, "Usuario Prueba", testCode, "Estudiante")
            val payload = JSONObject().apply {
                put("to", testEmail)
                put("subject", "Prueba de conexión Webhook CSR Tech")
                put("htmlBody", html)
                put("body", "Prueba de Webhook CSR Tech exitosa.")
                put("code", testCode)
            }

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 10000
                readTimeout = 10000
                instanceFollowRedirects = false
                doOutput = true
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use {
                it.write(payload.toString())
                it.flush()
            }

            val code = conn.responseCode
            if (code in 200..399) {
                Pair(true, "¡Conexión exitosa! El correo de prueba fue enviado a $testEmail")
            } else {
                Pair(false, "El servidor respondió con código HTTP $code")
            }
        } catch (e: Exception) {
            Pair(false, "Error al conectar: ${e.message}")
        }
    }

    suspend fun dispatchVerificationEmail(
        context: Context,
        recipientEmail: String,
        recipientName: String,
        code: String,
        role: String
    ): EmailSendResult = withContext(Dispatchers.IO) {

        val emailHtml = generateTechVerificationHtml(
            recipientEmail = recipientEmail,
            recipientName = recipientName,
            code = code,
            role = role
        )

        // 1. Intentar mediante Webhook de Google Apps Script (Configurable desde la app o BuildConfig)
        val webhookUrl = getCustomWebhookUrl(context)

        if (webhookUrl.isNotBlank() && webhookUrl.startsWith("http")) {
            // Intentar primero vía POST con JSON completo (incluye HTML enriquecido)
            try {
                val payload = JSONObject().apply {
                    put("to", recipientEmail)
                    put("subject", "Código de verificación de CSR Tech ($code)")
                    put("htmlBody", emailHtml)
                    put("body", "Código de verificación CSR Tech: $code\nIngresa este código en la aplicación para activar tu cuenta de $role ($recipientName).")
                    put("code", code)
                    put("role", role)
                    put("name", recipientName)
                }

                val conn = (URL(webhookUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    connectTimeout = 10000
                    readTimeout = 10000
                    instanceFollowRedirects = false
                    doOutput = true
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use {
                    it.write(payload.toString())
                    it.flush()
                }

                val resCode = conn.responseCode
                if (resCode in 200..399) {
                    return@withContext EmailSendResult.RealEmailSent(
                        "¡Correo enviado a $recipientEmail desde tu Google Apps Script!"
                    )
                }
            } catch (_: Exception) {}

            // Fallback a GET si el script no configuró doPost
            try {
                val encodedTo = URLEncoder.encode(recipientEmail, "UTF-8")
                val encodedSubject = URLEncoder.encode("Código de verificación CSR Tech: $code", "UTF-8")
                val encodedBody = URLEncoder.encode(
                    "Colegio San Redentorista\n\nTu código de verificación CSR Tech es: $code\n\nIngresa este código en la aplicación para activar tu cuenta de $role ($recipientName).",
                    "UTF-8"
                )
                val targetUrl = "$webhookUrl?to=$encodedTo&subject=$encodedSubject&body=$encodedBody&code=$code"
                val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    instanceFollowRedirects = true
                }
                if (conn.responseCode in 200..399) {
                    return@withContext EmailSendResult.RealEmailSent(
                        "¡Correo enviado a $recipientEmail vía Google Apps Script!"
                    )
                }
            } catch (_: Exception) {}
        }

        // 2. Intentar mediante Resend API con plantilla tecnológica idéntica a Google Verification
        val apiKey = try {
            val field = BuildConfig::class.java.getField("RESEND_API_KEY")
            field.get(null) as? String ?: ""
        } catch (_: Throwable) {
            ""
        }

        if (!apiKey.isNullOrBlank() && apiKey.startsWith("re_")) {
            try {
                val url = URL("https://api.resend.com/emails")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                }

                val emailHtml = generateTechVerificationHtml(
                    recipientEmail = recipientEmail,
                    recipientName = recipientName,
                    code = code,
                    role = role
                )

                val payload = JSONObject().apply {
                    put("from", "CSR Tech Reportes <onboarding@resend.dev>")
                    put("to", JSONArray().put(recipientEmail))
                    put("subject", "Código de verificación de CSR Tech ($code)")
                    put("html", emailHtml)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    return@withContext EmailSendResult.RealEmailSent(
                        "¡Correo tecnológico enviado exitosamente a $recipientEmail! Revisa tu bandeja de entrada o spam en Gmail."
                    )
                }
            } catch (_: Exception) {}
        }

        // 3. Flujo directo: Disparar notificación push al sistema Android (Heads-up notification)
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Canal de notificaciones de verificación institucional CSR"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("Gmail • Código de verificación ($recipientEmail)")
                .setContentText("Tu código de verificación CSR Tech es: $code")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "Colegio San Redentorista\n\nTu código de verificación institucional para $recipientName ($role) es:\n\n$code\n\nEscribe este código de 6 dígitos en la aplicación para activar tu cuenta."
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(101, notification)
        } catch (_: Exception) {}

        EmailSendResult.SimulatedSent(
            code = code,
            message = "¡Notificación enviada a tu dispositivo con el código de verificación para $recipientEmail!"
        )
    }

    /**
     * Genera la plantilla HTML con el estilo idéntico al correo de verificación de Google,
     * pero personalizado con diseño tecnológico / Cyberpunk (Cian Neón, fondos oscuros, insignia de candado).
     */
    fun generateTechVerificationHtml(
        recipientEmail: String,
        recipientName: String,
        code: String,
        role: String
    ): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="margin:0;padding:24px 12px;background-color:#0A0A0A;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
          <div style="max-width:560px;margin:0 auto;background:#141414;border-radius:18px;border:1px solid #00FFFF;overflow:hidden;box-shadow:0 0 30px rgba(0,255,255,0.25);">
            
            <!-- Barra Superior: Logo e Icono Tecnológico -->
            <div style="padding:22px 28px;background:#101010;border-bottom:1px solid #222222;">
              <table style="width:100%;border-collapse:collapse;">
                <tr>
                  <td style="vertical-align:middle;">
                    <span style="font-size:20px;font-weight:900;color:#00FFFF;letter-spacing:1px;">⚡ CSR TECH</span>
                    <span style="font-size:11px;color:#888888;margin-left:8px;font-weight:bold;">SISTEMA ESCOLAR</span>
                  </td>
                  <td style="text-align:right;vertical-align:middle;">
                    <div style="display:inline-block;width:34px;height:34px;background:#00FFFF;border-radius:50%;text-align:center;line-height:34px;color:#000000;font-size:16px;">
                      🔒
                    </div>
                  </td>
                </tr>
              </table>
            </div>

            <!-- Banner Azul / Cian Neón (como el de la imagen de Google) -->
            <div style="background:linear-gradient(135deg, #0056D2 0%, #00FFFF 100%);padding:24px 28px;">
              <h1 style="margin:0;color:#000000;font-size:22px;font-weight:800;letter-spacing:0.3px;">
                Código de verificación de CSR Tech
              </h1>
              <p style="margin:4px 0 0 0;color:#002244;font-size:12px;font-weight:700;">
                Seguridad de Cuentas Institucionales • Colegio San Redentorista
              </p>
            </div>

            <!-- Contenido del Correo -->
            <div style="padding:28px;color:#D8D8D8;font-size:14px;line-height:1.6;">
              <p style="margin-top:0;color:#CCCCCC;">
                El Colegio San Redentorista recibió una solicitud para usar esta dirección de correo electrónico como método de registro para la cuenta institucional de <strong style="color:#00FFFF;">$recipientEmail</strong> ($role: $recipientName).
              </p>

              <!-- Bloque del Código (Destacado y centrado exactamente como la imagen) -->
              <div style="text-align:center;margin:32px 0;padding:20px;background:#0D0D0D;border-radius:14px;border:1.5px dashed #00FFFF;box-shadow:inset 0 0 15px rgba(0,255,255,0.1);">
                <div style="font-size:11px;color:#888888;text-transform:uppercase;letter-spacing:2px;margin-bottom:8px;font-weight:bold;">
                  Código de Activación Institucional
                </div>
                <div style="font-size:42px;font-weight:900;letter-spacing:8px;color:#00FFFF;font-family:monospace;">
                  $code
                </div>
              </div>

              <p style="color:#CCCCCC;font-size:13px;">
                Cuando se te solicite en la aplicación, ingresa el código para demostrar que el colegio puede comunicarse contigo a través de esta dirección de correo electrónico.
              </p>

              <p style="color:#888888;font-size:12px;margin-top:20px;">
                Si no reconoces <em>$recipientEmail</em>, es probable que alguien haya dado tu dirección de correo electrónico por error. Puedes ignorar este mensaje con seguridad.
              </p>

              <div style="margin-top:28px;padding-top:16px;border-top:1px solid #262626;color:#AAAAAA;font-size:13px;">
                <strong>Atentamente,</strong><br>
                El equipo de Cuentas y Tecnología CSR
              </div>
            </div>

            <!-- Footer Informativo -->
            <div style="padding:16px 28px;background:#0D0D0D;color:#666666;font-size:11px;line-height:1.4;border-top:1px solid #1C1C1C;">
              No se pueden enviar respuestas a esta dirección de correo electrónico.<br>
              © Colegio San Redentorista • 2026 Plataforma de Gestión de Reportes
            </div>
          </div>
        </body>
        </html>
        """.trimIndent()
    }
}
