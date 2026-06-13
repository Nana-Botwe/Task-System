package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val fullName: String,
    val passwordHash: String,
    val role: String, // "ADMIN", "MANAGER", "MEMBER"
    val mfaEnabled: Boolean = false,
    val mfaSecret: String = ""
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val status: String, // "TODO", "IN_PROGRESS", "REVIEW", "DONE"
    val priority: String, // "HIGH", "MEDIUM", "LOW"
    val assignedTo: String, // email or "Unassigned"
    val assignedName: String = "Unassigned",
    val dueDate: Long, // Epoch timestamp
    val category: String, // "Features", "Bugs", "Design", "Marketing"
    val subtasksJson: String = "[]", // List of Subtask in JSON
    val lastUpdated: Long = System.currentTimeMillis(),
    val syncState: String = "SYNCED", // "SYNCED", "PENDING", "SYNCING"
    val activeEditor: String? = null // For simulating live collaboration editing conflict state
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val authorName: String,
    val authorRole: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "task_templates")
data class TaskTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val templateName: String,
    val title: String,
    val description: String,
    val priority: String, // "HIGH", "MEDIUM", "LOW"
    val defaultAssigneeRole: String, // "ADMIN", "MANAGER", "MEMBER" or "Unassigned"
    val category: String, // "Features", "Bugs", "Design", "Marketing"
    val subtasksJson: String = "[]" // List of Subtask in JSON
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val taskTitle: String,
    val userName: String,
    val userRole: String,
    val action: String, // e.g. "Completed", "Re-assigned", "Created Comment", "Changed Priority"
    val timestamp: Long = System.currentTimeMillis()
)

// Helper model representing a compact subtask
@JsonClass(generateAdapter = true)
data class Subtask(
    val id: String,
    val title: String,
    val isCompleted: Boolean
)

class DatabaseConverters {
    companion object {
        private val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        private val subtaskListType = Types.newParameterizedType(List::class.java, Subtask::class.java)
        private val adapter = moshi.adapter<List<Subtask>>(subtaskListType)
    }

    @TypeConverter
    fun fromSubtaskList(value: List<Subtask>?): String {
        return value?.let { adapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toSubtaskList(value: String?): List<Subtask> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            adapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
