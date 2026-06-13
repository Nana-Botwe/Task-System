package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteUserByEmail(email: String)

    @Query("SELECT * FROM users ORDER BY fullName ASC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY fullName ASC")
    suspend fun getAllUsers(): List<UserEntity>
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY lastUpdated DESC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id_")
    suspend fun getTaskById(id_: Int): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id_")
    fun getTaskByIdFlow(id_: Int): Flow<TaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE taskId = :taskId ORDER BY timestamp ASC")
    fun getCommentsForTask(taskId: Int): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity)

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}

@Dao
interface TaskTemplateDao {
    @Query("SELECT * FROM task_templates ORDER BY id ASC")
    fun getAllTemplatesFlow(): Flow<List<TaskTemplateEntity>>

    @Query("SELECT * FROM task_templates ORDER BY id ASC")
    suspend fun getAllTemplates(): List<TaskTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TaskTemplateEntity): Long

    @Query("DELETE FROM task_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Int)
}

@Database(
    entities = [UserEntity::class, TaskEntity::class, CommentEntity::class, ActivityLogEntity::class, TaskTemplateEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val userDao: UserDao
    abstract val taskDao: TaskDao
    abstract val commentDao: CommentDao
    abstract val activityLogDao: ActivityLogDao
    abstract val taskTemplateDao: TaskTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "collabtask_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
