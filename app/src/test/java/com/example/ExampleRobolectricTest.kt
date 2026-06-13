package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.TaskEntity
import com.example.viewmodel.TaskViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun testViewModelInstantiation() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TaskViewModel(application)
    assertNotNull(viewModel)
  }

  @Test
  fun testMainActivityLaunch() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      assertNotNull(scenario)
    }
  }

  @Test
  fun testDatabaseOperations() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val taskDao = db.taskDao
    
    val task = TaskEntity(
      title = "Test Subtasks Integration",
      description = "Checking if Moshi converter crashes Room on write/read",
      status = "TODO",
      priority = "HIGH",
      assignedTo = "admin@collabtask.com",
      assignedName = "System Admin",
      dueDate = System.currentTimeMillis() + 86400000,
      category = "Features",
      subtasksJson = "[{\"id\":\"s123\",\"title\":\"Verify test task\",\"isCompleted\":false}]"
    )
    
    val id = taskDao.insertTask(task).toInt()
    val retrieved = taskDao.getTaskById(id)
    assertNotNull(retrieved)
    assertEquals("Test Subtasks Integration", retrieved?.title)
  }

  @Test
  fun testLoginFlow() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TaskViewModel(application)
    
    // Seeding is automatically executed within TaskViewModel init
    // Just delay slightly and idle looper to ensure the seed completes on the dispatcher
    kotlinx.coroutines.delay(500)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    
    // Attempt Login
    viewModel.login("admin@collabtask.com", "admin123")
    
    // Wait for async login flow and idle looper
    kotlinx.coroutines.delay(200)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    
    // MFA should be required
    assertTrue(viewModel.isMfaRequired.value)
    val otp = viewModel.currentMfaOtp.value
    assertNotNull(otp)
    
    // Verify MFA OTP
    viewModel.verifyMfa(otp!!)
    
    // Wait for async verification
    kotlinx.coroutines.delay(200)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    
    // User should now be authenticated
    assertTrue(viewModel.isAuthenticated.value)
    assertEquals("System Admin", viewModel.currentUser.value?.fullName)
  }
}
