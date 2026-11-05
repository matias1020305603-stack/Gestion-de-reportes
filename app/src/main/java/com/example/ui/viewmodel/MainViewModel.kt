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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
            if (role == com.example.data.model.UserRoles.SERVICIOS_GENERALES) {
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
            _isAuthDialogOpen.value = false
            onResult(true, "Registro exitoso como $role")
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    fun quickLoginAs(role: String) {
        viewModelScope.launch {
            val username = when (role) {
                com.example.data.model.UserRoles.SERVICIOS_GENERALES -> "servicios"
                com.example.data.model.UserRoles.PROFESOR -> "profesor"
                else -> "estudiante"
            }
            val user = repository.findUserByUsername(username)
            if (user != null) {
                _currentUser.value = user
                _isAuthDialogOpen.value = false
            } else {
                val tempUser = UserEntity(
                    username = username,
                    fullName = when (role) {
                        com.example.data.model.UserRoles.SERVICIOS_GENERALES -> "Mario Gómez (Servicios Generales)"
                        com.example.data.model.UserRoles.PROFESOR -> "Prof. Andrés Castro"
                        else -> "Camila Torres (Estudiante)"
                    },
                    password = "123",
                    role = role
                )
                val id = repository.registerUser(tempUser)
                _currentUser.value = tempUser.copy(id = id)
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

    // Report Actions
    fun openCreateReportDialog() {
        _isReportDialogOpen.value = true
    }

    fun closeReportDialog() {
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
        reportedBy: String
    ) {
        viewModelScope.launch {
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
                    status = ReportStatus.ABIERTO.name,
                    location = if (location.isBlank()) "Área de Planta" else location.trim(),
                    reportedBy = userAuthor
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
                            status = ReportStatus.ABIERTO.name,
                            authorName = userAuthor,
                            authorRole = _currentUser.value?.role ?: "Estudiante",
                            authorEmail = _currentUser.value?.email ?: authUser.email ?: ""
                        )
                    )
                }
            } catch (_: Exception) {}

            closeReportDialog()
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

    fun deleteReport(report: ReportEntity) {
        viewModelScope.launch {
            repository.deleteReport(report)
            if (_selectedReportDetail.value?.id == report.id) {
                _selectedReportDetail.value = null
            }
        }
    }

    fun clearAllReports() {
        viewModelScope.launch {
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
            _currentUser.value = user
        }
    }
}
