package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AuthScreen
import com.example.ui.MainLayout
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: TaskViewModel = viewModel()
      val isAuthenticated by viewModel.isAuthenticated.collectAsState()
      val themeModeIsDark by viewModel.themeModeIsDark.collectAsState()

      MyApplicationTheme(darkTheme = themeModeIsDark) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
          if (isAuthenticated) {
            MainLayout(viewModel = viewModel)
          } else {
            AuthScreen(viewModel = viewModel)
          }
        }
      }
    }
  }
}
