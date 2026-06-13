package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest

class DatabaseRepository(private val database: AppDatabase) {

    private val userDao = database.userDao
    private val taskDao = database.taskDao
    private val commentDao = database.commentDao
    private val logDao = database.activityLogDao
    private val templateDao = database.taskTemplateDao

    // --- Authentication & User Operations ---

    fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            password // Fallback to plain if digest fails for some reason
        }
    }

    suspend fun registerUser(email: String, fullName: String, passwordRaw: String, role: String): Boolean {
        val existing = userDao.getUserByEmail(email)
        if (existing != null) return false // Already exists

        // Generate mock MFA secret if not exist
        val mfaSecret = (100000..999999).random().toString()
        val user = UserEntity(
            email = email,
            fullName = fullName,
            passwordHash = hashPassword(passwordRaw),
            role = role,
            mfaEnabled = true, // Force MFA simulator for extra security compliance
            mfaSecret = mfaSecret
        )
        userDao.insertUser(user)

        // Log the user self-registration
        logAction(
            taskId = 0,
            taskTitle = "User Directory",
            userName = fullName,
            userRole = role,
            action = "Registered new account (MFA enabled)"
        )
        return true
    }

    suspend fun authenticateUser(email: String, passwordRaw: String): UserEntity? {
        val user = userDao.getUserByEmail(email) ?: return null
        val targetHash = hashPassword(passwordRaw)
        return if (user.passwordHash == targetHash) user else null
    }

    suspend fun getUser(email: String): UserEntity? = userDao.getUserByEmail(email)

    fun getAllUsersFlow(): Flow<List<UserEntity>> = userDao.getAllUsersFlow()
    
    suspend fun getAllUsers(): List<UserEntity> = userDao.getAllUsers()

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(email: String) {
        userDao.deleteUserByEmail(email)
    }

    suspend fun createUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    // Seed default admin/users if database is empty
    suspend fun seedDefaultDataIfEmpty() {
        if (userDao.getAllUsers().isEmpty()) {
            registerUser("admin@collabtask.com", "System Admin", "admin123", "ADMIN")
            registerUser("manager@collabtask.com", "Sarah Jenkins", "manager123", "MANAGER")
            registerUser("member@collabtask.com", "John Doe", "member123", "MEMBER")

            // Seed a couple of default tasks to showcase the app
            val adminUser = userDao.getUserByEmail("admin@collabtask.com")!!
            val initialTasks = listOf(
                TaskEntity(
                    title = "Setup Production Server Replication",
                    description = "Establish a multi-region PostgreSQL cluster and automated snapshotting schedules.",
                    status = "TODO",
                    priority = "HIGH",
                    assignedTo = "admin@collabtask.com",
                    assignedName = "System Admin",
                    dueDate = System.currentTimeMillis() + 86400000 * 2,
                    category = "Features",
                    subtasksJson = "[{\"id\":\"s1\",\"title\":\"Setup AWS RDS Multi-AZ\",\"isCompleted\":true},{\"id\":\"s2\",\"title\":\"Configure replica latency alarms\",\"isCompleted\":false}]"
                ),
                TaskEntity(
                    title = "Database Schema Migration",
                    description = "Integrate secure activity logging and subtask structures into active state repositories.",
                    status = "IN_PROGRESS",
                    priority = "MEDIUM",
                    assignedTo = "manager@collabtask.com",
                    assignedName = "Sarah Jenkins",
                    dueDate = System.currentTimeMillis() + 86400000 * 4,
                    category = "Bugs",
                    subtasksJson = "[{\"id\":\"s3\",\"title\":\"Update migration scripts\",\"isCompleted\":true},{\"id\":\"s4\",\"title\":\"Test offline sync transactions\",\"isCompleted\":false}]"
                ),
                TaskEntity(
                    title = "Client-Side High Performance Dashboard",
                    description = "Verify real-time analytic graph updates and clean visual Material 3 theme colors.",
                    status = "DONE",
                    priority = "LOW",
                    assignedTo = "member@collabtask.com",
                    assignedName = "John Doe",
                    dueDate = System.currentTimeMillis() - 86400000,
                    category = "Design",
                    subtasksJson = "[{\"id\":\"s5\",\"title\":\"Theme dark/light toggles\",\"isCompleted\":true}]"
                )
            )

            for (task in initialTasks) {
                val taskId = taskDao.insertTask(task).toInt()
                logAction(
                    taskId = taskId,
                    taskTitle = task.title,
                    userName = "System Boot",
                    userRole = "SYSTEM",
                    action = "System seeded initial task"
                )
            }
        }

        // Seed default templates if empty
        if (templateDao.getAllTemplates().isEmpty()) {
            val initialTemplates = listOf(
                TaskTemplateEntity(
                    templateName = "Weekly Server Compliance Audit",
                    title = "Run Server Compliance & QA Audit",
                    description = "Establish AWS replication connectivity, check log logs, and configure security alerts.",
                    priority = "HIGH",
                    defaultAssigneeRole = "ADMIN",
                    category = "Features",
                    subtasksJson = "[{\"id\":\"t1\",\"title\":\"Check multi-region latency delay\",\"isCompleted\":false},{\"id\":\"t2\",\"title\":\"Confirm PostgreSQL replica health\",\"isCompleted\":false},{\"id\":\"t3\",\"title\":\"Log compliance reports\",\"isCompleted\":false}]"
                ),
                TaskTemplateEntity(
                    templateName = "Feature Design & Contrast Review",
                    title = "Execute Material 3 Contrast Peer Review",
                    description = "Review dynamic theme pairing contrast, check padding density compliance, and verify layouts on tablet viewports.",
                    priority = "MEDIUM",
                    defaultAssigneeRole = "MANAGER",
                    category = "Design",
                    subtasksJson = "[{\"id\":\"t4\",\"title\":\"Inspect dark/light mode palette transitions\",\"isCompleted\":false},{\"id\":\"t5\",\"title\":\"Conduct visual density peer review\",\"isCompleted\":false}]"
                ),
                TaskTemplateEntity(
                    templateName = "Regression & Staging Hotfix Suite",
                    title = "Execute Docker Regression & Test Runner",
                    description = "Build intermediate container targets, run performance load testing, and resolve open repository regression traces.",
                    priority = "HIGH",
                    defaultAssigneeRole = "MEMBER",
                    category = "Bugs",
                    subtasksJson = "[{\"id\":\"t6\",\"title\":\"Pull and build intermediate Docker target locally\",\"isCompleted\":false},{\"id\":\"t7\",\"title\":\"Run all local testing pipelines\",\"isCompleted\":false},{\"id\":\"t8\",\"title\":\"Publish regression audit findings\",\"isCompleted\":false}]"
                )
            )
            for (template in initialTemplates) {
                templateDao.insertTemplate(template)
            }
        }
    }

    // --- Task-Related API Middleware ---
    private inline fun <T> executeWithMiddleware(
        endpointName: String,
        payload: Any?,
        block: () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        Log.i("TaskAPI_Middleware", "--- [API REQUEST] Endpoint: $endpointName ---")
        if (payload != null) {
            val payloadDetails = when (payload) {
                is TaskEntity -> {
                    "TaskEntity(id=${payload.id}, title='${payload.title}', category='${payload.category}', priority='${payload.priority}', status='${payload.status}', assignedTo='${payload.assignedTo}', subtasksJsonLength=${payload.subtasksJson.length})"
                }
                is TaskTemplateEntity -> {
                    "TaskTemplateEntity(id=${payload.id}, templateName='${payload.templateName}', title='${payload.title}', priority='${payload.priority}', defaultAssigneeRole='${payload.defaultAssigneeRole}', category='${payload.category}')"
                }
                is CommentEntity -> {
                    "CommentEntity(id=${payload.id}, taskId=${payload.taskId}, author='${payload.authorName}', content='${payload.content.take(50)}...')"
                }
                is UserEntity -> {
                    "UserEntity(email='${payload.email}', fullName='${payload.fullName}', role='${payload.role}')"
                }
                is Map<*, *> -> {
                    payload.entries.joinToString(", ") { "${it.key}=${it.value}" }
                }
                else -> payload.toString()
            }
            Log.i("TaskAPI_Middleware", "[PAYLOAD] Structure: $payloadDetails")
        } else {
            Log.i("TaskAPI_Middleware", "[PAYLOAD] Empty")
        }

        return try {
            val result = block()
            val latency = System.currentTimeMillis() - startTime
            Log.i("TaskAPI_Middleware", "--- [API RESPONSE] Endpoint: $endpointName | STATUS: SUCCESS | Latency: ${latency}ms ---")
            result
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            Log.e("TaskAPI_Middleware", "--- [API RESPONSE] Endpoint: $endpointName | STATUS: FAILED (${e.javaClass.simpleName}) | Latency: ${latency}ms ---", e)
            throw e
        }
    }

    // --- Task Operations ---

    fun getAllTasks(): Flow<List<TaskEntity>> {
        return executeWithMiddleware("GET /api/tasks", null) {
            taskDao.getAllTasksFlow()
        }
    }

    suspend fun getTaskById(taskId: Int): TaskEntity? {
        return executeWithMiddleware("GET /api/tasks/$taskId", mapOf("taskId" to taskId)) {
            taskDao.getTaskById(taskId)
        }
    }

    fun getTaskByIdFlow(taskId: Int): Flow<TaskEntity?> {
        return executeWithMiddleware("GET /api/tasks/$taskId/flow", mapOf("taskId" to taskId)) {
            taskDao.getTaskByIdFlow(taskId)
        }
    }

    suspend fun createTask(task: TaskEntity, activeUser: UserEntity): Int {
        return executeWithMiddleware("POST /api/tasks", mapOf("task" to task, "user" to activeUser)) {
            val insertedId = taskDao.insertTask(task).toInt()
            logAction(
                taskId = insertedId,
                taskTitle = task.title,
                userName = activeUser.fullName,
                userRole = activeUser.role,
                action = "Created task in [${task.category}]"
            )
            insertedId
        }
    }

    suspend fun updateTask(task: TaskEntity, activeUser: UserEntity, changeSummary: String) {
        val payloadMap = mapOf("task" to task, "user" to activeUser, "changeSummary" to changeSummary)
        executeWithMiddleware("PUT /api/tasks/${task.id}", payloadMap) {
            taskDao.updateTask(task)
            logAction(
                taskId = task.id,
                taskTitle = task.title,
                userName = activeUser.fullName,
                userRole = activeUser.role,
                action = changeSummary
            )
        }
    }

    suspend fun deleteTask(taskId: Int, activeUser: UserEntity, taskTitle: String) {
        val payloadMap = mapOf("taskId" to taskId, "user" to activeUser, "taskTitle" to taskTitle)
        executeWithMiddleware("DELETE /api/tasks/$taskId", payloadMap) {
            taskDao.deleteTaskById(taskId)
            logAction(
                taskId = taskId,
                taskTitle = taskTitle,
                userName = activeUser.fullName,
                userRole = activeUser.role,
                action = "Deleted task"
            )
        }
    }

    // --- Comment Operations ---

    fun getCommentsForTask(taskId: Int): Flow<List<CommentEntity>> = commentDao.getCommentsForTask(taskId)

    suspend fun addComment(comment: CommentEntity, activeUser: UserEntity, taskTitle: String) {
        val payloadMap = mapOf("comment" to comment, "user" to activeUser, "taskTitle" to taskTitle)
        executeWithMiddleware("POST /api/tasks/${comment.taskId}/comments", payloadMap) {
            commentDao.insertComment(comment)
            logAction(
                taskId = comment.taskId,
                taskTitle = taskTitle,
                userName = activeUser.fullName,
                userRole = activeUser.role,
                action = "Added comment: \"${comment.content.take(30)}...\""
            )
        }
    }

    // --- Activity Log Operations ---

    fun getAllActivityLogs(): Flow<List<ActivityLogEntity>> = logDao.getAllLogs()

    suspend fun logAction(taskId: Int, taskTitle: String, userName: String, userRole: String, action: String) {
        val log = ActivityLogEntity(
            taskId = taskId,
            taskTitle = taskTitle,
            userName = userName,
            userRole = userRole,
            action = action,
            timestamp = System.currentTimeMillis()
        )
        logDao.insertLog(log)
    }

    suspend fun clearAllActivityLogs() {
        logDao.clearLogs()
    }

    // --- Task Template Operations ---

    fun getAllTemplatesFlow(): Flow<List<TaskTemplateEntity>> {
        return executeWithMiddleware("GET /api/templates", null) {
            templateDao.getAllTemplatesFlow()
        }
    }

    suspend fun getAllTemplates(): List<TaskTemplateEntity> {
        return executeWithMiddleware("GET /api/templates/all", null) {
            templateDao.getAllTemplates()
        }
    }

    suspend fun createTemplate(template: TaskTemplateEntity): Int {
        return executeWithMiddleware("POST /api/templates", template) {
            templateDao.insertTemplate(template).toInt()
        }
    }

    suspend fun deleteTemplate(id: Int) {
        executeWithMiddleware("DELETE /api/templates/$id", mapOf("templateId" to id)) {
            templateDao.deleteTemplateById(id)
        }
    }
}
