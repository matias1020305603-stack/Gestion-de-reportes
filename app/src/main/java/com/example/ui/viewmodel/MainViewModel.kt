package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ReportCategory
import com.example.data.model.ReportEntity
import com.example.data.model.ReportSeverity
import com.example.data.model.ReportStatus
import com.example.data.model.TaskCategory
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.UserEntity
import com.example.data.repository.TaskReportRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri
import com.example.data.model.ChatMessage
import com.example.data.model.FriendContact
import com.example.data.model.SocialComment
import com.example.data.model.SocialPost
import com.example.data.model.UserRoles
import com.example.util.ContentModerator
import com.example.util.EmailSendResult
import com.example.util.EmailService
import com.example.util.ImageModerationResult
import com.example.util.ImageSafetyModerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TaskFilterStatus(val label: String) {
    ALL("Todas"),
    PENDING("Pendientes"),
    COMPLETED("Completadas")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskReportRepository
    val firestoreRepository: com.example.data.repository.FirestoreReportRepository by lazy {
        com.example.data.repository.FirestoreReportRepository(application)
    }

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = TaskReportRepository(database.taskDao(), database.reportDao(), database.userDao())
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.ensurePreconfiguredServiciosAccounts()
        }
    }

    // --- Authentication & Session State ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Flag for displaying the Login/Register modal if requested, or starting as guest
    private val _isAuthDialogOpen = MutableStateFlow(false)
    val isAuthDialogOpen: StateFlow<Boolean> = _isAuthDialogOpen.asStateFlow()

    fun openAuthDialog() { _isAuthDialogOpen.value = true }
    fun closeAuthDialog() { _isAuthDialogOpen.value = false }

    // Avatar Picker State
    private val _isAvatarPickerOpen = MutableStateFlow(false)
    val isAvatarPickerOpen: StateFlow<Boolean> = _isAvatarPickerOpen.asStateFlow()

    fun openAvatarPicker() { _isAvatarPickerOpen.value = true }
    fun closeAvatarPicker() { _isAvatarPickerOpen.value = false }

    // User Profile & History Dialog State
    private val _isProfileOpen = MutableStateFlow(false)
    val isProfileOpen: StateFlow<Boolean> = _isProfileOpen.asStateFlow()

    fun openProfile() { _isProfileOpen.value = true }
    fun closeProfile() { _isProfileOpen.value = false }

    fun updateUserAvatar(avatarId: Int, colorHex: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.updateUserAvatar(user.id, avatarId, colorHex)
            _currentUser.value = user.copy(avatarId = avatarId, avatarColorHex = colorHex)
            closeAvatarPicker()
        }
    }

    fun changePassword(newPassword: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val trimmed = newPassword.trim()
            if (trimmed.length < 3) {
                onResult(false, "La contraseña debe tener mínimo 3 caracteres")
                return@launch
            }
            repository.updateUserPassword(user.id, trimmed)
            _currentUser.value = user.copy(password = trimmed)
            onResult(true, "Contraseña actualizada con éxito")
        }
    }

    fun updateUserEmail(newEmail: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val trimmed = newEmail.trim()
            repository.updateUserEmail(user.id, trimmed)
            _currentUser.value = user.copy(email = trimmed)
            onResult(true, "Correo institucional actualizado")
        }
    }

    // --- Reddit-style App Preferences State ---
    private val _isCompactView = MutableStateFlow(false)
    val isCompactView: StateFlow<Boolean> = _isCompactView.asStateFlow()
    fun toggleCompactView(enabled: Boolean) { _isCompactView.value = enabled }

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    fun toggleNotifications(enabled: Boolean) { _notificationsEnabled.value = enabled }

    private val _highPriorityAlerts = MutableStateFlow(true)
    val highPriorityAlerts: StateFlow<Boolean> = _highPriorityAlerts.asStateFlow()
    fun toggleHighPriorityAlerts(enabled: Boolean) { _highPriorityAlerts.value = enabled }

    private val _fluidAnimations = MutableStateFlow(true)
    val fluidAnimations: StateFlow<Boolean> = _fluidAnimations.asStateFlow()
    fun toggleFluidAnimations(enabled: Boolean) { _fluidAnimations.value = enabled }

    private val _hapticFeedback = MutableStateFlow(true)
    val hapticFeedback: StateFlow<Boolean> = _hapticFeedback.asStateFlow()
    fun toggleHapticFeedback(enabled: Boolean) { _hapticFeedback.value = enabled }


    fun login(
        username: String,
        password: String,
        expectedRole: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmedUser = username.trim()
            val trimmedPass = password.trim()
            if (trimmedUser.isBlank() || trimmedPass.isBlank()) {
                onResult(false, "Ingresa tu usuario y contraseña")
                return@launch
            }
            val user = repository.findUserByUsername(trimmedUser)
            if (user != null && user.password == trimmedPass) {
                if (expectedRole != null && user.role != expectedRole) {
                    onResult(false, "Esta cuenta pertenece al rol '${user.role}'. Selecciona '${user.role}' arriba para continuar.")
                    return@launch
                }
                _currentUser.value = user
                _activeChatFriend.value = null
                _isAuthDialogOpen.value = false
                onResult(true, "¡Bienvenido, ${user.fullName} (${user.role})!")
            } else {
                onResult(false, "Usuario o contraseña incorrectos")
            }
        }
    }

    fun register(
        username: String,
        fullName: String,
        password: String,
        role: String,
        email: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val u = username.trim()
            val fn = fullName.trim()
            val p = password.trim()
            if (u.isBlank() || fn.isBlank() || p.isBlank()) {
                onResult(false, "Por favor completa todos los campos requeridos")
                return@launch
            }
            if (role == UserRoles.SERVICIOS_GENERALES) {
                onResult(false, "Las cuentas de Servicios Generales son asignadas por la administración del colegio. No requieren registrarse.")
                return@launch
            }

            val usernameCheck = com.example.util.UsernameValidator.validate(u)
            if (!usernameCheck.isValid) {
                onResult(false, usernameCheck.message ?: "Nombre de usuario no permitido")
                return@launch
            }

            val existing = repository.findUserByUsername(u)
            if (existing != null) {
                onResult(false, "El nombre de usuario 'u/$u' ya está en uso. Por favor elige otro.")
                return@launch
            }

            val newUser = UserEntity(
                username = u,
                fullName = fn,
                password = p,
                role = role,
                email = email.trim()
            )
            val newId = repository.registerUser(newUser)
            val created = newUser.copy(id = newId)
            _currentUser.value = created
            _activeChatFriend.value = null
            _isAuthDialogOpen.value = false
            onResult(true, "Registro exitoso como $role")
        }
    }

    fun logout() {
        _activeChatFriend.value = null
        _currentUser.value = null
    }

    fun requestPasswordReset(usernameOrEmail: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val query = usernameOrEmail.trim()
            if (query.isBlank()) {
                onResult(false, "Ingresa tu usuario o correo institucional")
                return@launch
            }
            val user = repository.findUserByUsername(query)
            if (user == null) {
                onResult(false, "No se encontró ninguna cuenta con ese usuario o correo")
                return@launch
            }
            val tempPassword = "Temp#${(1000..9999).random()}"
            repository.updateUserPassword(user.id, tempPassword)

            val emailToUse = user.email.ifBlank { "${user.username}@colegio.edu.co" }
            val result = EmailService.dispatchVerificationEmail(
                context = getApplication(),
                recipientEmail = emailToUse,
                recipientName = user.fullName,
                code = tempPassword,
                role = user.role
            )

            when (result) {
                is EmailSendResult.RealEmailSent -> {
                    onResult(true, "¡Correo de restablecimiento enviado a $emailToUse! Tu nueva contraseña temporal es: $tempPassword")
                }
                is EmailSendResult.SimulatedSent -> {
                    onResult(true, "¡Correo enviado con éxito! Tu nueva contraseña temporal es: $tempPassword")
                }
                is EmailSendResult.Error -> {
                    onResult(false, "Error al enviar correo: ${result.message}")
                }
            }
        }
    }

    fun quickLoginAs(role: String) {
        viewModelScope.launch {
            val username = when (role) {
                UserRoles.SERVICIOS_GENERALES -> "servicios"
                UserRoles.ADMINISTRADOR -> "admin"
                else -> "profesor"
            }
            val user = repository.findUserByUsername(username)
            if (user != null) {
                _currentUser.value = user
                _activeChatFriend.value = null
                _isAuthDialogOpen.value = false
            } else {
                val tempUser = UserEntity(
                    username = username,
                    fullName = when (role) {
                        UserRoles.SERVICIOS_GENERALES -> "Mario Gómez (Servicios Generales)"
                        UserRoles.ADMINISTRADOR -> "Super Administrador"
                        else -> "Prof. Andrés Castro"
                    },
                    password = if (role == UserRoles.ADMINISTRADOR) "AdminMaster2026*" else "123",
                    role = role
                )
                val id = repository.registerUser(tempUser)
                _currentUser.value = tempUser.copy(id = id)
                _activeChatFriend.value = null
                _isAuthDialogOpen.value = false
            }
        }
    }

    // --- Navigation ---
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    // --- Data Flows ---
    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allReports: StateFlow<List<ReportEntity>> = repository.allReports.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun adminDeleteUser(userId: Long) {
        viewModelScope.launch {
            repository.deleteUserById(userId)
        }
    }

    fun adminUpdateUserPassword(userId: Long, newPass: String) {
        viewModelScope.launch {
            repository.updateUserPassword(userId, newPass)
        }
    }

    fun adminDeleteReport(reportId: Long) {
        viewModelScope.launch {
            repository.deleteReportById(reportId)
        }
    }

    fun adminUpdateReportStatus(reportId: Long, status: String, notes: String) {
        viewModelScope.launch {
            repository.updateReportStatus(reportId, status, notes)
        }
    }

    // Task Filter State
    private val _taskSearchQuery = MutableStateFlow("")
    val taskSearchQuery: StateFlow<String> = _taskSearchQuery.asStateFlow()

    private val _taskFilterStatus = MutableStateFlow(TaskFilterStatus.ALL)
    val taskFilterStatus: StateFlow<TaskFilterStatus> = _taskFilterStatus.asStateFlow()

    private val _taskFilterPriority = MutableStateFlow<String?>(null)
    val taskFilterPriority: StateFlow<String?> = _taskFilterPriority.asStateFlow()

    // Filtered Tasks
    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        _taskSearchQuery,
        _taskFilterStatus,
        _taskFilterPriority
    ) { tasks, query, status, priority ->
        tasks.filter { task ->
            val matchesQuery = query.isBlank() ||
                task.title.contains(query, ignoreCase = true) ||
                task.description.contains(query, ignoreCase = true) ||
                task.category.contains(query, ignoreCase = true)

            val matchesStatus = when (status) {
                TaskFilterStatus.ALL -> true
                TaskFilterStatus.PENDING -> !task.isCompleted
                TaskFilterStatus.COMPLETED -> task.isCompleted
            }

            val matchesPriority = priority == null || task.priority == priority

            matchesQuery && matchesStatus && matchesPriority
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Report Filter State
    private val _reportSearchQuery = MutableStateFlow("")
    val reportSearchQuery: StateFlow<String> = _reportSearchQuery.asStateFlow()

    private val _reportFilterStatus = MutableStateFlow<String?>(null)
    val reportFilterStatus: StateFlow<String?> = _reportFilterStatus.asStateFlow()

    private val _reportFilterSeverity = MutableStateFlow<String?>(null)
    val reportFilterSeverity: StateFlow<String?> = _reportFilterSeverity.asStateFlow()

    // Filtered Reports
    val filteredReports: StateFlow<List<ReportEntity>> = combine(
        allReports,
        _reportSearchQuery,
        _reportFilterStatus,
        _reportFilterSeverity
    ) { reports, query, status, severity ->
        reports.filter { report ->
            val matchesQuery = query.isBlank() ||
                report.title.contains(query, ignoreCase = true) ||
                report.code.contains(query, ignoreCase = true) ||
                report.description.contains(query, ignoreCase = true) ||
                report.location.contains(query, ignoreCase = true) ||
                report.reportedBy.contains(query, ignoreCase = true)

            val matchesStatus = status == null || report.status == status
            val matchesSeverity = severity == null || report.severity == severity

            matchesQuery && matchesStatus && matchesSeverity
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dialog / Sheet states
    private val _isTaskDialogOpen = MutableStateFlow(false)
    val isTaskDialogOpen: StateFlow<Boolean> = _isTaskDialogOpen.asStateFlow()

    private val _editingTask = MutableStateFlow<TaskEntity?>(null)
    val editingTask: StateFlow<TaskEntity?> = _editingTask.asStateFlow()

    private val _isReportDialogOpen = MutableStateFlow(false)
    val isReportDialogOpen: StateFlow<Boolean> = _isReportDialogOpen.asStateFlow()

    private val _selectedReportDetail = MutableStateFlow<ReportEntity?>(null)
    val selectedReportDetail: StateFlow<ReportEntity?> = _selectedReportDetail.asStateFlow()

    // Filter update functions
    fun setTaskSearchQuery(query: String) { _taskSearchQuery.value = query }
    fun setTaskFilterStatus(status: TaskFilterStatus) { _taskFilterStatus.value = status }
    fun setTaskFilterPriority(priority: String?) {
        _taskFilterPriority.value = if (_taskFilterPriority.value == priority) null else priority
    }

    fun setReportSearchQuery(query: String) { _reportSearchQuery.value = query }
    fun setReportFilterStatus(status: String?) {
        _reportFilterStatus.value = if (_reportFilterStatus.value == status) null else status
    }
    fun setReportFilterSeverity(severity: String?) {
        _reportFilterSeverity.value = if (_reportFilterSeverity.value == severity) null else severity
    }

    // Task Actions
    fun openCreateTaskDialog() {
        _editingTask.value = null
        _isTaskDialogOpen.value = true
    }

    fun openEditTaskDialog(task: TaskEntity) {
        _editingTask.value = task
        _isTaskDialogOpen.value = true
    }

    fun closeTaskDialog() {
        _editingTask.value = null
        _isTaskDialogOpen.value = false
    }

    fun saveTask(
        title: String,
        description: String,
        priority: String,
        category: String,
        dueDate: String
    ) {
        viewModelScope.launch {
            val current = _editingTask.value
            if (current != null) {
                repository.updateTask(
                    current.copy(
                        title = title.trim(),
                        description = description.trim(),
                        priority = priority,
                        category = category,
                        dueDate = dueDate.trim()
                    )
                )
            } else {
                repository.addTask(
                    TaskEntity(
                        title = title.trim(),
                        description = description.trim(),
                        priority = priority,
                        category = category,
                        dueDate = dueDate.trim(),
                        isCompleted = false
                    )
                )
            }
            closeTaskDialog()
        }
    }

    fun toggleTask(id: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTask(id, isCompleted)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // Report Actions & Image Moderation
    private val _reportImageModeration = MutableStateFlow<ImageModerationResult>(ImageModerationResult.Idle)
    val reportImageModeration: StateFlow<ImageModerationResult> = _reportImageModeration.asStateFlow()

    fun validateReportImage(uri: Uri) {
        viewModelScope.launch {
            _reportImageModeration.value = ImageModerationResult.Checking
            _reportImageModeration.value = ImageSafetyModerator.moderateImageUri(getApplication(), uri)
        }
    }

    fun resetReportImageModeration() {
        _reportImageModeration.value = ImageModerationResult.Idle
    }

    fun openCreateReportDialog() {
        _reportImageModeration.value = ImageModerationResult.Idle
        _isReportDialogOpen.value = true
    }

    fun closeReportDialog() {
        _reportImageModeration.value = ImageModerationResult.Idle
        _isReportDialogOpen.value = false
    }

    fun openReportDetail(report: ReportEntity) {
        _selectedReportDetail.value = report
    }

    fun closeReportDetail() {
        _selectedReportDetail.value = null
    }

    fun saveReport(
        title: String,
        description: String,
        category: String,
        severity: String,
        location: String,
        reportedBy: String,
        imageUriString: String? = null
    ) {
        viewModelScope.launch {
            val savedImagePath = if (!imageUriString.isNullOrBlank()) {
                if (_reportImageModeration.value !is ImageModerationResult.Approved) {
                    return@launch
                }
                copyImageToInternalStorage(imageUriString)
            } else {
                ""
            }
            val count = allReports.value.size + 101
            val reportCode = "REP-$count"
            val userAuthor = _currentUser.value?.fullName ?: reportedBy.ifBlank { "Operador" }
            repository.addReport(
                ReportEntity(
                    code = reportCode,
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    severity = severity,
                    status = ReportStatus.EN_PROCESO.name,
                    location = if (location.isBlank()) "Área de Planta" else location.trim(),
                    reportedBy = userAuthor,
                    imagePath = savedImagePath
                )
            )

            // Sincronización en la nube con Firebase Firestore si el usuario está autenticado
            try {
                val authUser = com.google.firebase.Firebase.auth.currentUser
                if (authUser != null) {
                    firestoreRepository.createReport(
                        com.example.data.model.ReportDocument(
                            userId = authUser.uid,
                            code = reportCode,
                            title = title.trim(),
                            description = description.trim(),
                            location = if (location.isBlank()) "Área de Planta" else location.trim(),
                            category = category,
                            severity = severity,
                            status = ReportStatus.EN_PROCESO.name,
                            authorName = userAuthor,
                            authorRole = _currentUser.value?.role ?: UserRoles.PROFESOR,
                            authorEmail = _currentUser.value?.email ?: authUser.email ?: ""
                        )
                    )
                }
            } catch (_: Exception) {}

            closeReportDialog()
        }
    }

    private suspend fun copyImageToInternalStorage(uriString: String): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val uri = android.net.Uri.parse(uriString)
                val imagesDir = java.io.File(context.filesDir, "report_images").apply { mkdirs() }
                val destFile = java.io.File(imagesDir, "rep_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (destFile.exists() && destFile.length() > 0) destFile.absolutePath else ""
            } catch (_: Exception) {
                ""
            }
        }

    fun updateReportStatus(reportId: Long, newStatus: String, notes: String) {
        viewModelScope.launch {
            repository.updateReportStatus(reportId, newStatus, notes.trim())
            val updated = allReports.value.find { it.id == reportId }
            if (updated != null) {
                _selectedReportDetail.value = updated.copy(
                    status = newStatus,
                    resolutionNotes = notes.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
    }

    fun claimReport(reportId: Long) {
        val user = _currentUser.value ?: return
        val staffName = user.fullName.ifBlank { user.username }
        viewModelScope.launch {
            repository.assignReport(reportId, assignedTo = staffName, status = ReportStatus.EN_PROCESO.name)
            val updated = allReports.value.find { it.id == reportId }
            if (updated != null) {
                _selectedReportDetail.value = updated.copy(
                    assignedTo = staffName,
                    status = ReportStatus.EN_PROCESO.name,
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
    }

    fun resolveAssignedReport(reportId: Long, notes: String) {
        viewModelScope.launch {
            val finalNotes = notes.trim().ifBlank { "Reparación completada y solucionada por personal autorizado." }
            repository.updateReportStatus(reportId, status = ReportStatus.RESUELTO.name, notes = finalNotes)
            val updated = allReports.value.find { it.id == reportId }
            if (updated != null) {
                _selectedReportDetail.value = updated.copy(
                    status = ReportStatus.RESUELTO.name,
                    resolutionNotes = finalNotes,
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
    }

    fun deleteReport(report: ReportEntity) {
        viewModelScope.launch {
            if (report.imagePath.isNotBlank()) {
                try {
                    java.io.File(report.imagePath).delete()
                } catch (_: Exception) {}
            }
            repository.deleteReport(report)
            if (_selectedReportDetail.value?.id == report.id) {
                _selectedReportDetail.value = null
            }
        }
    }

    fun clearAllReports() {
        viewModelScope.launch {
            try {
                val imagesDir = java.io.File(getApplication<Application>().filesDir, "report_images")
                if (imagesDir.exists()) imagesDir.deleteRecursively()
            } catch (_: Exception) {}
            repository.clearAllReports()
            _selectedReportDetail.value = null
        }
    }

    // Formatted Executive Summary for Sharing
    fun generateExecutiveSummary(): String {
        val reports = allReports.value

        val openReports = reports.count { it.status == ReportStatus.ABIERTO.name }
        val inProcessReports = reports.count { it.status == ReportStatus.EN_PROCESO.name }
        val resolvedReports = reports.count { it.status == ReportStatus.RESUELTO.name }
        val total = reports.size
        val resolutionPct = if (total > 0) (resolvedReports * 100 / total) else 0

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val now = dateFormat.format(Date())
        val reporter = _currentUser.value?.let { "${it.fullName} (${it.role})" } ?: "Personal Institucional"

        return buildString {
            appendLine("📋 INFORME INSTITUCIONAL DE REPORTES E INCIDENCIAS")
            appendLine("📅 Fecha: $now")
            appendLine("👤 Emitido por: $reporter")
            appendLine("--------------------------------------------")
            appendLine("📊 BALANCE GENERAL:")
            appendLine("• Total reportes registrados: $total")
            appendLine("• Resueltos: $resolvedReports ($resolutionPct%)")
            appendLine("• En proceso de atención: $inProcessReports")
            appendLine("• Pendientes / Abiertos: $openReports")
            appendLine("--------------------------------------------")
            if (reports.isNotEmpty()) {
                appendLine("📝 DETALLE DE REPORTES:")
                reports.forEach { rep ->
                    appendLine("• [${rep.code}] ${rep.title}")
                    appendLine("  Área: ${rep.location} | Severidad: ${rep.severity} | Estado: ${rep.status}")
                    if (rep.resolutionNotes.isNotBlank()) {
                        appendLine("  Solución: ${rep.resolutionNotes}")
                    }
                }
            } else {
                appendLine("Sin novedades ni incidencias pendientes registradas.")
            }
        }
    }

    fun loginAsGoogleUser(uid: String, name: String, email: String, role: String) {
        viewModelScope.launch {
            val userKey = email.ifBlank { uid }
            var user = repository.findUserByUsername(userKey)
            if (user == null) {
                val newUser = UserEntity(
                    username = if (email.contains("@")) email.substringBefore("@") else "user_$uid",
                    fullName = name.ifBlank { "Usuario Institucional" },
                    password = uid,
                    role = role,
                    email = email,
                    avatarId = (1..20).random(),
                    avatarColorHex = "#00FFFF"
                )
                val id = repository.registerUser(newUser)
                user = newUser.copy(id = id)
            }
            _activeChatFriend.value = null
            _currentUser.value = user
        }
    }

    // --- Social Feed State & Methods (Moderated community feed) ---
    private val _socialImageModeration = MutableStateFlow<ImageModerationResult>(ImageModerationResult.Idle)
    val socialImageModeration: StateFlow<ImageModerationResult> = _socialImageModeration.asStateFlow()

    fun validateSocialImage(uri: Uri) {
        viewModelScope.launch {
            _socialImageModeration.value = ImageModerationResult.Checking
            _socialImageModeration.value = ImageSafetyModerator.moderateImageUri(getApplication(), uri)
        }
    }

    fun resetSocialImageModeration() {
        _socialImageModeration.value = ImageModerationResult.Idle
    }

    private val _socialPosts = MutableStateFlow<List<SocialPost>>(
        listOf(
            SocialPost(
                authorName = "Carlos Pérez",
                authorRole = "Estudiante",
                avatarId = 1,
                avatarColorHex = "#00FFFF",
                content = "Se reporta daño en la llave de agua del segundo piso del bloque B. Hay una pequeña fuga. #Planta #Daños",
                imageUrl = "https://picsum.photos/400/300",
                timestamp = "Hace 15 min",
                likesCount = 4,
                comments = listOf(
                    SocialComment(
                        authorName = "Prof. Andrea Gómez",
                        authorRole = "Profesor",
                        avatarId = 2,
                        avatarColorHex = "#FF00FF",
                        text = "Gracias por reportarlo Carlos, lo notificaremos a Servicios Generales.",
                        timestamp = "Hace 10 min"
                    )
                )
            ),
            SocialPost(
                authorName = "Servicios Generales CSR",
                authorRole = UserRoles.SERVICIOS_GENERALES,
                avatarId = 3,
                avatarColorHex = "#34C759",
                content = "Atendiendo revisión de luminarias en el salón 201. Todo en orden por ahora. ¡Buen día a todos!",
                timestamp = "Hace 1 hora",
                likesCount = 9
            )
        )
    )
    val socialPosts: StateFlow<List<SocialPost>> = _socialPosts.asStateFlow()

    fun createSocialPost(
        content: String,
        imageUrl: String?,
        localImageUriString: String? = null
    ): Pair<Boolean, String?> {
        val check = ContentModerator.checkContent(content)
        if (!check.first) {
            return check
        }
        val trimmedUrl = imageUrl?.trim()?.ifBlank { null }
        if (trimmedUrl != null) {
            val urlCheck = ContentModerator.checkContent(trimmedUrl)
            if (!urlCheck.first) {
                return Pair(false, "La URL o enlace de imagen contiene términos no permitidos.")
            }
        }
        if (!localImageUriString.isNullOrBlank()) {
            when (val modState = _socialImageModeration.value) {
                is ImageModerationResult.Approved -> Unit
                is ImageModerationResult.Checking ->
                    return Pair(false, "Espera a que termine la verificación de seguridad de la imagen.")
                is ImageModerationResult.Rejected ->
                    return Pair(false, "Imagen bloqueada: ${modState.reason}")
                is ImageModerationResult.Error ->
                    return Pair(false, modState.message)
                is ImageModerationResult.Idle ->
                    return Pair(false, "Debes validar la imagen seleccionada antes de publicar.")
            }
        }

        val user = _currentUser.value
        val authorName = user?.fullName ?: "Usuario Anónimo"
        val authorRole = user?.role ?: UserRoles.PROFESOR
        val avatarId = user?.avatarId ?: 1
        val avatarColorHex = user?.avatarColorHex ?: "#00FFFF"

        if (!localImageUriString.isNullOrBlank()) {
            viewModelScope.launch {
                val savedPath = copyImageToInternalStorage(localImageUriString)
                val finalImageRef = savedPath.ifBlank { localImageUriString }
                val newPost = SocialPost(
                    authorName = authorName,
                    authorRole = authorRole,
                    avatarId = avatarId,
                    avatarColorHex = avatarColorHex,
                    content = content.trim(),
                    imageUrl = finalImageRef,
                    timestamp = "Hace un momento"
                )
                _socialPosts.value = listOf(newPost) + _socialPosts.value
                _socialImageModeration.value = ImageModerationResult.Idle
            }
            return Pair(true, null)
        }

        val newPost = SocialPost(
            authorName = authorName,
            authorRole = authorRole,
            avatarId = avatarId,
            avatarColorHex = avatarColorHex,
            content = content.trim(),
            imageUrl = trimmedUrl,
            timestamp = "Hace un momento"
        )
        _socialPosts.value = listOf(newPost) + _socialPosts.value
        return Pair(true, null)
    }

    fun toggleLikePost(postId: String) {
        _socialPosts.value = _socialPosts.value.map { post ->
            if (post.id == postId) {
                val newLiked = !post.isLikedByMe
                post.copy(
                    isLikedByMe = newLiked,
                    likesCount = if (newLiked) post.likesCount + 1 else maxOf(0, post.likesCount - 1)
                )
            } else {
                post
            }
        }
    }

    fun addCommentToPost(postId: String, commentText: String): Pair<Boolean, String?> {
        val check = ContentModerator.checkContent(commentText)
        if (!check.first) {
            return check
        }
        val user = _currentUser.value
        val authorName = user?.fullName ?: "Usuario Anónimo"
        val authorRole = user?.role ?: UserRoles.PROFESOR
        val avatarId = user?.avatarId ?: 1
        val avatarColorHex = user?.avatarColorHex ?: "#00FFFF"

        val comment = SocialComment(
            authorName = authorName,
            authorRole = authorRole,
            avatarId = avatarId,
            avatarColorHex = avatarColorHex,
            text = commentText.trim(),
            timestamp = "Hace un momento"
        )

        _socialPosts.value = _socialPosts.value.map { post ->
            if (post.id == postId) {
                post.copy(comments = post.comments + comment)
            } else {
                post
            }
        }
        return Pair(true, null)
    }

    // --- Chat / Amigos State & Methods (Estricto aislamiento por grupo/rol) ---
    fun normalizeRoleGroup(role: String?): String {
        val normalized = role?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return when {
            normalized.contains("servicios") || normalized.contains("serv.") || normalized.contains("mantenimiento") ->
                UserRoles.SERVICIOS_GENERALES
            else -> UserRoles.PROFESOR
        }
    }

    private val institutionalDirectoryPeers = listOf(
        FriendContact(
            id = "dir_prof_1",
            name = "Prof. Roberto Méndez",
            role = UserRoles.PROFESOR,
            avatarId = 2,
            avatarColorHex = "#FF00FF",
            lastMessage = "Revisé el reporte del salón 201.",
            isOnline = true
        ),
        FriendContact(
            id = "dir_prof_2",
            name = "Prof. Andrea Gómez",
            role = UserRoles.PROFESOR,
            avatarId = 6,
            avatarColorHex = "#00FFFF",
            lastMessage = "¿Confirmamos la revisión del laboratorio?",
            isOnline = true
        ),
        FriendContact(
            id = "dir_prof_3",
            name = "Prof. Claudia Navarro",
            role = UserRoles.PROFESOR,
            avatarId = 8,
            avatarColorHex = "#9D4EDD",
            lastMessage = "Coordinemos la sala de profesores.",
            isOnline = false
        ),
        FriendContact(
            id = "dir_prof_4",
            name = "Prof. Daniel Soto",
            role = UserRoles.PROFESOR,
            avatarId = 4,
            avatarColorHex = "#00FFFF",
            lastMessage = "Reporté el daño en el proyector.",
            isOnline = true
        ),
        FriendContact(
            id = "dir_prof_5",
            name = "Prof. Marcela Ríos",
            role = UserRoles.PROFESOR,
            avatarId = 1,
            avatarColorHex = "#FFCC00",
            lastMessage = "Aprobado el reporte de mantenimiento.",
            isOnline = true
        ),
        FriendContact(
            id = "dir_prof_6",
            name = "Prof. Jorge Rueda",
            role = UserRoles.PROFESOR,
            avatarId = 7,
            avatarColorHex = "#39FF14",
            lastMessage = "Revisamos los equipos del bloque B.",
            isOnline = false
        ),
        FriendContact(
            id = "dir_serv_1",
            name = "Mantenimiento General",
            role = UserRoles.SERVICIOS_GENERALES,
            avatarId = 5,
            avatarColorHex = "#34C759",
            lastMessage = "Entendido, acudiremos al bloque C.",
            isOnline = true
        )
    )

    private val _allFriendsPool = MutableStateFlow<List<FriendContact>>(
        listOf(
            institutionalDirectoryPeers[0], // Prof. Roberto Méndez
            institutionalDirectoryPeers[3], // Prof. Daniel Soto
            institutionalDirectoryPeers[6]  // Mantenimiento General
        )
    )

    val friendsList: StateFlow<List<FriendContact>> = combine(
        _allFriendsPool,
        _currentUser
    ) { pool, user ->
        val userGroup = normalizeRoleGroup(user?.role)
        val selfName = user?.fullName?.trim()?.lowercase(Locale.ROOT)
        pool.filter { contact ->
            normalizeRoleGroup(contact.role) == userGroup &&
                (selfName == null || contact.name.trim().lowercase(Locale.ROOT) != selfName)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val availablePeersToAdd: StateFlow<List<FriendContact>> = combine(
        repository.allUsers,
        friendsList,
        _currentUser
    ) { dbUsers, activeFriends, currentUser ->
        val userGroup = normalizeRoleGroup(currentUser?.role)
        val selfUsername = currentUser?.username?.trim()?.lowercase(Locale.ROOT)
        val selfName = currentUser?.fullName?.trim()?.lowercase(Locale.ROOT)
        val activeNames = activeFriends.map { it.name.trim().lowercase(Locale.ROOT) }.toSet()

        val dbContacts = dbUsers
            .filter { normalizeRoleGroup(it.role) == userGroup }
            .filter { u ->
                val uname = u.username.trim().lowercase(Locale.ROOT)
                val fname = u.fullName.trim().lowercase(Locale.ROOT)
                uname != selfUsername && fname != selfName
            }
            .map { u ->
                FriendContact(
                    id = "user_${u.id}_${u.username}",
                    name = u.fullName,
                    role = normalizeRoleGroup(u.role),
                    avatarId = u.avatarId,
                    avatarColorHex = u.avatarColorHex,
                    lastMessage = "Miembro de $userGroup • Disponible para chatear",
                    isOnline = true
                )
            }

        val directorySameGroup = institutionalDirectoryPeers
            .filter { normalizeRoleGroup(it.role) == userGroup }
            .filter { peer ->
                val pname = peer.name.trim().lowercase(Locale.ROOT)
                selfName == null || pname != selfName
            }

        (dbContacts + directorySameGroup)
            .distinctBy { it.name.trim().lowercase(Locale.ROOT) }
            .filter { !activeNames.contains(it.name.trim().lowercase(Locale.ROOT)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _activeChatFriend = MutableStateFlow<FriendContact?>(null)
    val activeChatFriend: StateFlow<FriendContact?> = _activeChatFriend.asStateFlow()

    private val _chatMessagesMap = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val chatMessagesMap: StateFlow<Map<String, List<ChatMessage>>> = _chatMessagesMap.asStateFlow()

    fun addPeerToChat(peer: FriendContact): Pair<Boolean, String?> {
        val currentGroup = normalizeRoleGroup(_currentUser.value?.role)
        if (normalizeRoleGroup(peer.role) != currentGroup) {
            return Pair(
                false,
                "Solo puedes agregar y chatear con personas de tu mismo grupo ($currentGroup)."
            )
        }
        val normalizedPeer = peer.copy(role = currentGroup)
        val existing = _allFriendsPool.value.find {
            it.name.equals(normalizedPeer.name, ignoreCase = true) &&
                normalizeRoleGroup(it.role) == currentGroup
        }
        val target = existing ?: normalizedPeer.also {
            _allFriendsPool.value = listOf(it) + _allFriendsPool.value
        }
        openChatWith(target)
        return Pair(true, null)
    }

    fun openChatWith(friend: FriendContact): Boolean {
        val currentGroup = normalizeRoleGroup(_currentUser.value?.role)
        if (normalizeRoleGroup(friend.role) != currentGroup) {
            return false
        }
        _activeChatFriend.value = friend
        if (!_chatMessagesMap.value.containsKey(friend.id)) {
            val initialMsg = ChatMessage(
                senderId = friend.id,
                text = friend.lastMessage,
                timestamp = "Hace 5 min",
                isFromMe = false
            )
            _chatMessagesMap.value = _chatMessagesMap.value + (friend.id to listOf(initialMsg))
        }
        return true
    }

    fun closeChat() {
        _activeChatFriend.value = null
    }

    fun sendChatMessage(friendId: String, text: String): Pair<Boolean, String?> {
        val target = _allFriendsPool.value.find { it.id == friendId }
        val currentGroup = normalizeRoleGroup(_currentUser.value?.role)
        if (target != null && normalizeRoleGroup(target.role) != currentGroup) {
            return Pair(false, "Solo puedes chatear con personas de tu mismo grupo ($currentGroup).")
        }
        val check = ContentModerator.checkContent(text)
        if (!check.first) {
            return check
        }
        val trimmed = text.trim()
        val userMsg = ChatMessage(
            senderId = "me",
            text = trimmed,
            timestamp = "Hace un momento",
            isFromMe = true
        )

        val currentList = _chatMessagesMap.value[friendId] ?: emptyList()
        _chatMessagesMap.value = _chatMessagesMap.value + (friendId to (currentList + userMsg))

        _allFriendsPool.value = _allFriendsPool.value.map { f ->
            if (f.id == friendId) f.copy(lastMessage = "Tú: $trimmed") else f
        }

        return Pair(true, null)
    }
}
