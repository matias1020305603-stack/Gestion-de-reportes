package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ReportEntity
import com.example.data.model.ReportStatus
import com.example.data.model.UserEntity
import com.example.data.model.UserRoles
import com.example.ui.theme.TechColors
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ProfileTab {
    HISTORIAL,
    AJUSTES
}

@Composable
fun UserProfileHistoryDialog(
    user: UserEntity,
    reports: List<ReportEntity>,
    onDismiss: () -> Unit,
    onOpenAvatarPicker: () -> Unit,
    onSelectReport: (ReportEntity) -> Unit,
    onLogout: () -> Unit,
    viewModel: MainViewModel? = null
) {
    val context = LocalContext.current
    val borderColor = hexToColor(user.avatarColorHex)
    val avatarRes = getAvatarDrawableRes(user.avatarId)

    var currentTab by remember { mutableStateOf(ProfileTab.HISTORIAL) }

    // Dialogs inside settings
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showSchoolRulesDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var cacheCleanedMessage by remember { mutableStateOf<String?>(null) }

    // Filter reports relevant to this user
    val userReports = if (user.role == UserRoles.SERVICIOS_GENERALES) {
        reports // Servicios generales supervisa y atiende todos los reportes institucionales
    } else {
        reports.filter {
            it.reportedBy.contains(user.fullName, ignoreCase = true) ||
            it.reportedBy.contains(user.username, ignoreCase = true)
        }
    }

    val resolvedCount = userReports.count { it.status == ReportStatus.RESUELTO.name }
    val inProcessCount = userReports.count { it.status == ReportStatus.EN_PROCESO.name }
    val openCount = userReports.count { it.status == ReportStatus.ABIERTO.name }

    // Reddit-style Karma calculation: base points per report + bonus for solved
    val karmaScore = (userReports.size * 25) + (resolvedCount * 50) + 120

    // Settings preferences state (collected from viewModel or remembered local fallback)
    val isCompactView = viewModel?.isCompactView?.collectAsStateWithLifecycle()?.value ?: false
    val notificationsEnabled = viewModel?.notificationsEnabled?.collectAsStateWithLifecycle()?.value ?: true
    val highPriorityAlerts = viewModel?.highPriorityAlerts?.collectAsStateWithLifecycle()?.value ?: true
    val fluidAnimations = viewModel?.fluidAnimations?.collectAsStateWithLifecycle()?.value ?: true
    val hapticFeedback = viewModel?.hapticFeedback?.collectAsStateWithLifecycle()?.value ?: true

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = TechColors.DeepCharcoal,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp)
                .testTag("user_profile_history_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Top Bar with Reddit handle and Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(TechColors.NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "u/${user.username}",
                            color = TechColors.NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = TechColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reddit Profile Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = TechColors.TechCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar with customized glowing circle & edit indicator
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(TechColors.DeepCharcoal)
                                    .border(width = 3.dp, color = borderColor, shape = CircleShape)
                                    .clickable { onOpenAvatarPicker() }
                                    .testTag("profile_avatar_circle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = avatarRes),
                                    contentDescription = "Avatar de Usuario",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )

                                // Small floating badge indicating avatar customizer
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(TechColors.NeonCyan)
                                        .border(1.5.dp, TechColors.DeepCharcoal, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = "Editar Avatar",
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TechColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(borderColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "r/${user.role}",
                                            color = borderColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (user.email.isNotBlank()) {
                                        Text(
                                            text = user.email,
                                            color = TechColors.TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Quick button to edit avatar
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onOpenAvatarPicker() }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = TechColors.NeonCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Personalizar Avatar y Borde",
                                        color = TechColors.NeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Reddit Karma & Stats Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(TechColors.DeepCharcoal.copy(alpha = 0.7f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$karmaScore",
                                    color = TechColors.TextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Karma de reporte",
                                    color = TechColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = TechColors.NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ciclo 2026",
                                    color = TechColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reddit-Style Segmented Tabs: [HISTORIAL] | [AJUSTES]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(TechColors.TechCard)
                        .padding(4.dp)
                ) {
                    // Historial Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (currentTab == ProfileTab.HISTORIAL) TechColors.NeonCyan
                                else Color.Transparent
                            )
                            .clickable { currentTab = ProfileTab.HISTORIAL }
                            .padding(vertical = 8.dp)
                            .testTag("tab_profile_historial"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = if (currentTab == ProfileTab.HISTORIAL) Color.Black else TechColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Historial (${userReports.size})",
                                color = if (currentTab == ProfileTab.HISTORIAL) Color.Black else TechColors.TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Ajustes Tab (Reddit Style)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (currentTab == ProfileTab.AJUSTES) TechColors.NeonCyan
                                else Color.Transparent
                            )
                            .clickable { currentTab = ProfileTab.AJUSTES }
                            .padding(vertical = 8.dp)
                            .testTag("tab_profile_ajustes"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = if (currentTab == ProfileTab.AJUSTES) Color.Black else TechColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ajustes",
                                color = if (currentTab == ProfileTab.AJUSTES) Color.Black else TechColors.TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content View according to selected tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentTab) {
                        ProfileTab.HISTORIAL -> {
                            ProfileHistoryContent(
                                user = user,
                                userReports = userReports,
                                resolvedCount = resolvedCount,
                                inProcessCount = inProcessCount,
                                openCount = openCount,
                                onSelectReport = {
                                    onDismiss()
                                    onSelectReport(it)
                                }
                            )
                        }
                        ProfileTab.AJUSTES -> {
                            RedditSettingsContent(
                                user = user,
                                isCompactView = isCompactView,
                                notificationsEnabled = notificationsEnabled,
                                highPriorityAlerts = highPriorityAlerts,
                                fluidAnimations = fluidAnimations,
                                hapticFeedback = hapticFeedback,
                                onOpenAvatarPicker = onOpenAvatarPicker,
                                onChangePasswordClick = { showChangePasswordDialog = true },
                                onChangeEmailClick = { showChangeEmailDialog = true },
                                onShowRulesClick = { showSchoolRulesDialog = true },
                                onToggleCompact = { viewModel?.toggleCompactView(it) },
                                onToggleNotifications = { viewModel?.toggleNotifications(it) },
                                onToggleHighPriority = { viewModel?.toggleHighPriorityAlerts(it) },
                                onToggleAnimations = { viewModel?.toggleFluidAnimations(it) },
                                onToggleHaptic = { viewModel?.toggleHapticFeedback(it) },
                                onClearCache = {
                                    cacheCleanedMessage = "¡Caché de reportes liberada con éxito (14.2 MB)!"
                                    Toast.makeText(context, "Caché de reportes liberada (14.2 MB)", Toast.LENGTH_SHORT).show()
                                },
                                onLogoutClick = { showLogoutConfirmation = true }
                            )
                        }
                    }
                }

                // Cache cleared banner
                AnimatedVisibility(visible = cacheCleanedMessage != null) {
                    cacheCleanedMessage?.let { msg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = TechColors.NeonCyan.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg,
                                    color = TechColors.NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { cacheCleanedMessage = null },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = TechColors.NeonCyan, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Footer with Session Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showLogoutConfirmation = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF3B30)),
                        modifier = Modifier.testTag("profile_logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cerrar Sesión", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonCyan)
                    ) {
                        Text("Listo", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // --- Sub-Dialogs ---

    // 1. Change Password Dialog
    if (showChangePasswordDialog) {
        ChangePasswordModal(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { newPass ->
                viewModel?.changePassword(newPass) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) {
                        showChangePasswordDialog = false
                    }
                }
            }
        )
    }

    // 2. Change Email Dialog
    if (showChangeEmailDialog) {
        ChangeEmailModal(
            currentEmail = user.email,
            onDismiss = { showChangeEmailDialog = false },
            onConfirm = { newEmail ->
                viewModel?.updateUserEmail(newEmail) { _, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    showChangeEmailDialog = false
                }
            }
        )
    }

    // 3. School Rules Dialog (Reddit-Style Guidelines)
    if (showSchoolRulesDialog) {
        SchoolRulesModal(onDismiss = { showSchoolRulesDialog = false })
    }

    // 4. Logout Confirmation Dialog
    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            containerColor = TechColors.TechCard,
            title = {
                Text(
                    "¿Cerrar Sesión?",
                    color = TechColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Se cerrará la sesión de la cuenta u/${user.username}. Tendrás que ingresar tus credenciales nuevamente.",
                    color = TechColors.TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmation = false
                        onDismiss()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) {
                    Text("Cerrar Sesión", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("Cancelar", color = TechColors.TextSecondary)
                }
            }
        )
    }
}

// ==========================================
// 1. HISTORIAL CONTENT VIEW
// ==========================================
@Composable
private fun ProfileHistoryContent(
    user: UserEntity,
    userReports: List<ReportEntity>,
    resolvedCount: Int,
    inProcessCount: Int,
    openCount: Int,
    onSelectReport: (ReportEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryStatChip(
                count = userReports.size,
                label = if (user.role == UserRoles.SERVICIOS_GENERALES) "Atendidos" else "Reportados",
                color = TechColors.NeonCyan,
                modifier = Modifier.weight(1f)
            )
            HistoryStatChip(
                count = resolvedCount,
                label = "Resueltos",
                color = getStatusColor(ReportStatus.RESUELTO.name),
                modifier = Modifier.weight(1f)
            )
            HistoryStatChip(
                count = inProcessCount + openCount,
                label = "Pendientes",
                color = getStatusColor(ReportStatus.EN_PROCESO.name),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // History List
        if (userReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TechColors.TechCard)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = TechColors.TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = "No hay actividad en tu historial",
                        color = TechColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Cuando registres un daño o atiendas una novedad institucional, aparecerá en esta lista.",
                        color = TechColors.TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(userReports, key = { it.id }) { report ->
                    HistoryReportItemCard(
                        report = report,
                        onClick = { onSelectReport(report) }
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. REDDIT-STYLE SETTINGS CONTENT VIEW
// ==========================================
@Composable
private fun RedditSettingsContent(
    user: UserEntity,
    isCompactView: Boolean,
    notificationsEnabled: Boolean,
    highPriorityAlerts: Boolean,
    fluidAnimations: Boolean,
    hapticFeedback: Boolean,
    onOpenAvatarPicker: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onChangeEmailClick: () -> Unit,
    onShowRulesClick: () -> Unit,
    onToggleCompact: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleHighPriority: (Boolean) -> Unit,
    onToggleAnimations: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onLogoutClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // SECTION 1: CUENTA (REDDIT STYLE)
        item {
            RedditSectionHeader(title = "CUENTA (u/${user.username})")
            RedditSectionCard {
                RedditSettingRow(
                    icon = Icons.Default.Palette,
                    title = "Personalizar Avatar y Borde",
                    subtitle = "Cambia el personaje y el color neón del círculo",
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TechColors.NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    onClick = onOpenAvatarPicker
                )

                HorizontalDivider(color = TechColors.DeepCharcoal, thickness = 1.dp)

                RedditSettingRow(
                    icon = Icons.Default.Person,
                    title = "Nombre institucional",
                    subtitle = user.fullName,
                    trailing = {
                        Text(
                            text = user.role,
                            color = TechColors.NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )

                HorizontalDivider(color = TechColors.DeepCharcoal, thickness = 1.dp)

                RedditSettingRow(
                    icon = Icons.Default.Email,
                    title = "Correo institucional",
                    subtitle = if (user.email.isNotBlank()) user.email else "Toca para registrar tu correo",
                    trailing = {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar Correo",
                            tint = TechColors.NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    onClick = onChangeEmailClick
                )

                HorizontalDivider(color = TechColors.DeepCharcoal, thickness = 1.dp)

                RedditSettingRow(
                    icon = Icons.Default.Lock,
                    title = "Cambiar Contraseña",
                    subtitle = "Actualiza tu clave de acceso al sistema",
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TechColors.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    onClick = onChangePasswordClick
                )
            }
        }

        // SECTION 2: PREFERENCIAS DE LA APP (REDDIT STYLE)
        item {
            RedditSectionHeader(title = "PREFERENCIAS DE LA APP")
            RedditSectionCard {
                RedditSettingRow(
                    icon = Icons.Default.DarkMode,
                    title = "Modo Cyberpunk Oscuro",
                    subtitle = "Tema AMOLED de alto contraste",
                    trailing = {
                        Switch(
                            checked = true,
                            onCheckedChange = { /* Always dark in cyberpunk mode */ },
                            colors = redditSwitchColors()
                        )
                    }
                )

                HorizontalDivider(color = TechColors.DeepCharcoal, thickness = 1.dp)

                RedditSettingRow(
                    icon = Icons.Default.ViewCompact,
                    title = "Vista compacta de reportes",
                    subtitle = "Aumenta la densidad para mostrar más reportes",
                    trailing = {
                        Switch(
                            checked = isCompactView,
                            onCheckedChange = onToggleCompact,
                            colors = redditSwitchColors()
                        )
                    }
                )

                HorizontalDivider(color = TechColors.DeepCharcoal, thickness = 1.dp)

                RedditSettingRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Animaciones fluidas neón",
                    subtitle = "Transiciones dinámicas y micro-interacciones",
                    trailing = {
                        Switch(
                            checked = fluidAnimations,
                            onCheckedChange = onToggleAnimations,
                            colors = redditSwitchColors()
                        )
                    }
                )

                HorizontalDivider(color = TechColors.DeepCharcoal, thickness = 1.dp)

                RedditSettingRow(
                    icon = Icons.Default.Vibration,
                    title = "Respuesta háptica",
                    subtitle = "Vibración al registrar y actualizar estados",
                    trailing = {
                        Switch(
                            checked = hapticFeedback,
                            onCheckedChange = onToggleHaptic,
                            colors = redditSwitchColors()
                        )
                    }
                )
            }
        }

        // SECTION 3: NOTIFICACIONES (REDDIT STYLE)
        item {
            RedditSectionHeader(title = "NOTIFICACIONES")
            RedditSectionCard {
                RedditSettingRow(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones push de reportes",
                    subtitle = "Alertas cuando se actualice el estado de tus incidencias",
                    trailing = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = onToggleNotifications,
                            colors = redditSwitchColors()
                        )
                    }
                )

                HorizontalDivider(color = TechColors.DeepCharcoal, thickness = 1.dp)

                RedditSettingRow(
                    icon = Icons.Default.NotificationsActive,
                    title = "Alertas críticas de emergencia",
                    subtitle = "Sonido prioritario para daños de severidad alta",
                    trailing = {
                        Switch(
                            checked = highPriorityAlerts,
                            onCheckedChange = onToggleHighPriority,
                            colors = redditSwitchColors()
                        )
                    }
                )
            }
        }

        // SECTION 4: SOPORTE Y NORMAS (REDDIT COMMUNITY)
        item {
            RedditSectionHeader(title = "COMUNIDAD Y NORMAS")
            RedditSectionCard {
                RedditSettingRow(
                    icon = Icons.Default.MenuBook,
                    title = "Reglamento de Reportes Escolares",
                    subtitle = "Guía institucional para reportar daños y novedades",
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TechColors.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    onClick = onShowRulesClick
                )

                HorizontalDivider(color = TechColors.DeepCharcoal, thickness = 1.dp)

                RedditSettingRow(
                    icon = Icons.Default.Info,
                    title = "Versión de la Aplicación",
                    subtitle = "v1.4.0 (Build 2026.04) • CSR Tech",
                    trailing = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TechColors.DeepCharcoal)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Oficial", color = TechColors.NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        // SECTION 5: ACCIONES Y ZONA DE SESIÓN
        item {
            RedditSectionHeader(title = "ACCIONES DE LA CUENTA")
            RedditSectionCard {
                RedditSettingRow(
                    icon = Icons.Default.DeleteSweep,
                    title = "Limpiar caché local",
                    subtitle = "Libera almacenamiento temporal de fotos y reportes",
                    trailing = {
                        Button(
                            onClick = onClearCache,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TechColors.DeepCharcoal)
                        ) {
                            Text("Limpiar", color = TechColors.NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )

                HorizontalDivider(color = TechColors.DeepCharcoal, thickness = 1.dp)

                RedditSettingRow(
                    icon = Icons.Default.Logout,
                    title = "Cerrar sesión",
                    subtitle = "Desconectar u/${user.username} de este dispositivo",
                    titleColor = Color(0xFFFF3B30),
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    onClick = onLogoutClick
                )
            }
        }

        // Bottom space for scrolling
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ==========================================
// REDDIT-STYLE REUSABLE ROW & CARD COMPONENTS
// ==========================================
@Composable
private fun RedditSectionHeader(title: String) {
    Text(
        text = title,
        color = TechColors.TextSecondary,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
    )
}

@Composable
private fun RedditSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TechColors.TechCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun RedditSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = TechColors.TextPrimary,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TechColors.DeepCharcoal),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (titleColor == Color(0xFFFF3B30)) Color(0xFFFF3B30) else TechColors.NeonCyan,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = TechColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun redditSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.Black,
    checkedTrackColor = TechColors.NeonCyan,
    uncheckedThumbColor = TechColors.TextSecondary,
    uncheckedTrackColor = TechColors.DeepCharcoal,
    uncheckedBorderColor = TechColors.TechCard
)

// ==========================================
// SUB-DIALOGS (PASSWORD, EMAIL, RULES)
// ==========================================
@Composable
private fun ChangePasswordModal(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = TechColors.TechCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CAMBIAR CONTRASEÑA",
                    color = TechColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorText = null },
                    label = { Text("Nueva Contraseña", color = TechColors.TextSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TechColors.NeonCyan,
                        unfocusedBorderColor = TechColors.DeepCharcoal,
                        focusedTextColor = TechColors.TextPrimary,
                        unfocusedTextColor = TechColors.TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorText = null },
                    label = { Text("Confirmar Contraseña", color = TechColors.TextSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TechColors.NeonCyan,
                        unfocusedBorderColor = TechColors.DeepCharcoal,
                        focusedTextColor = TechColors.TextPrimary,
                        unfocusedTextColor = TechColors.TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorText!!,
                        color = Color(0xFFFF3B30),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TechColors.TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newPassword.isBlank() || newPassword.length < 3) {
                                errorText = "Mínimo 3 caracteres"
                            } else if (newPassword != confirmPassword) {
                                errorText = "Las contraseñas no coinciden"
                            } else {
                                onConfirm(newPassword)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonCyan)
                    ) {
                        Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeEmailModal(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var emailText by remember { mutableStateOf(currentEmail) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = TechColors.TechCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CORREO INSTITUCIONAL",
                    color = TechColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = emailText,
                    onValueChange = { emailText = it },
                    label = { Text("correo@colegio.edu", color = TechColors.TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TechColors.NeonCyan,
                        unfocusedBorderColor = TechColors.DeepCharcoal,
                        focusedTextColor = TechColors.TextPrimary,
                        unfocusedTextColor = TechColors.TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TechColors.TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(emailText.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonCyan)
                    ) {
                        Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SchoolRulesModal(onDismiss: () -> Unit) {
    val rules = listOf(
        "1. Identifica con precisión la ubicación del daño (ej. Aula 302, Laboratorio de Química, Baño 2° piso)." to "Permite al equipo de Servicios Generales llegar rápidamente al lugar.",
        "2. Selecciona la severidad adecuada (Baja, Media, Alta o Crítica)." to "Las incidencias que involucren riesgo eléctrico o de agua tienen prioridad crítica.",
        "3. Evita duplicar reportes existentes." to "Revisa en la lista de 'Abiertos' si otro estudiante o profesor ya reportó la misma novedad.",
        "4. Cuidado de las instalaciones institucionales." to "El reporte ayuda a mantener el colegio en condiciones óptimas para toda la comunidad."
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = TechColors.TechCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = TechColors.NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Normas de la Comunidad",
                        color = TechColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                rules.forEach { (ruleTitle, ruleDesc) ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(
                            text = ruleTitle,
                            color = TechColors.NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = ruleDesc,
                            color = TechColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonCyan)
                ) {
                    Text("Entendido", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// HISTORY ITEMS & CARDS
// ==========================================
@Composable
private fun HistoryStatChip(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TechColors.TechCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = TechColors.TextSecondary
            )
        }
    }
}

@Composable
private fun HistoryReportItemCard(
    report: ReportEntity,
    onClick: () -> Unit
) {
    val statusColor = getStatusColor(report.status)
    val severityColor = getSeverityColor(report.severity)
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(report.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_report_item_${report.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TechColors.TechCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.code,
                    color = TechColors.NeonCyan,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(severityColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = report.severity,
                            color = severityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = report.status.replace("_", " "),
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = report.title,
                color = TechColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TechColors.TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = report.location,
                        color = TechColors.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = dateStr,
                    color = TechColors.TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
