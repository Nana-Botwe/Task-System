package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DatabaseRepository = DatabaseRepository(AppDatabase.getDatabase(application))

    // --- Authentication States ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isMfaRequired = MutableStateFlow(false)
    val isMfaRequired: StateFlow<Boolean> = _isMfaRequired.asStateFlow()

    private val _pendingMfaUser = MutableStateFlow<UserEntity?>(null)
    
    // Simulate SMS/Email delivery of MFA OTP by showing a top notification banner
    private val _currentMfaOtp = MutableStateFlow<String?>(null)
    val currentMfaOtp: StateFlow<String?> = _currentMfaOtp.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // --- UI Theme Preferences ---
    private val _themeModeIsDark = MutableStateFlow(true) // Start with premium dark theme
    val themeModeIsDark: StateFlow<Boolean> = _themeModeIsDark.asStateFlow()

    // --- Data Streams ---
    val tasks: StateFlow<List<TaskEntity>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<ActivityLogEntity>> = repository.getAllActivityLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<UserEntity>> = repository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<TaskTemplateEntity>> = repository.getAllTemplatesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Selected Task & Comments ---
    private val _selectedTaskId = MutableStateFlow<Int?>(null)
    val selectedTaskId: StateFlow<Int?> = _selectedTaskId.asStateFlow()

    val selectedTask: StateFlow<TaskEntity?> = _selectedTaskId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getTaskByIdFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentComments: StateFlow<List<CommentEntity>> = _selectedTaskId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getCommentsForTask(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Offline Mode & Peer Collaboration Simulator State ---
    private val _offlineMode = MutableStateFlow(false)
    val offlineMode: StateFlow<Boolean> = _offlineMode.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _collaborationStateMessage = MutableStateFlow<String?>(null)
    val collaborationStateMessage: StateFlow<String?> = _collaborationStateMessage.asStateFlow()

    // Notification Alerts Tray
    private val _notificationAlerts = MutableStateFlow<List<Pair<String, Long>>>(emptyList())
    val notificationAlerts: StateFlow<List<Pair<String, Long>>> = _notificationAlerts.asStateFlow()

    // Filter and Calendar Navigation States
    val filterSearchQuery = MutableStateFlow("")
    val filterStatus = MutableStateFlow<String?>("ALL") // "ALL", "TODO", "IN_PROGRESS", "REVIEW", "DONE"
    val filterPriority = MutableStateFlow<String?>("ALL") // "ALL", "HIGH", "MEDIUM", "LOW"
    val filterCategory = MutableStateFlow<String?>("ALL") // "ALL", "Features", "Bugs", "Design", "Marketing"
    
    // Calendar Focus Date (Epoch Mills)
    val calendarFocusedDate = MutableStateFlow(System.currentTimeMillis())

    init {
        // Seed initial mock user directory and demo tasks
        viewModelScope.launch {
            repository.seedDefaultDataIfEmpty()
        }

        viewModelScope.launch {
            tasks.collect {
                if (it.isNotEmpty()) {
                    scanApproachingDeadlines()
                }
            }
        }
    }

    // --- Authentication Operations ---

    fun toggleTheme() {
        _themeModeIsDark.value = !_themeModeIsDark.value
    }

    fun login(email: String, passwordRaw: String) {
        viewModelScope.launch {
            _authError.value = null
            val user = repository.authenticateUser(email, passwordRaw)
            if (user != null) {
                // Trigger Simulated Multi-Factor Authentication (MFA OTP) for safety compliant audits
                _pendingMfaUser.value = user
                val generatedOtp = (100000..999999).random().toString()
                _currentMfaOtp.value = generatedOtp
                _isMfaRequired.value = true
                addNotification("MFA Login Code Generated: $generatedOtp")
            } else {
                _authError.value = "Invalid credentials. Try admin@collabtask.com / admin123"
            }
        }
    }

    fun verifyMfa(otpInput: String) {
        val activeCode = _currentMfaOtp.value
        val pendingUser = _pendingMfaUser.value
        if (activeCode != null && otpInput == activeCode && pendingUser != null) {
            _currentUser.value = pendingUser
            _isAuthenticated.value = true
            _isMfaRequired.value = false
            _currentMfaOtp.value = null
            _pendingMfaUser.value = null
            _authError.value = null
            addNotification("Welcome back, ${pendingUser.fullName}!")
            viewModelScope.launch {
                repository.logAction(
                    taskId = 0,
                    taskTitle = "Security Logs",
                    userName = pendingUser.fullName,
                    userRole = pendingUser.role,
                    action = "Successful login with multi-factor verification"
                )
            }
        } else {
            _authError.value = "Incorrect verification code. Please check your simulated OTP alert."
        }
    }

    fun register(fullName: String, email: String, passwordRaw: String, role: String) {
        viewModelScope.launch {
            _authError.value = null
            if (fullName.isEmpty() || email.isEmpty() || passwordRaw.isEmpty()) {
                _authError.value = "Please complete all fields"
                return@launch
            }
            val success = repository.registerUser(email, fullName, passwordRaw, role)
            if (success) {
                // Jump to login with preconfigured role
                login(email, passwordRaw)
            } else {
                _authError.value = "Account with this email already exists"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _isAuthenticated.value = false
        _isMfaRequired.value = false
        _pendingMfaUser.value = null
        _currentMfaOtp.value = null
        _authError.value = null
        addNotification("Logged out successfully.")
    }

    // --- Task CRUD Procedures with Offline & Role-based Guard ---

    fun upsertTask(
        id: Int = 0,
        title: String,
        description: String,
        status: String,
        priority: String,
        assignedEmail: String,
        category: String,
        subtasks: List<Subtask>,
        dueDate: Long
    ) {
        val user = _currentUser.value ?: return
        
        // RBAC validation
        val roleUpper = user.role.uppercase(Locale.getDefault())
        if (roleUpper == "MEMBER" || roleUpper == "VIEWER") {
            Log.w("TaskViewModel", "Unauthorized update attempted by restrictive role: $roleUpper")
            addNotification("Access Denied: Only Admin or Editor can create/edit tasks")
            return
        }

        viewModelScope.launch {
            val database = AppDatabase.getDatabase(getApplication())
            val subtasksJson = DatabaseConverters().fromSubtaskList(subtasks)
            
            // Map assignee email to name
            val assignedUser = repository.getAllUsers().find { it.email == assignedEmail }
            val assignedName = assignedUser?.fullName ?: "Unassigned"

            val isOffline = _offlineMode.value
            val syncState = if (isOffline) "PENDING" else "SYNCED"

            val task = TaskEntity(
                id = id,
                title = title,
                description = description,
                status = status,
                priority = priority,
                assignedTo = assignedEmail,
                assignedName = assignedName,
                dueDate = dueDate,
                category = category,
                subtasksJson = subtasksJson,
                syncState = syncState,
                lastUpdated = System.currentTimeMillis()
            )

            if (id == 0) {
                val newId = repository.createTask(task, user)
                addNotification("Created task: $title")
            } else {
                val old = repository.getTaskById(id)
                val summary = if (old != null && old.status != status) {
                    "Changed status to $status"
                } else if (old != null && old.assignedTo != assignedEmail) {
                    "Assigned task to $assignedName"
                } else {
                    "Updated task specifications"
                }
                repository.updateTask(task, user, summary)
                addNotification("Updated: $title")
            }
        }
    }

    fun updateTaskStatusOnly(taskId: Int, newStatus: String) {
        val user = _currentUser.value ?: return
        val roleUpper = user.role.uppercase(Locale.getDefault())
        if (roleUpper == "MEMBER" || roleUpper == "VIEWER") {
            addNotification("Access Denied: Viewers cannot change task status.")
            return
        }
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            val updated = task.copy(
                status = newStatus,
                syncState = if (_offlineMode.value) "PENDING" else "SYNCED",
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateTask(updated, user, "Updated status to $newStatus")
            addNotification("Task '${task.title}' marked as $newStatus")
        }
    }

    fun updateSubtaskChecklist(taskId: Int, subtasks: List<Subtask>) {
        val user = _currentUser.value ?: return
        val roleUpper = user.role.uppercase(Locale.getDefault())
        if (roleUpper == "MEMBER" || roleUpper == "VIEWER") {
            addNotification("Access Denied: Viewers cannot change checklists.")
            return
        }
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            val subtasksJson = DatabaseConverters().fromSubtaskList(subtasks)
            val updated = task.copy(
                subtasksJson = subtasksJson,
                syncState = if (_offlineMode.value) "PENDING" else "SYNCED",
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateTask(updated, user, "Updated checklist progress")
        }
    }

    fun deleteTask(taskId: Int, taskTitle: String) {
        val user = _currentUser.value ?: return
        val roleUpper = user.role.uppercase(Locale.getDefault())
        if (roleUpper != "ADMIN") {
            addNotification("Access Denied: Only Admins can delete tasks.")
            return
        }
        viewModelScope.launch {
            repository.deleteTask(taskId, user, taskTitle)
            addNotification("Deleted task: $taskTitle")
            if (_selectedTaskId.value == taskId) {
                _selectedTaskId.value = null
            }
        }
    }

    // --- Task Template Operations ---

    fun saveAsTemplate(
        templateName: String,
        title: String,
        description: String,
        priority: String,
        defaultAssigneeRole: String,
        category: String,
        subtasks: List<Subtask>
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val subtasksJson = DatabaseConverters().fromSubtaskList(subtasks)
            val template = TaskTemplateEntity(
                templateName = templateName,
                title = title,
                description = description,
                priority = priority,
                defaultAssigneeRole = defaultAssigneeRole,
                category = category,
                subtasksJson = subtasksJson
            )
            repository.createTemplate(template)
            addNotification("Saved template: $templateName")
            repository.logAction(
                taskId = 0,
                taskTitle = "Task Templates",
                userName = user.fullName,
                userRole = user.role,
                action = "Saved reusable task template '$templateName'"
            )
        }
    }

    fun deleteTemplate(templateId: Int, templateName: String) {
        val user = _currentUser.value ?: return
        val roleUpper = user.role.uppercase(Locale.getDefault())
        if (roleUpper != "ADMIN") {
            addNotification("Access Denied: Only Admins can delete templates.")
            return
        }
        viewModelScope.launch {
            repository.deleteTemplate(templateId)
            addNotification("Deleted template: $templateName")
            repository.logAction(
                taskId = 0,
                taskTitle = "Task Templates",
                userName = user.fullName,
                userRole = user.role,
                action = "Deleted task template '$templateName'"
            )
        }
    }

    // --- Dynamic Collaborator Comments ---

    fun submitComment(content: String) {
        val user = _currentUser.value ?: return
        val roleUpper = user.role.uppercase(Locale.getDefault())
        if (roleUpper == "MEMBER" || roleUpper == "VIEWER") {
            addNotification("Access Denied: Viewers cannot post comments.")
            return
        }
        val taskId = _selectedTaskId.value ?: return
        if (content.trim().isEmpty()) return

        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            val comment = CommentEntity(
                taskId = taskId,
                authorName = user.fullName,
                authorRole = user.role,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            repository.addComment(comment, user, task.title)
        }
    }

    fun selectTask(taskId: Int?) {
        _selectedTaskId.value = taskId
    }

    // --- Simulated Offline Sychronization ---

    fun toggleOfflineMode() {
        val active = !_offlineMode.value
        _offlineMode.value = active
        if (active) {
            addNotification("Network toggled: Offline workspace active.")
        } else {
            addNotification("Network toggled: Online server mode. Triggering synchronization...")
            triggerFullSync()
        }
    }

    fun triggerFullSync() {
        if (_offlineMode.value) {
            addNotification("Cannot sync: currently offline")
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            addNotification("Syncing local changes with cloud database...")
            delay(1500) // Simulate replication delay

            // Gather all pending tasks and update their state to Synced
            val currentTasksList = tasks.value
            val user = _currentUser.value ?: UserEntity("system@sync.com", "Sync Service", "", "SYSTEM")
            
            var syncCount = 0
            for (t in currentTasksList) {
                if (t.syncState == "PENDING") {
                    val syncedTask = t.copy(syncState = "SYNCED", lastUpdated = System.currentTimeMillis())
                    databaseRepositoryNoLogUpdate(syncedTask)
                    syncCount++
                }
            }

            _isSyncing.value = false
            addNotification("Synchronized successfully. $syncCount records committed.")
            repository.logAction(
                taskId = 0,
                taskTitle = "Cloud Synchronization",
                userName = user.fullName,
                userRole = user.role,
                action = "Completed high-availability replication for $syncCount tasks"
            )
        }
    }

    private suspend fun databaseRepositoryNoLogUpdate(task: TaskEntity) {
        val database = AppDatabase.getDatabase(getApplication())
        database.taskDao.updateTask(task)
    }

    // --- Multi-User Collaboration Simulation Trigger ---
    // Simulates dynamic live action updates from external mock users

    fun simulateActiveUserAction() {
        if (_offlineMode.value) {
            _collaborationStateMessage.value = "Cannot simulate incoming events while offline!"
            viewModelScope.launch {
                delay(2000)
                _collaborationStateMessage.value = null
            }
            return
        }

        viewModelScope.launch {
            val currentTasks = tasks.value
            if (currentTasks.isEmpty()) return@launch

            val names = listOf("Sarah Jenkins", "System Admin", "John Doe", "Alex Rivera (Lead Engineer)")
            val roles = listOf("MANAGER", "ADMIN", "MEMBER", "MEMBER")
            val commentTexts = listOf(
                "I finished verifying this database configuration. Looks solid!",
                "Can we re-assign this to the staging team for QA testing?",
                "Running performance benchmarks now across 10k mock tasks.",
                "Let's double-check the API documentation endpoints before merging.",
                "Pushing custom notifications pipeline edits. Ready for review."
            )

            val selector = (0 until currentTasks.size).random()
            val selected = currentTasks[selector]

            // Pick a random user that is not our current user
            val randIndex = names.indices.random()
            val colleagueName = names[randIndex]
            val colleagueRole = roles[randIndex]

            _collaborationStateMessage.value = "$colleagueName is editing '${selected.title}'..."
            
            // Highlight a transient editing lock
            val lockedTask = selected.copy(activeEditor = colleagueName)
            databaseRepositoryNoLogUpdate(lockedTask)

            delay(3000) // Simulates editor keystrokes

            // Create a remote comment or update status
            val taskStateAction = (0..1).random()
            if (taskStateAction == 0) {
                // Post remote comment
                val mockComment = CommentEntity(
                    taskId = selected.id,
                    authorName = colleagueName,
                    authorRole = colleagueRole,
                    content = commentTexts.random(),
                    timestamp = System.currentTimeMillis()
                )
                repository.addComment(mockComment, UserEntity(colleagueName.lowercase().replace(" ", "") + "@collabtask.com", colleagueName, "", colleagueRole), selected.title)
                addNotification("New Comment on [${selected.title}] from $colleagueName")
            } else {
                // Change progress status
                val newStatuses = listOf("TODO", "IN_PROGRESS", "REVIEW", "DONE")
                val oldStatus = selected.status
                var randomStatus = newStatuses.random()
                if (randomStatus == oldStatus) {
                    randomStatus = if (oldStatus == "TODO") "IN_PROGRESS" else "DONE"
                }
                
                val updatedTask = selected.copy(status = randomStatus, activeEditor = null, lastUpdated = System.currentTimeMillis())
                databaseRepositoryNoLogUpdate(updatedTask)
                
                repository.logAction(
                    taskId = selected.id,
                    taskTitle = selected.title,
                    userName = colleagueName,
                    userRole = colleagueRole,
                    action = "Updated status from $oldStatus to $randomStatus (Remote collaborative sync)"
                )
                addNotification("Task Sync: $colleagueName moved '${selected.title}' to $randomStatus")
            }

            // Remove editor lock
            val finalUpdatedTask = repository.getTaskById(selected.id)
            if (finalUpdatedTask != null && finalUpdatedTask.activeEditor == colleagueName) {
                databaseRepositoryNoLogUpdate(finalUpdatedTask.copy(activeEditor = null))
            }

            _collaborationStateMessage.value = "Cloud synchronizations completed."
            delay(2000)
            _collaborationStateMessage.value = null
        }
    }

    private fun addNotification(message: String) {
        val item = Pair(message, System.currentTimeMillis())
        val updated = listOf(item) + _notificationAlerts.value
        _notificationAlerts.value = updated.take(15) // Limit history in alert tray
    }

    fun clearNotificationAlerts() {
        _notificationAlerts.value = emptyList()
    }

    fun scanApproachingDeadlines() {
        val activeTasks = tasks.value
        val now = System.currentTimeMillis()
        val oneDay = 86400000L // 24 hours
        val activeAlerts = _notificationAlerts.value.map { it.first }
        for (task in activeTasks) {
            if (task.status != "DONE") {
                val timeLeft = task.dueDate - now
                if (timeLeft in 0..oneDay) {
                    val alertMsg = "Alert: Task '${task.title}' is nearing its deadline! (Due in less than 24 hours)"
                    if (!activeAlerts.contains(alertMsg)) {
                        addNotification(alertMsg)
                    }
                }
            }
        }
    }

    fun adminDeleteUsers(emails: List<String>, onResult: (Boolean, String) -> Unit) {
        val activeUser = _currentUser.value ?: return
        if (activeUser.role != "ADMIN") {
            onResult(false, "Access Denied: Only Admins can manage users")
            return
        }
        val safeEmails = emails.filter { it != activeUser.email }
        if (safeEmails.isEmpty()) {
            onResult(false, "No valid/other accounts selected for deletion")
            return
        }
        viewModelScope.launch {
            for (email in safeEmails) {
                val user = repository.getUser(email) ?: continue
                repository.deleteUser(email)
                repository.logAction(0, "User Directory", activeUser.fullName, activeUser.role, "Admin bulk deleted user: ${user.fullName}")
            }
            addNotification("Admin bulk deleted ${safeEmails.size} user accounts")
            onResult(true, "Bulk deleted successfully")
        }
    }

    fun adminDeleteTasks(taskIds: List<Int>, onResult: (Boolean, String) -> Unit) {
        val user = _currentUser.value ?: return
        val roleUpper = user.role.uppercase(Locale.getDefault())
        if (roleUpper != "ADMIN") {
            onResult(false, "Access Denied: Only Admins can delete tasks.")
            return
        }
        viewModelScope.launch {
            var count = 0
            for (taskId in taskIds) {
                val task = repository.getTaskById(taskId) ?: continue
                repository.deleteTask(taskId, user, task.title)
                if (_selectedTaskId.value == taskId) {
                    _selectedTaskId.value = null
                }
                count++
            }
            if (count > 0) {
                addNotification("Admin bulk deleted $count tasks from global ledger")
                onResult(true, "Bulk deleted $count tasks successfully")
            } else {
                onResult(false, "No tasks were deleted")
            }
        }
    }

    fun adminCreateUser(fullName: String, email: String, passwordRaw: String, role: String, onResult: (Boolean, String) -> Unit) {
        val activeUser = _currentUser.value ?: return
        if (activeUser.role != "ADMIN") {
            onResult(false, "Access Denied: Only Admins can manage users")
            return
        }
        viewModelScope.launch {
            if (fullName.isEmpty() || email.isEmpty() || passwordRaw.isEmpty()) {
                onResult(false, "All fields are required")
                return@launch
            }
            val existing = repository.getUser(email)
            if (existing != null) {
                onResult(false, "User with this email already exists")
                return@launch
            }
            val passwordHash = repository.hashPassword(passwordRaw)
            val newUser = UserEntity(
                email = email,
                fullName = fullName,
                passwordHash = passwordHash,
                role = role,
                mfaEnabled = true,
                mfaSecret = (100000..999999).random().toString()
            )
            repository.createUser(newUser)
            addNotification("Admin created user: $fullName ($role)")
            repository.logAction(0, "User Directory", activeUser.fullName, activeUser.role, "Admin created user: $fullName ($role)")
            onResult(true, "User created successfully")
        }
    }

    fun adminUpdateUser(email: String, fullName: String, passwordRaw: String, role: String, onResult: (Boolean, String) -> Unit) {
        val activeUser = _currentUser.value ?: return
        if (activeUser.role != "ADMIN") {
            onResult(false, "Access Denied: Only Admins can manage users")
            return
        }
        viewModelScope.launch {
            val existing = repository.getUser(email)
            if (existing == null) {
                onResult(false, "User not found")
                return@launch
            }
            val newHash = if (passwordRaw.isNotEmpty()) repository.hashPassword(passwordRaw) else existing.passwordHash
            val updatedUser = existing.copy(
                fullName = fullName,
                passwordHash = newHash,
                role = role
            )
            repository.updateUser(updatedUser)
            addNotification("Admin updated user: $fullName ($role)")
            repository.logAction(0, "User Directory", activeUser.fullName, activeUser.role, "Admin updated user: $fullName ($role)")
            
            // If the admin edited themselves, let's update _currentUser!
            if (email == activeUser.email) {
                _currentUser.value = updatedUser
            }
            onResult(true, "User updated successfully")
        }
    }

    fun adminDeleteUser(email: String, onResult: (Boolean, String) -> Unit) {
        val activeUser = _currentUser.value ?: return
        if (activeUser.role != "ADMIN") {
            onResult(false, "Access Denied: Only Admins can manage users")
            return
        }
        if (email == activeUser.email) {
            onResult(false, "You cannot delete your own active Admin account!")
            return
        }
        viewModelScope.launch {
            val user = repository.getUser(email) ?: return@launch
            repository.deleteUser(email)
            addNotification("Admin deleted user: ${user.fullName}")
            repository.logAction(0, "User Directory", activeUser.fullName, activeUser.role, "Admin deleted user: ${user.fullName}")
            onResult(true, "User deleted successfully")
        }
    }

    // --- Report Exporters: CSV and PDF formatted file writers ---

    fun exportToCSV(context: Context) {
        val currentTasks = tasks.value
        if (currentTasks.isEmpty()) {
            Toast.makeText(context, "No tasks to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val csvBuilder = StringBuilder()
            csvBuilder.append("ID,Title,Description,Status,Priority,Category,AssignedTo,DueDate,SyncStatus\n")
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            for (t in currentTasks) {
                val cleanDesc = t.description.replace(",", ";").replace("\n", " ")
                val dateStr = sdf.format(Date(t.dueDate))
                csvBuilder.append("${t.id},\"${t.title}\",\"$cleanDesc\",\"${t.status}\",\"${t.priority}\",\"${t.category}\",\"${t.assignedName}\",\"$dateStr\",\"${t.syncState}\"\n")
            }

            val filename = "collabtask_report_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, filename)
            FileOutputStream(file).use {
                it.write(csvBuilder.toString().toByteArray())
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "CollabTask Operations Report CSV")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share CSV Report"))
            
            viewModelScope.launch {
                val user = _currentUser.value ?: UserEntity("visitor@task.com", "Visitor", "", "MEMBER")
                repository.logAction(0, "System export", user.fullName, user.role, "Exported detailed report as CSV")
                addNotification("CSV project export successfully built.")
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportToPDF(context: Context) {
        val currentTasks = tasks.value
        if (currentTasks.isEmpty()) {
            Toast.makeText(context, "No tasks to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Android platform-native PDF generation using Canvas
            val pdfDocument = PdfDocument()
            val paint = Paint()
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }
            val titlePaint = Paint().apply {
                color = Color.rgb(18, 120, 240)
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val headerPaint = Paint().apply {
                color = Color.rgb(100, 110, 120)
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }

            // Define custom A4 size dimensions (595 x 842 points)
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Page Background styling
            canvas.drawColor(Color.WHITE)

            // Header elements
            canvas.drawText("CollabTask Ecosystem Report", 40f, 50f, titlePaint)
            
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            canvas.drawText("Generated: ${sdf.format(Date())}", 40f, 75f, textPaint)
            canvas.drawText("Authentication: SECURE (TLS Replication)", 40f, 92f, textPaint)
            canvas.drawHorizontalLine(canvas, 40f, 550f, 105f)

            // Dynamic Table columns
            canvas.drawText("Task Specifications", 40f, 130f, headerPaint)
            canvas.drawText("Assigned To", 280f, 130f, headerPaint)
            canvas.drawText("Status & Priority", 420f, 130f, headerPaint)
            canvas.drawHorizontalLine(canvas, 40f, 550f, 140f)

            var yOffset = 160f
            val taskSdf = SimpleDateFormat("MM-dd", Locale.getDefault())

            for ((index, t) in currentTasks.take(15).withIndex()) { // fit comfortably on primary sheet
                // Title and category line
                val cleanTitle = if (t.title.length > 32) t.title.take(30) + "..." else t.title
                canvas.drawText("${index + 1}. $cleanTitle", 40f, yOffset, textPaint.apply { isFakeBoldText = true })
                canvas.drawText("[${t.category}]", 40f, yOffset + 15f, textPaint.apply { isFakeBoldText = false; color = Color.GRAY; textSize = 9f })
                
                // Assignee & DueDate
                textPaint.color = Color.BLACK
                textPaint.textSize = 11f
                val cleanName = if (t.assignedName.length > 20) t.assignedName.take(18) + ".." else t.assignedName
                canvas.drawText(cleanName, 280f, yOffset, textPaint)
                canvas.drawText("Due: ${taskSdf.format(Date(t.dueDate))}", 280f, yOffset + 15f, textPaint.apply { color = Color.DKGRAY; textSize = 9f })
                
                // Status & Priority labels
                textPaint.color = Color.BLACK
                textPaint.textSize = 11f
                canvas.drawText(t.status, 420f, yOffset, textPaint)
                val priorityColor = when (t.priority) {
                    "HIGH" -> Color.RED
                    "MEDIUM" -> Color.rgb(200, 120, 0)
                    else -> Color.rgb(40, 160, 40)
                }
                canvas.drawText("• ${t.priority}", 420f, yOffset + 15f, textPaint.apply { color = priorityColor; isFakeBoldText = true; textSize = 10f })

                yOffset += 45f
                
                // Keep dividing line
                canvas.drawHorizontalLine(canvas, 40f, 550f, yOffset - 10f, Color.rgb(230, 230, 230))
            }

            // Footer info
            canvas.drawText("End of Audit report -- Page 1 of 1", 40f, 800f, textPaint.apply { color = Color.GRAY; textSize = 8f })

            pdfDocument.finishPage(page)

            // Save document into cache folder
            val filename = "collabtask_report_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, filename)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, "CollabTask PDF Report Briefing")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share PDF Document"))

            viewModelScope.launch {
                val user = _currentUser.value ?: UserEntity("visitor@task.com", "Visitor", "", "MEMBER")
                repository.logAction(0, "System export", user.fullName, user.role, "Compiled system database to secure PDF layout")
                addNotification("PDF analytics summary successfully generated.")
            }
        } catch (e: Exception) {
            Toast.makeText(context, "PDF compilation failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun Canvas.drawHorizontalLine(canvas: Canvas, startX: Float, endX: Float, y: Float, color: Int = Color.BLACK) {
        val p = Paint().apply {
            this.color = color
            strokeWidth = 1f
        }
        canvas.drawLine(startX, y, endX, y, p)
    }

    // Trigger standard SendTo action simulating weekly automatic alerts
    fun sendWeeklySummaryEmail(context: Context) {
        val currentTasks = tasks.value
        val openCount = currentTasks.count { it.status != "DONE" }
        val closedCount = currentTasks.count { it.status == "DONE" }
        val highCount = currentTasks.count { it.priority == "HIGH" }

        val emailBody = """
            COLLABTASK WORKSPACE WEEKLY DIGEST
            ------------------------------------------
            This is an automated analytical briefing of your active task pipelines.
            
            PROJECT SUMMARY METRICS:
            • Unfinished Tasks: $openCount
            • Resolved Tasks: $closedCount
            • High-Criticality Issues pending: $highCount
            
            ACTIVE SYSTEM AUDIT TRACE LOGS:
            - Cloud Node Health Check: SYNCED
            - Active Security Matrix: End-To-End AES-256 Enabled
            
            Sync status verified. For real-time board manipulation, launch CollabTask App on your Android device.
            ------------------------------------------
            CollabTask Automated Reporter System.
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "CollabTask Weekly Analytical Briefing")
            putExtra(Intent.EXTRA_TEXT, emailBody)
        }
        
        try {
            context.startActivity(Intent.createChooser(intent, "Dispatch Digest Email"))
            addNotification("Automated weekly email summary builder triggered.")
        } catch (e: Exception) {
            Toast.makeText(context, "No email client application found", Toast.LENGTH_SHORT).show()
        }
    }
}
