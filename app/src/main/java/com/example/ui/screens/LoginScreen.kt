package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import com.example.R
import com.example.data.model.UserRoles
import com.example.ui.components.GlowingButton
import com.example.ui.components.TechTextField
import com.example.ui.theme.TechColors
import com.example.ui.viewmodel.MainViewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedRole by remember { mutableStateOf(UserRoles.PROFESOR) }
    var emailOrUser by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotInput by remember { mutableStateOf("") }
    var forgotStatusMessage by remember { mutableStateOf<String?>(null) }
    var isResettingPassword by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TechColors.DeepCharcoal)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // Icono tecnológico
        Icon(
            imageVector = Icons.Default.Memory,
            contentDescription = "Logo Tecnológico",
            tint = TechColors.NeonCyan,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "INICIAR SESIÓN",
            style = TextStyle(
                color = TechColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )
        )

        Text(
            text = "Plataforma Institucional de Reportes de Daños",
            style = TextStyle(
                color = TechColors.NeonCyan,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Selector de tipo de usuario: Profesor, Servicios Generales o Administrador
        Text(
            text = "Selecciona tu perfil de acceso:",
            style = TextStyle(
                color = TechColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RoleSelectorButton(
                title = "Profesor",
                icon = Icons.Default.Person,
                isSelected = selectedRole == UserRoles.PROFESOR,
                onClick = {
                    selectedRole = UserRoles.PROFESOR
                    errorMessage = ""
                },
                modifier = Modifier.weight(1f)
            )

            RoleSelectorButton(
                title = "Serv. Gen.",
                icon = Icons.Default.CleaningServices,
                isSelected = selectedRole == UserRoles.SERVICIOS_GENERALES,
                onClick = {
                    selectedRole = UserRoles.SERVICIOS_GENERALES
                    errorMessage = ""
                },
                modifier = Modifier.weight(1f)
            )

            RoleSelectorButton(
                title = "Admin",
                icon = Icons.Default.AdminPanelSettings,
                isSelected = selectedRole == UserRoles.ADMINISTRADOR,
                onClick = {
                    selectedRole = UserRoles.ADMINISTRADOR
                    errorMessage = ""
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Banner informativo especial para Servicios Generales o Administrador
        if (selectedRole == UserRoles.SERVICIOS_GENERALES) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = TechColors.TechCard
                ),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(TechColors.NeonCyan)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = TechColors.NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Acceso Especial: Servicios Generales",
                            color = TechColors.NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Ya estás registrado en el sistema. Ingresa tu usuario y contraseña institucional.",
                            color = TechColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else if (selectedRole == UserRoles.ADMINISTRADOR) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = TechColors.TechCard
                ),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(TechColors.NeonMagenta)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = TechColors.NeonMagenta,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Acceso Exclusivo: Administrador General",
                            color = TechColors.NeonMagenta,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Control total del sistema. Contraseña maestra requerida (3 intentos fallidos = 3 días de baneo temporal).",
                            color = TechColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        if (errorMessage.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = TechColors.TechCard
                ),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(TechColors.NeonMagenta)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = TechColors.NeonMagenta,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (selectedRole != UserRoles.ADMINISTRADOR) {
            TechTextField(
                value = emailOrUser,
                onValueChange = {
                    emailOrUser = it
                    errorMessage = ""
                },
                label = if (selectedRole == UserRoles.SERVICIOS_GENERALES) "Usuario de Servicios Generales" else "Usuario o Correo Institucional",
                icon = if (selectedRole == UserRoles.SERVICIOS_GENERALES) Icons.Default.CleaningServices else Icons.Default.Email,
                modifier = Modifier.testTag("login_email_field")
            )
        }

        TechTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = ""
            },
            label = if (selectedRole == UserRoles.ADMINISTRADOR) "Contraseña Maestra de Administrador" else "Contraseña",
            icon = Icons.Default.Lock,
            isPassword = true,
            modifier = Modifier.testTag("login_password_field")
        )

        Spacer(modifier = Modifier.height(24.dp))

        GlowingButton(
            text = if (selectedRole == UserRoles.ADMINISTRADOR) "INGRESAR COMO ADMINISTRADOR" else "ENTRAR COMO ${selectedRole.uppercase()}",
            onClick = {
                if (selectedRole == UserRoles.ADMINISTRADOR) {
                    val prefs = context.getSharedPreferences("admin_security", android.content.Context.MODE_PRIVATE)
                    val banUntil = prefs.getLong("ban_until", 0L)
                    val currentTime = System.currentTimeMillis()
                    if (currentTime < banUntil) {
                        val remainingHours = (banUntil - currentTime) / (1000 * 60 * 60)
                        errorMessage = "ACCESO BLOQUEADO: Baneo temporal de 3 días activo ($remainingHours horas restantes)."
                        return@GlowingButton
                    }
                    if (password.trim() != "AdminMaster2026*") {
                        val failed = prefs.getInt("failed_attempts", 0) + 1
                        if (failed >= 3) {
                            val threeDaysLater = currentTime + 3L * 24 * 60 * 60 * 1000
                            prefs.edit().putLong("ban_until", threeDaysLater).putInt("failed_attempts", 0).apply()
                            errorMessage = "¡3 intentos fallidos! Baneo temporal de 3 días aplicado al Administrador."
                        } else {
                            prefs.edit().putInt("failed_attempts", failed).apply()
                            errorMessage = "Contraseña de Administrador incorrecta. Intentos restantes antes del baneo temporal: ${3 - failed}"
                        }
                        return@GlowingButton
                    } else {
                        prefs.edit().putInt("failed_attempts", 0).putLong("ban_until", 0L).apply()
                        viewModel.login("admin", password, expectedRole = UserRoles.ADMINISTRADOR) { success, msg ->
                            if (success) {
                                onLoginSuccess()
                            } else {
                                errorMessage = msg
                            }
                        }
                    }
                } else {
                    if (emailOrUser.isBlank() || password.isBlank()) {
                        errorMessage = "Ingresa usuario y contraseña"
                    } else {
                        viewModel.login(emailOrUser, password, expectedRole = selectedRole) { success, msg ->
                            if (success) {
                                onLoginSuccess()
                            } else {
                                errorMessage = msg
                            }
                        }
                    }
                }
            },
            modifier = Modifier.testTag("login_submit_btn")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Botón de Google Sign-In para sincronización con Firebase Cloud
        GoogleSignInButton(
            onAuthSuccess = {
                val fbUser = Firebase.auth.currentUser
                if (fbUser != null) {
                    val displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "Usuario Google"
                    viewModel.loginAsGoogleUser(
                        uid = fbUser.uid,
                        name = displayName,
                        email = fbUser.email ?: "",
                        role = selectedRole
                    )
                    Toast.makeText(context, "¡Conectado con Google Firebase!", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                }
            },
            onAuthError = { err ->
                errorMessage = err
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = {
            showForgotPasswordDialog = true
        }) {
            Text("¿Olvidaste tu contraseña?", color = TechColors.NeonMagenta)
        }

        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = {
                    showForgotPasswordDialog = false
                    forgotStatusMessage = null
                    forgotInput = ""
                },
                title = { Text("Recuperar Contraseña Institucional") },
                text = {
                    Column {
                        Text(
                            text = "Ingresa tu usuario o correo institucional (Administrador, Servicios Generales o Profesor). Te enviaremos un correo con una contraseña temporal de restablecimiento.",
                            fontSize = 13.sp,
                            color = TechColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = forgotInput,
                            onValueChange = { forgotInput = it },
                            label = { Text("Usuario o Correo") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (forgotStatusMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = forgotStatusMessage!!,
                                color = TechColors.NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isResettingPassword = true
                            viewModel.requestPasswordReset(forgotInput) { success, msg ->
                                isResettingPassword = false
                                forgotStatusMessage = msg
                                if (success) {
                                    Toast.makeText(context, "¡Correo de recuperación enviado!", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isResettingPassword && forgotInput.isNotBlank()
                    ) {
                        if (isResettingPassword) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        } else {
                            Text("Enviar Correo")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showForgotPasswordDialog = false
                        forgotStatusMessage = null
                        forgotInput = ""
                    }) {
                        Text("Cerrar")
                    }
                }
            )
        }

        TextButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.testTag("go_to_register_btn")
        ) {
            Text(
                text = "¿Eres nuevo Profesor? Regístrate aquí",
                color = TechColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Divider(color = TechColors.TechCard, thickness = 1.dp)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Acceso Directo con Cuentas de Prueba:",
            style = TextStyle(color = TechColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.quickLoginAs(UserRoles.PROFESOR)
                    onLoginSuccess()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TechColors.NeonCyan)
            ) {
                Text("Profesor", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    viewModel.quickLoginAs(UserRoles.SERVICIOS_GENERALES)
                    onLoginSuccess()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TechColors.NeonCyan)
            ) {
                Text("Servicios", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun RoleSelectorButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TechColors.NeonCyan else TechColors.TechCard
        ),
        border = if (!isSelected) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(TechColors.TechCard)
        ) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) TechColors.DeepCharcoal else TechColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) TechColors.DeepCharcoal else TechColors.TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun GoogleSignInButton(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = {
            isLoading = true
            val clientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                isLoading = false
                onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
                return@OutlinedButton
            }

            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

            coroutineScope.launch {
                try {
                    val result = credentialManager.getCredential(context as Activity, request)
                    val credential = result.credential
                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                        Firebase.auth.signInWithCredential(authCredential).await()
                        isLoading = false
                        onAuthSuccess()
                    } else {
                        isLoading = false
                        onAuthError("Credencial de Google no reconocida")
                    }
                } catch (e: GetCredentialCancellationException) {
                    isLoading = false
                    android.util.Log.w("Auth", "Google Sign-In cancelado: ${e.message}")
                } catch (e: Exception) {
                    isLoading = false
                    onAuthError(e.localizedMessage ?: "Fallo al iniciar sesión con Google")
                }
            }
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF1E1E22),
            contentColor = Color.White
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(TechColors.NeonCyan)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("google_sign_in_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = TechColors.NeonCyan,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Conectando con Google...", color = Color.White, fontSize = 12.sp)
        } else {
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = TechColors.NeonCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Iniciar Sesión con Google (Firebase)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

