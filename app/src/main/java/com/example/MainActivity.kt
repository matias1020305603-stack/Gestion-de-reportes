package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AvatarPickerScreen
import com.example.ui.components.CreateReportDialog
import com.example.ui.components.ReportDetailDialog
import com.example.ui.components.UserProfileHistoryDialog
import com.example.ui.components.getAvatarDrawableRes
import com.example.ui.components.hexToColor
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SummaryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigator()
            }
        }
    }
}

enum class AuthView {
    LOGIN,
    REGISTER
}

@Composable
fun AppNavigator(
    viewModel: MainViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var currentAuthView by remember { mutableStateOf(AuthView.LOGIN) }

    // FIRST VIEW: If not logged in, show the requested Login or Register screen
    if (currentUser == null) {
        BackHandler(enabled = currentAuthView == AuthView.REGISTER) {
            currentAuthView = AuthView.LOGIN
        }

        Crossfade(targetState = currentAuthView, label = "auth_screen_transition") { authView ->
            when (authView) {
                AuthView.LOGIN -> LoginScreen(
                    viewModel = viewModel,
                    onNavigateToRegister = { currentAuthView = AuthView.REGISTER },
                    onLoginSuccess = { /* Automatically transitions via currentUser state */ }
                )
                AuthView.REGISTER -> RegisterScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = { currentAuthView = AuthView.LOGIN },
                    onRegisterSuccess = { /* Automatically transitions via currentUser state */ }
                )
            }
        }
    } else {
        // Main Reports & Metrics Management Dashboard
        MainDashboard(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: MainViewModel
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isReportDialogOpen by viewModel.isReportDialogOpen.collectAsStateWithLifecycle()
    val selectedReportDetail by viewModel.selectedReportDetail.collectAsStateWithLifecycle()
    val isAvatarPickerOpen by viewModel.isAvatarPickerOpen.collectAsStateWithLifecycle()
    val isProfileOpen by viewModel.isProfileOpen.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allReports by viewModel.allReports.collectAsStateWithLifecycle()

    var userMenuExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = isProfileOpen || isAvatarPickerOpen || selectedTab != 0 || isReportDialogOpen || selectedReportDetail != null) {
        when {
            isProfileOpen -> viewModel.closeProfile()
            isAvatarPickerOpen -> viewModel.closeAvatarPicker()
            selectedReportDetail != null -> viewModel.closeReportDetail()
            isReportDialogOpen -> viewModel.closeReportDialog()
            selectedTab != 0 -> viewModel.selectTab(0)
        }
    }

    if (isAvatarPickerOpen) {
        AvatarPickerScreen(
            initialAvatarId = currentUser?.avatarId ?: 1,
            initialColorHex = currentUser?.avatarColorHex ?: "#00FFFF",
            onDismiss = { viewModel.closeAvatarPicker() },
            onAvatarGuardado = { avatarId, colorHex ->
                viewModel.updateUserAvatar(avatarId, colorHex)
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.clickable { viewModel.openProfile() }
                        ) {
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Reportes e Incidencias"
                                    else -> "Métricas de Atención"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                            currentUser?.let { user ->
                                Text(
                                    text = "${user.fullName} (${user.role})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { viewModel.openProfile() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("user_profile_chip")
                            ) {
                                // Dynamic circular avatar with customized border color
                                val borderColor = currentUser?.avatarColorHex?.let { hexToColor(it) } ?: Color(0xFF00FFFF)
                                val avatarRes = getAvatarDrawableRes(currentUser?.avatarId ?: 1)

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E1E1E))
                                        .border(width = 2.dp, color = borderColor, shape = CircleShape),
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
                                }

                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentUser?.role ?: "Sesión",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = { userMenuExpanded = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Menú",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = userMenuExpanded,
                                onDismissRequest = { userMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Mi Perfil e Historial") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        userMenuExpanded = false
                                        viewModel.openProfile()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ajustes de Cuenta") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        userMenuExpanded = false
                                        viewModel.openProfile()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Personalizar Avatar") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        userMenuExpanded = false
                                        viewModel.openAvatarPicker()
                                    }
                                )
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Logout,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        userMenuExpanded = false
                                        viewModel.logout()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        icon = { Icon(Icons.Default.Description, contentDescription = "Reportes") },
                        label = { Text("Reportes") },
                        modifier = Modifier.testTag("nav_tab_reports")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Métricas") },
                        label = { Text("Métricas") },
                        modifier = Modifier.testTag("nav_tab_summary")
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = { viewModel.openCreateReportDialog() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_report")
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = "Nuevo Reporte")
                    }
                }
            }
        ) { innerPadding ->
            Crossfade(
                targetState = selectedTab,
                label = "tab_crossfade",
                modifier = Modifier.padding(innerPadding)
            ) { tab ->
                when (tab) {
                    0 -> ReportsScreen(
                        viewModel = viewModel,
                        onSelectReport = { report -> viewModel.openReportDetail(report) }
                    )
                    1 -> SummaryScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // User Profile and Report History Dialog
    if (isProfileOpen && currentUser != null) {
        UserProfileHistoryDialog(
            user = currentUser!!,
            reports = allReports,
            onDismiss = { viewModel.closeProfile() },
            onOpenAvatarPicker = {
                viewModel.closeProfile()
                viewModel.openAvatarPicker()
            },
            onSelectReport = { report ->
                viewModel.closeProfile()
                viewModel.openReportDetail(report)
            },
            onLogout = {
                viewModel.closeProfile()
                viewModel.logout()
            },
            viewModel = viewModel
        )
    }

    // Report Create Dialog
    if (isReportDialogOpen) {
        CreateReportDialog(
            initialReporter = currentUser?.fullName ?: "",
            onDismiss = { viewModel.closeReportDialog() },
            onSave = { title, description, category, severity, location, reportedBy ->
                viewModel.saveReport(title, description, category, severity, location, reportedBy)
            }
        )
    }

    // Report Detail Dialog
    selectedReportDetail?.let { report ->
        ReportDetailDialog(
            report = report,
            onDismiss = { viewModel.closeReportDetail() },
            onStatusChange = { newStatus, notes ->
                viewModel.updateReportStatus(report.id, newStatus, notes)
            },
            onDelete = {
                viewModel.deleteReport(report)
            }
        )
    }
}
