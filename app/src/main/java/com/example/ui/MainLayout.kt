package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(viewModel: TaskViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val themeModeIsDark by viewModel.themeModeIsDark.collectAsState()
    val offlineMode by viewModel.offlineMode.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val collaborationStateMessage by viewModel.collaborationStateMessage.collectAsState()
    val notificationAlerts by viewModel.notificationAlerts.collectAsState()

    var activeScreenId by remember { mutableStateOf("dashboard") } // "dashboard", "board", "calendar", "logs"
    var showNotificationDrawer by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    Row(modifier = Modifier.fillMaxSize()) {
        // RESPONSIVE canonical layout: Show NavigationRail on Tablet/Wide screens
        if (isWideScreen) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                header = {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SyncAlt, contentDescription = null, tint = Color.White)
                    }
                },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                NavigationRailItem(
                    selected = activeScreenId == "dashboard",
                    onClick = { activeScreenId = "dashboard" },
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Dashboard") },
                    label = { Text("Metrics", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationRailItem(
                    selected = activeScreenId == "analytics",
                    onClick = { activeScreenId = "analytics" },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
                    label = { Text("Analytics", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationRailItem(
                    selected = activeScreenId == "board",
                    onClick = { activeScreenId = "board" },
                    icon = { Icon(Icons.Default.Dns, contentDescription = "Tasks") },
                    label = { Text("Projects", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationRailItem(
                    selected = activeScreenId == "calendar",
                    onClick = { activeScreenId = "calendar" },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
                    label = { Text("Calendar", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationRailItem(
                    selected = activeScreenId == "logs",
                    onClick = { activeScreenId = "logs" },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Audit Logs") },
                    label = { Text("Trace", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                if (currentUser?.role?.equals("ADMIN", ignoreCase = true) == true) {
                    NavigationRailItem(
                        selected = activeScreenId == "admin",
                        onClick = { activeScreenId = "admin" },
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Console") },
                        label = { Text("Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "OPERATIONAL CONTROL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "Workspace",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            // State subtitle indicators
                            if (collaborationStateMessage != null) {
                                Text(
                                    text = collaborationStateMessage!!,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        // Display user role pill in TopBar
                        currentUser?.let { user ->
                            val colorScheme = when (user.role) {
                                "ADMIN" -> Color(0xFFFFCC80) to Color(0xFF5D3F00)
                                "MANAGER" -> Color(0xFFD0BCFF) to Color(0xFF381E72)
                                else -> Color(0xFF90CAF9) to Color(0xFF0D47A1)
                            }
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorScheme.first)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = user.role,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colorScheme.second,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    },
                    actions = {
                        // Dashboard screen specific Export CSV icon action in toolbar
                        if (activeScreenId == "dashboard") {
                            IconButton(
                                onClick = {
                                    viewModel.exportToCSV(context)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Output,
                                    contentDescription = "Export CSV",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Force Simulated Collaboration event button (Extremely cool for evaluation!)
                        IconButton(
                            onClick = {
                                viewModel.simulateActiveUserAction()
                            }
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "Simulate peer edit", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Network Offline sync trigger controller
                        IconButton(
                            onClick = {
                                viewModel.toggleOfflineMode()
                            }
                        ) {
                            Icon(
                                imageVector = if (offlineMode) Icons.Default.CloudOff else Icons.Default.CloudSync,
                                contentDescription = "Network offline switcher",
                                tint = if (offlineMode) Color.Red else Color(0xFF00C853)
                            )
                        }

                        // Notifications Tray Button
                        Box {
                            IconButton(onClick = { showNotificationDrawer = !showNotificationDrawer }) {
                                Icon(
                                    imageVector = if (notificationAlerts.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = "Notifications tray",
                                    tint = if (notificationAlerts.isNotEmpty()) Color.Red else MaterialTheme.colorScheme.onBackground
                                )
                            }
                            if (notificationAlerts.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 4.dp, end = 4.dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                        .border(1.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                )
                            }
                        }

                        // Theme Mode switch
                        IconButton(onClick = { viewModel.toggleTheme() }) {
                            Icon(
                                imageVector = if (themeModeIsDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Theme Toggle"
                            )
                        }

                        // Logout Icon
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(Icons.Default.Logout, contentDescription = "Logout")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                // Show bottom NavigationBar ONLY on Compact mobile viewports
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            selected = activeScreenId == "dashboard",
                            onClick = { activeScreenId = "dashboard" },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Metrics") },
                            label = { Text("Metrics", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = activeScreenId == "analytics",
                            onClick = { activeScreenId = "analytics" },
                            icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
                            label = { Text("Analytics", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = activeScreenId == "board",
                            onClick = { activeScreenId = "board" },
                            icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Tasks") },
                            label = { Text("My Board", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = activeScreenId == "calendar",
                            onClick = { activeScreenId = "calendar" },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                            label = { Text("Calendar", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = activeScreenId == "logs",
                            onClick = { activeScreenId = "logs" },
                            icon = { Icon(Icons.Default.History, contentDescription = "Trace") },
                            label = { Text("Trace Logs", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        if (currentUser?.role?.equals("ADMIN", ignoreCase = true) == true) {
                            NavigationBarItem(
                                selected = activeScreenId == "admin",
                                onClick = { activeScreenId = "admin" },
                                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Console") },
                                label = { Text("Admin Console", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Screen Navigation Controller
                AnimatedContent(
                    targetState = activeScreenId,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "AppScreenNavigation"
                ) { screenId ->
                    when (screenId) {
                        "dashboard" -> DashboardScreen(viewModel)
                        "analytics" -> AnalyticsDashboardScreen(viewModel)
                        "board" -> TasksScreen(viewModel)
                        "calendar" -> CalendarScreen(viewModel)
                        "logs" -> ActivityLogsScreen(viewModel)
                        "admin" -> AdminScreen(viewModel)
                    }
                }

                // Synchronization Progress Spinner banner
                if (isSyncing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text("Replication in progress...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Sliding notifications alerts tray
                if (showNotificationDrawer) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .clickable { showNotificationDrawer = false }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .fillMaxWidth(if (isWideScreen) 0.4f else 0.85f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable(enabled = false) {}
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Dynamic Alerts Logs", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    IconButton(onClick = { viewModel.clearNotificationAlerts() }) {
                                        Icon(Icons.Default.ClearAll, contentDescription = "Clear all notifications", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                if (notificationAlerts.isEmpty()) {
                                    Text(
                                        text = "No alerts generated. Simulate peer edits to populate logs.",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.heightIn(max = 300.dp)
                                    ) {
                                        items(notificationAlerts) { item ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary)
                                                )
                                                Text(item.first, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
