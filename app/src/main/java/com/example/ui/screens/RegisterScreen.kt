package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserRoles
import com.example.ui.components.GlowingButton
import com.example.ui.components.TechTextField
import com.example.ui.theme.TechColors
import com.example.ui.viewmodel.MainViewModel
import com.example.util.EmailService
import com.example.util.UsernameValidator
import kotlinx.coroutines.launch

enum class RegisterStep {
    STEP1_CREDENTIALS,
    STEP2_USERNAME
}

@Composable
fun RegisterScreen(
    viewModel: MainViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* Permiso procesado */ }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Step state
    var currentStep by remember { mutableStateOf(RegisterStep.STEP1_CREDENTIALS) }

    // Role state: strictly Estudiante or Profesor (Servicios Generales is not allowed to register)
    var selectedRole by remember { mutableStateOf(UserRoles.ESTUDIANTE) }

    // Step 1 Fields
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Email verification state
    var generatedSecretCode by remember { mutableStateOf<String?>(null) }
    var enteredCode by remember { mutableStateOf("") }
    var isSendingEmail by remember { mutableStateOf(false) }
    var isEmailVerified by remember { mutableStateOf(false) }
    var showIncomingEmailModal by remember { mutableStateOf(false) }
    var showWebhookSetupDialog by remember { mutableStateOf(false) }
    var webhookUrlInput by remember { mutableStateOf(EmailService.getCustomWebhookUrl(context)) }
    var isTestingWebhook by remember { mutableStateOf(false) }
    var webhookTestMessage by remember { mutableStateOf<String?>(null) }

    // Step 2 Fields: Username with strict profanity & sensual restrictions
    var username by remember { mutableStateOf("") }

    // Errors
    var errorMessage by remember { mutableStateOf("") }

    val usernameValidation = remember(username) {
        if (username.isBlank()) null
        else UsernameValidator.validate(username)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TechColors.DeepCharcoal)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // Header
        Icon(
            imageVector = Icons.Default.AppRegistration,
            contentDescription = "Registro Institucional",
            tint = TechColors.NeonCyan,
            modifier = Modifier.size(54.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "REGISTRO INSTITUCIONAL",
            style = TextStyle(
                color = TechColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            )
        )

        Text(
            text = "Exclusivo para Profesores y Estudiantes",
            style = TextStyle(
                color = TechColors.NeonCyan,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TechColors.TechCard)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StepIndicatorItem(
                stepNumber = "1",
                label = "Correo y Clave",
                isActive = currentStep == RegisterStep.STEP1_CREDENTIALS,
                isCompleted = currentStep == RegisterStep.STEP2_USERNAME
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TechColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            StepIndicatorItem(
                stepNumber = "2",
                label = "Nombre de Usuario",
                isActive = currentStep == RegisterStep.STEP2_USERNAME,
                isCompleted = false
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error Banner
        if (errorMessage.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TechColors.TechCard),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFF3B30))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF3B30))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF3B30),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // ========================================================
        // PASO 1: CORREO, ENVÍO DE CÓDIGO Y CONTRASEÑAS
        // ========================================================
        if (currentStep == RegisterStep.STEP1_CREDENTIALS) {

            // 1. Selector de Rol
            Text(
                text = "1. Selecciona tu rol en el colegio:",
                style = TextStyle(
                    color = TechColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RoleSelectionCard(
                    title = "Estudiante",
                    icon = Icons.Default.School,
                    description = "Reportar daños en aulas y campus",
                    isSelected = selectedRole == UserRoles.ESTUDIANTE,
                    onClick = {
                        selectedRole = UserRoles.ESTUDIANTE
                        errorMessage = ""
                    },
                    modifier = Modifier.weight(1f)
                )

                RoleSelectionCard(
                    title = "Profesor",
                    icon = Icons.Default.Person,
                    description = "Reportar laboratorios e incidencias",
                    isSelected = selectedRole == UserRoles.PROFESOR,
                    onClick = {
                        selectedRole = UserRoles.PROFESOR
                        errorMessage = ""
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta informativa de Servicios Generales
            Card(
                colors = CardDefaults.cardColors(containerColor = TechColors.TechCard),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(TechColors.NeonMagenta.copy(alpha = 0.5f))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = TechColors.NeonMagenta,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aviso para Servicios Generales",
                            color = TechColors.NeonMagenta,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "El personal de Servicios Generales NO requiere registrarse. Su cuenta ya fue creada por la administración institucional.",
                            color = TechColors.TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ir a Iniciar Sesión →",
                            color = TechColors.NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToLogin() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "2. Ingresa tu correo y solicita tu código de activación:",
                style = TextStyle(
                    color = TechColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Nombre Completo
            TechTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    errorMessage = ""
                },
                label = "Nombre Completo",
                icon = Icons.Default.Badge,
                placeholder = "Ej. Santiago Gómez",
                modifier = Modifier.testTag("reg_fullname_field")
            )

            // Correo Electrónico
            TechTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = ""
                    if (isEmailVerified) {
                        isEmailVerified = false
                        generatedSecretCode = null
                        enteredCode = ""
                    }
                },
                label = "Correo Electrónico (Gmail o Institucional)",
                icon = Icons.Default.Email,
                placeholder = "matias.1020305603@colredentorista.edu.co",
                enabled = !isEmailVerified,
                trailingIcon = {
                    if (isEmailVerified) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verificado",
                            tint = Color(0xFF34C759)
                        )
                    }
                },
                modifier = Modifier.testTag("reg_email_field")
            )

            // Panel de Verificación de Correo
            if (!isEmailVerified) {
                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val trimmedEmail = email.trim()
                        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
                            errorMessage = "Por favor ingresa un correo electrónico válido"
                            return@Button
                        }
                        if (fullName.isBlank()) {
                            errorMessage = "Por favor ingresa tu nombre completo antes de solicitar el código"
                            return@Button
                        }

                        errorMessage = ""
                        isSendingEmail = true

                        coroutineScope.launch {
                            // Generar código numérico limpio de 6 dígitos exactamente como la imagen de ejemplo
                            val randomCode = (100000..999999).random().toString()
                            generatedSecretCode = randomCode

                            // Despachar el correo mediante el servicio
                            val result = EmailService.dispatchVerificationEmail(
                                context = context,
                                recipientEmail = trimmedEmail,
                                recipientName = fullName.trim(),
                                code = randomCode,
                                role = selectedRole
                            )

                            isSendingEmail = false
                            when (result) {
                                is com.example.util.EmailSendResult.RealEmailSent -> {
                                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                    showIncomingEmailModal = false
                                }
                                is com.example.util.EmailSendResult.SimulatedSent -> {
                                    showIncomingEmailModal = true
                                    Toast.makeText(context, "Código preparado para $trimmedEmail", Toast.LENGTH_SHORT).show()
                                }
                                is com.example.util.EmailSendResult.Error -> {
                                    errorMessage = result.message
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSendingEmail) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviando código a tu correo...", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (generatedSecretCode == null) "Enviar Código de Verificación al Correo"
                                   else "Reenviar Código al Correo",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Aviso de entrega por notificación push directa
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = TechColors.NeonCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Recibirás una notificación instantánea con tu código de 6 dígitos",
                        color = TechColors.TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Sección obligatoria para ESCRIBIR el código que llegó a la notificación
                if (generatedSecretCode != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = TechColors.TechCard),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(TechColors.NeonCyan.copy(alpha = 0.6f))
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = TechColors.NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Notificación Despachada",
                                    color = TechColors.NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Hemos enviado una notificación a tu dispositivo con el código para $email. Revisa la barra superior o toca abajo para ver el correo:",
                                color = TechColors.TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Enlace para volver a ver el correo si cerró la notificación
                            Text(
                                text = "👉 Toca aquí para ver la carta de verificación",
                                color = TechColors.NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { showIncomingEmailModal = true }
                                    .padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Campo para que el usuario escriba obligatoriamente el código
                            TechTextField(
                                value = enteredCode,
                                onValueChange = {
                                    enteredCode = it.trim().uppercase()
                                    errorMessage = ""
                                },
                                label = "Código de 6 dígitos (ej. 656971)",
                                icon = Icons.Default.Key,
                                placeholder = "656971",
                                modifier = Modifier.testTag("reg_code_input_field")
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    val secret = generatedSecretCode?.trim()
                                    val typed = enteredCode.trim()

                                    if (typed.isBlank()) {
                                        errorMessage = "Debes escribir el código numérico de 6 dígitos que recibiste"
                                    } else if (secret != null && (typed.equals(secret, ignoreCase = true) || typed.equals("CSR-$secret", ignoreCase = true))) {
                                        isEmailVerified = true
                                        errorMessage = ""
                                        Toast.makeText(context, "¡Código verificado con éxito!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        errorMessage = "El código ingresado ('$typed') no coincide. Revisa el correo y vuelve a escribirlo."
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verificar Código", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                // Tarjeta de confirmación: Correo verificado con éxito
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF34C759).copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Correo verificado con éxito: $email",
                            color = Color(0xFF34C759),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Contraseña y Confirmar Contraseña (Separadas del Correo)
            Text(
                text = "3. Crea tu contraseña para la aplicación:",
                style = TextStyle(
                    color = TechColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.align(Alignment.Start)
            )

            Text(
                text = "Nota: Es una contraseña para tu cuenta de la app (NO uses la de tu correo personal).",
                color = TechColors.TextSecondary.copy(alpha = 0.8f),
                fontSize = 10.sp,
                lineHeight = 13.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            TechTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = ""
                },
                label = "Contraseña para la App",
                icon = Icons.Default.Lock,
                isPassword = !passwordVisible,
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Ver contraseña",
                            tint = TechColors.TextSecondary
                        )
                    }
                },
                modifier = Modifier.testTag("reg_pass_field")
            )

            TechTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = ""
                },
                label = "Confirmar Contraseña",
                icon = Icons.Default.LockReset,
                isPassword = !passwordVisible,
                isError = confirmPassword.isNotBlank() && confirmPassword != password,
                trailingIcon = {
                    if (confirmPassword.isNotBlank()) {
                        if (confirmPassword == password) {
                            Icon(Icons.Default.Check, contentDescription = "Coinciden", tint = Color(0xFF34C759))
                        } else {
                            Icon(Icons.Default.Close, contentDescription = "No coinciden", tint = Color(0xFFFF3B30))
                        }
                    }
                },
                modifier = Modifier.testTag("reg_confirm_pass_field")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Botón para avanzar al Paso 2: Elección de Nombre de Usuario
            GlowingButton(
                text = "CONTINUAR: ELEGIR USUARIO →",
                onClick = {
                    when {
                        fullName.isBlank() -> {
                            errorMessage = "Por favor ingresa tu nombre completo"
                        }
                        email.isBlank() || !email.contains("@") -> {
                            errorMessage = "Por favor ingresa un correo electrónico válido"
                        }
                        !isEmailVerified -> {
                            errorMessage = "Debes solicitar y escribir el código de verificación para continuar"
                        }
                        password.isBlank() || password.length < 3 -> {
                            errorMessage = "La contraseña debe tener mínimo 3 caracteres"
                        }
                        password != confirmPassword -> {
                            errorMessage = "Las contraseñas no coinciden"
                        }
                        else -> {
                            errorMessage = ""
                            if (username.isBlank()) {
                                val cleanFirst = fullName.trim().split(" ").firstOrNull()?.lowercase() ?: "usuario"
                                val roleTag = if (selectedRole == UserRoles.PROFESOR) "prof" else "est"
                                username = "${roleTag}.${cleanFirst}"
                            }
                            currentStep = RegisterStep.STEP2_USERNAME
                        }
                    }
                },
                modifier = Modifier.testTag("reg_step1_next_btn")
            )

        } else {
            // ========================================================
            // PASO 2: ELECCIÓN DE NOMBRE DE USUARIO CON FILTRO DE
            // NOMBRES SENSUALES Y MALEDUCADOS
            // ========================================================

            Card(
                colors = CardDefaults.cardColors(containerColor = TechColors.TechCard),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(TechColors.NeonCyan)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AlternateEmail,
                            contentDescription = null,
                            tint = TechColors.NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ELIGE TU NOMBRE DE USUARIO",
                            color = TechColors.TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Este será tu identificador público en el colegio (u/nombre). Por normativa escolar y convivencia, NO se admiten nombres sensuales, eróticos, provocativos o maleducados.",
                        color = TechColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo para el nombre de usuario
            TechTextField(
                value = username,
                onValueChange = {
                    username = it.trim().lowercase()
                    errorMessage = ""
                },
                label = "Nombre de Usuario (ej. u/carlos.mora)",
                icon = Icons.Default.AccountCircle,
                placeholder = "u/tu.nombre",
                isError = usernameValidation != null && !usernameValidation.isValid,
                trailingIcon = {
                    if (usernameValidation != null) {
                        if (usernameValidation.isValid) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Válido", tint = Color(0xFF34C759))
                        } else {
                            Icon(Icons.Default.Cancel, contentDescription = "Restringido", tint = Color(0xFFFF3B30))
                        }
                    }
                },
                modifier = Modifier.testTag("reg_username_field")
            )

            // Tarjeta de Validación en Tiempo Real
            if (usernameValidation != null) {
                if (usernameValidation.isValid) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF34C759).copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nombre respetuoso y permitido: u/$username",
                                color = Color(0xFF34C759),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF3B30).copy(alpha = 0.15f)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFF3B30))
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = usernameValidation.message ?: "Nombre de usuario no permitido",
                                color = Color(0xFFFF3B30),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Resumen de la cuenta
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TechColors.TechCard)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Datos de Registro:",
                        color = TechColors.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "• Rol: $selectedRole", color = TechColors.NeonCyan, fontSize = 12.sp)
                    Text(text = "• Nombre: $fullName", color = TechColors.TextPrimary, fontSize = 12.sp)
                    Text(text = "• Correo verificado: $email", color = TechColors.TextPrimary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón Final de Registro
            GlowingButton(
                text = "REGISTRARME COMO ${selectedRole.uppercase()}",
                onClick = {
                    val validation = UsernameValidator.validate(username)
                    if (!validation.isValid) {
                        errorMessage = validation.message ?: "Nombre de usuario no válido"
                        return@GlowingButton
                    }

                    viewModel.register(
                        username = username.trim().lowercase(),
                        fullName = fullName.trim(),
                        password = password.trim(),
                        role = selectedRole,
                        email = email.trim()
                    ) { success, msg ->
                        if (success) {
                            Toast.makeText(context, "¡Registro completado con éxito!", Toast.LENGTH_SHORT).show()
                            onRegisterSuccess()
                        } else {
                            errorMessage = msg
                        }
                    }
                },
                modifier = Modifier.testTag("reg_submit_btn")
            )

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = { currentStep = RegisterStep.STEP1_CREDENTIALS }
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = TechColors.TextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Volver al Paso 1", color = TechColors.TextSecondary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Volver a Iniciar Sesión
        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.testTag("go_to_login_btn")
        ) {
            Text(
                text = "¿Ya tienes cuenta o eres de Servicios Generales? Inicia Sesión",
                color = TechColors.TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    // ========================================================
    // MODAL DE BANDEJA DE CORREO RECIBIDO (SIMULACIÓN REAL GMAIL)
    // ========================================================
    // ========================================================
    // MODAL DE BANDEJA DE CORREO RECIBIDO (RÉPLICA EXACTA DE LA IMAGEN GMAIL)
    // ========================================================
    if (showIncomingEmailModal && generatedSecretCode != null) {
        Dialog(onDismissRequest = { showIncomingEmailModal = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(TechColors.NeonCyan)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Barra Superior Estilo Gmail (Icono M rojo + Título)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF202124)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Mail,
                                    contentDescription = "Gmail",
                                    tint = Color(0xFFEA4335), // Rojo característico de Gmail
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Código de verificación de CSR Tech",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "1 mensaje • Bandeja de entrada",
                                    color = Color(0xFF9AA0A6),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { showIncomingEmailModal = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color(0xFF9AA0A6)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF2D3135), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Metadatos del remitente y fecha (idéntico al encabezado del correo)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "CSR Tech ",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "<noreply@colredentorista.edu.co>",
                                    color = Color(0xFF9AA0A6),
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "Para: $email",
                                color = Color(0xFF9AA0A6),
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = "Hoy, hace un momento",
                            color = Color(0xFF80868B),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. TARJETA PRINCIPAL DEL CORREO (RÉPLICA EXACTA DE LA IMAGEN ADJUNTA)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2C2C34))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Cabecera interna: Logo tecnológico a la izquierda, insignia de candado a la derecha
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "CSR",
                                        color = TechColors.NeonCyan,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = " Tech",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }

                                // Círculo azul con candado/cerradura idéntico a la imagen
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color(0xFF1A73E8)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Banner azul/cian vibrante idéntico al banner azul de la imagen de Google
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            listOf(Color(0xFF1A73E8), Color(0xFF00B4D8))
                                        )
                                    )
                                    .padding(horizontal = 20.dp, vertical = 18.dp)
                            ) {
                                Text(
                                    text = "Código de verificación de CSR Tech",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    lineHeight = 22.sp
                                )
                            }

                            // Cuerpo del mensaje
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "El Colegio San Redentorista recibió una solicitud para usar esta dirección de correo electrónico como método de registro para la cuenta institucional de $selectedRole ($fullName).",
                                    color = Color(0xFFD2D3D5),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(22.dp))

                                // CÓDIGO DE 6 DÍGITOS DESTACADO Y CENTRADO EXACTO COMO EN LA IMAGEN (ej: 656971)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF111113))
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = generatedSecretCode ?: "",
                                        color = TechColors.NeonCyan,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 32.sp,
                                        letterSpacing = 6.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "Cuando se te solicite, ingresa el código para demostrar que el colegio puede comunicarse contigo a través de esta dirección de correo electrónico.",
                                    color = Color(0xFFD2D3D5),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Si no reconoces $email, es probable que alguien haya dado tu dirección de correo electrónico por error. Puedes ignorar este mensaje.",
                                    color = Color(0xFF9AA0A6),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "Atentamente,\nEl equipo de Cuentas y Tecnología CSR",
                                    color = Color(0xFFD2D3D5),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Pie de página del correo (disclaimer)
                    Text(
                        text = "No se pueden enviar respuestas a esta dirección de correo electrónico. Para obtener más información, consulta con el Administrador de Sistemas CSR.\n© CSR Tech Inc., Colegio San Redentorista, Colombia",
                        color = Color(0xFF5F6368),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 5. Botón de acción para cerrar y escribir en la app
                    Button(
                        onClick = { showIncomingEmailModal = false },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Entendido (Escribir código en el registro)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    // ========================================================
    // MODAL DE CONFIGURACIÓN DEL WEBHOOK DE GOOGLE APPS SCRIPT
    // ========================================================
    if (showWebhookSetupDialog) {
        Dialog(onDismissRequest = { showWebhookSetupDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(TechColors.NeonCyan)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TechColors.NeonCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = TechColors.NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Conectar Gmail Real",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Vía Google Apps Script (Gratis)",
                                    color = TechColors.NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        IconButton(
                            onClick = { showWebhookSetupDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF9AA0A6))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF2D3135))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Con este webhook, cada código de verificación se enviará directamente a tu bandeja de Gmail usando tu propia cuenta de Google.",
                        color = Color(0xFFD2D3D5),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tarjeta de pasos
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Instrucciones en 4 pasos rápidos:",
                                color = TechColors.NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "1. Abre script.google.com en tu navegador con tu cuenta de Google.\n" +
                                        "2. Crea un 'Nuevo proyecto', borra lo que haya y pega el script de abajo.\n" +
                                        "3. Haz clic en 'Implementar' > 'Nueva implementación'. Elige tipo 'Aplicación web', ejecuta como 'Yo' y en 'Quién tiene acceso' selecciona 'Cualquier usuario'.\n" +
                                        "4. Haz clic en 'Implementar', autoriza los permisos y copia la URL que termina en /exec.",
                                color = Color(0xFFB0B3B8),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón para copiar el script al portapapeles
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(EmailService.SAMPLE_APPS_SCRIPT_CODE))
                            Toast.makeText(context, "¡Script de Google Apps Script copiado al portapapeles!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2B30)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TechColors.NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copiar Código de Google Apps Script",
                            color = TechColors.NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo de entrada de la URL
                    TechTextField(
                        value = webhookUrlInput,
                        onValueChange = {
                            webhookUrlInput = it
                            webhookTestMessage = null
                        },
                        label = "Pega aquí tu URL Web de Google Apps Script",
                        placeholder = "https://script.google.com/macros/s/.../exec",
                        icon = Icons.Default.Link,
                        modifier = Modifier.testTag("webhook_url_field")
                    )

                    if (webhookTestMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (webhookTestMessage?.contains("exitos", ignoreCase = true) == true) Color(0xFF34C759).copy(alpha = 0.15f)
                                else Color(0xFFFF3B30).copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = webhookTestMessage ?: "",
                                color = if (webhookTestMessage?.contains("exitos", ignoreCase = true) == true) Color(0xFF34C759) else Color(0xFFFF453A),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón Probar y Guardar
                    Button(
                        onClick = {
                            val trimmedUrl = webhookUrlInput.trim()
                            if (trimmedUrl.isBlank() || !trimmedUrl.startsWith("http")) {
                                webhookTestMessage = "Por favor ingresa una URL válida (ej. https://script.google.com/macros/s/.../exec)"
                                return@Button
                            }

                            isTestingWebhook = true
                            webhookTestMessage = null
                            val targetEmail = if (email.isNotBlank()) email.trim() else "matias.1020305603@colredentorista.edu.co"

                            coroutineScope.launch {
                                val (ok, msg) = EmailService.testWebhookConnection(context, trimmedUrl, targetEmail)
                                isTestingWebhook = false
                                if (ok) {
                                    EmailService.saveCustomWebhookUrl(context, trimmedUrl)
                                    webhookTestMessage = "✅ ¡Conexión exitosa! Revisa $targetEmail en Gmail. Se ha enviado un correo con el código de prueba."
                                    Toast.makeText(context, "Webhook guardado correctamente", Toast.LENGTH_SHORT).show()
                                } else {
                                    webhookTestMessage = "❌ $msg. Asegúrate de haber seleccionado 'Cualquier usuario' en la implementación de Google Apps Script."
                                }
                            }
                        },
                        enabled = !isTestingWebhook,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        if (isTestingWebhook) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Probando y conectando con Gmail...", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar y Probar Envío a Gmail", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (EmailService.getCustomWebhookUrl(context).isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = {
                                EmailService.saveCustomWebhookUrl(context, "")
                                webhookUrlInput = ""
                                webhookTestMessage = "Se ha desconectado el webhook personalizado."
                                Toast.makeText(context, "Webhook eliminado", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Desconectar Webhook", color = Color(0xFFFF453A), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicatorItem(
    stepNumber: String,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when {
                        isCompleted -> Color(0xFF34C759)
                        isActive -> TechColors.NeonCyan
                        else -> TechColors.DeepCharcoal
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            } else {
                Text(
                    text = stepNumber,
                    color = if (isActive) Color.Black else TechColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (isActive) TechColors.NeonCyan else TechColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun RoleSelectionCard(
    title: String,
    icon: ImageVector,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TechColors.NeonCyan else TechColors.TechCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) TechColors.DeepCharcoal else TechColors.NeonCyan,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) TechColors.DeepCharcoal else TechColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 10.sp,
                color = if (isSelected) TechColors.DeepCharcoal.copy(alpha = 0.8f) else TechColors.TextSecondary,
                lineHeight = 12.sp,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}
