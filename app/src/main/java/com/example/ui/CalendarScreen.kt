package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TaskEntity
import com.example.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val focusedDateMillis by viewModel.calendarFocusedDate.collectAsState()
    val context = LocalContext.current

    // Set up standard Calendar object matching state instance
    val calendar = remember(focusedDateMillis) {
        Calendar.getInstance().apply {
            timeInMillis = focusedDateMillis
        }
    }

    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH) // 0-indexed

    // Generate month header label (e.g. "June 2026")
    val monthLabel = remember(focusedDateMillis) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdf.format(Date(focusedDateMillis))
    }

    // Identify days of active month
    val daysInMonth = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val startOffset = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        // Day of week is 1-indexed (Sunday = 1, Saturday = 7)
        cal.get(Calendar.DAY_OF_WEEK) - 1
    }

    var selectedDayNum by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    // Aggregate tasks due on the selected day
    val selectedDayTasks = remember(tasks, currentYear, currentMonth, selectedDayNum) {
        tasks.filter { task ->
            val taskCal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
            taskCal.get(Calendar.YEAR) == currentYear &&
                    taskCal.get(Calendar.MONTH) == currentMonth &&
                    taskCal.get(Calendar.DAY_OF_MONTH) == selectedDayNum
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Selector Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = focusedDateMillis
                    cal.add(Calendar.MONTH, -1)
                    viewModel.calendarFocusedDate.value = cal.timeInMillis
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month")
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = monthLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = focusedDateMillis
                    cal.add(Calendar.MONTH, 1)
                    viewModel.calendarFocusedDate.value = cal.timeInMillis
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }
        }

        // Calendar Grid Map Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Day of Week Header row
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { weekday ->
                        Text(
                            text = weekday,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Build full month cells grid sequentially
                val totalCells = daysInMonth + startOffset
                val totalRows = (totalCells + 6) / 7

                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - startOffset + 1

                            if (cellIndex in startOffset until (daysInMonth + startOffset)) {
                                // Task dots on this particular day
                                val dayTasks = tasks.filter { task ->
                                    val taskCal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
                                    taskCal.get(Calendar.YEAR) == currentYear &&
                                            taskCal.get(Calendar.MONTH) == currentMonth &&
                                            taskCal.get(Calendar.DAY_OF_MONTH) == dayNum
                                }

                                val isSel = dayNum == selectedDayNum
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSel) MaterialTheme.colorScheme.primaryContainer 
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (dayTasks.isNotEmpty()) 1.dp else 0.dp,
                                            color = if (dayTasks.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedDayNum = dayNum },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        // Visual Dot Indicators for tasks on this date
                                        if (dayTasks.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.padding(top = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                dayTasks.take(3).forEach { t ->
                                                    val dotColor = when (t.priority) {
                                                        "HIGH" -> Color.Red
                                                        "MEDIUM" -> Color.Yellow
                                                        else -> Color.Blue
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(dotColor)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Empty padding cells
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Schedule Headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tasks for Day $selectedDayNum: (${selectedDayTasks.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Alert sound simulation trigger for schedule checking
            TextButton(
                onClick = {
                    Toast.makeText(context, "MOCK CALENDAR ALARM: Notifications pipeline sync complete.", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.Alarm, contentDescription = "Mock Alarm", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Simulate Active Reminder", fontSize = 11.sp)
            }
        }

        // List selected day schedule
        if (selectedDayTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No milestones scheduled for this calendar date.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedDayTasks) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val color = when (task.priority) {
                                "HIGH" -> Color.Red
                                "MEDIUM" -> Color.Yellow
                                else -> Color.Blue
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("${task.category} • Assg: ${task.assignedName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Small status label
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(task.status, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
