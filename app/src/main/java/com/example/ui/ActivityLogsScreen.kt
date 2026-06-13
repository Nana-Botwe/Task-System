package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityLogEntity
import com.example.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActivityLogsScreen(viewModel: TaskViewModel) {
    val logs by viewModel.activityLogs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var logSearchQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(logs, logSearchQuery) {
        logs.filter { l ->
            l.taskTitle.contains(logSearchQuery, ignoreCase = true) ||
                    l.userName.contains(logSearchQuery, ignoreCase = true) ||
                    l.action.contains(logSearchQuery, ignoreCase = true)
        }
    }

    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Log Statistics Header block
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Security Audit Trail",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Permanent write-ahead compliance ledger",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Wipe audit capability: Restricted to System Admins only
            if (currentUser?.role == "ADMIN") {
                TextButton(
                    onClick = { viewModel.clearNotificationAlerts() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear logs", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Alerts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live Log query search
        OutlinedTextField(
            value = logSearchQuery,
            onValueChange = { logSearchQuery = it },
            placeholder = { Text("Filter logs by user, action...") },
            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
            trailingIcon = if (logSearchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { logSearchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Historical Ledger rows
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HistoryToggleOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No audit trails found matching parameters.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header: User, Role Badge, Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = log.userName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    // Colored role pill
                                    val roleBg = when (log.userRole) {
                                        "ADMIN" -> Color(0xFFD0BCFF)
                                        "MANAGER" -> Color(0xFFFFCC80)
                                        "MEMBER" -> Color(0xFF90CAF9)
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                    val roleText = when (log.userRole) {
                                        "ADMIN" -> Color(0xFF381E72)
                                        "MANAGER" -> Color(0xFF5D3F00)
                                        "MEMBER" -> Color(0xFF0D47A1)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(roleBg)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = log.userRole,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Black,
                                            color = roleText
                                        )
                                    }
                                }

                                Text(
                                    text = sdf.format(Date(log.timestamp)),
                                    fontSize = 8.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Action and context task
                            Text(
                                text = log.action,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (log.taskTitle.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "ENTITY TARGET:",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = log.taskTitle,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
