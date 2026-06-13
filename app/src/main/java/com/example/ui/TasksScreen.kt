package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val offlineMode by viewModel.offlineMode.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    // Search and Filters
    val searchQuery by viewModel.filterSearchQuery.collectAsState()
    val statusFilter by viewModel.filterStatus.collectAsState()
    val priorityFilter by viewModel.filterPriority.collectAsState()
    val categoryFilter by viewModel.filterCategory.collectAsState()

    // UI Dialog selectors
    var showUpsertDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var selectedTaskForDetails by remember { mutableStateOf<TaskEntity?>(null) }

    // Filter logic
    val filteredTasks = remember(tasks, searchQuery, statusFilter, priorityFilter, categoryFilter) {
        tasks.filter { task ->
            val matchesSearch = task.title.contains(searchQuery, ignoreCase = true) || 
                                task.description.contains(searchQuery, ignoreCase = true)
            val matchesStatus = statusFilter == "ALL" || task.status == statusFilter
            val matchesPriority = priorityFilter == "ALL" || task.priority == priorityFilter
            val matchesCategory = categoryFilter == "ALL" || task.category == categoryFilter
            matchesSearch && matchesStatus && matchesPriority && matchesCategory
        }
    }

    Scaffold(
        floatingActionButton = {
            // RBAC layout protection: Only manager or admin can instantiate new tasks
            if (currentUser?.role == "ADMIN" || currentUser?.role == "MANAGER") {
                FloatingActionButton(
                    onClick = {
                        taskToEdit = null
                        showUpsertDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add task")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Input Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.filterSearchQuery.value = it },
                placeholder = { Text("Filter projects, requirements...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.filterSearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filtering Tags Drawer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category filtering chip
                var showCatMenu by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { showCatMenu = true },
                        label = { Text(if (categoryFilter == "ALL") "Category" else "$categoryFilter") },
                        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Category", modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenu(expanded = showCatMenu, onDismissRequest = { showCatMenu = false }) {
                        listOf("ALL", "Features", "Bugs", "Design", "Marketing").forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    viewModel.filterCategory.value = cat
                                    showCatMenu = false
                                }
                            )
                        }
                    }
                }

                // Priority filtering chip
                var showPriorityMenu by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { showPriorityMenu = true },
                        label = { Text(if (priorityFilter == "ALL") "Priority" else "$priorityFilter") },
                        leadingIcon = { Icon(Icons.Default.Sort, contentDescription = "Priority", modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenu(expanded = showPriorityMenu, onDismissRequest = { showPriorityMenu = false }) {
                        listOf("ALL", "HIGH", "MEDIUM", "LOW").forEach { pri ->
                            DropdownMenuItem(
                                text = { Text(pri) },
                                onClick = {
                                    viewModel.filterPriority.value = pri
                                    showPriorityMenu = false
                                }
                            )
                        }
                    }
                }

                // Status filtering chip
                var showStatusMenu by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { showStatusMenu = true },
                        label = { Text(if (statusFilter == "ALL") "Status" else "$statusFilter") },
                        leadingIcon = { Icon(Icons.Default.Adjust, contentDescription = "Status", modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                        listOf("ALL", "TODO", "IN_PROGRESS", "REVIEW", "DONE").forEach { stat ->
                            DropdownMenuItem(
                                text = { Text(stat) },
                                onClick = {
                                    viewModel.filterStatus.value = stat
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Task List Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Operational Task Board (${filteredTasks.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                if (statusFilter != "ALL" || priorityFilter != "ALL" || categoryFilter != "ALL" || searchQuery.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.filterSearchQuery.value = ""
                            viewModel.filterCategory.value = "ALL"
                            viewModel.filterPriority.value = "ALL"
                            viewModel.filterStatus.value = "ALL"
                        }
                    ) {
                        Text("Reset Filters", fontSize = 11.sp)
                    }
                }
            }

            // Tasks List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(100.dp)
                        ) {
                            val strokeColor = MaterialTheme.colorScheme.outline
                            val circleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(80.dp)) {
                                drawCircle(
                                    color = circleColor,
                                    radius = size.minDimension / 2.5f
                                )
                                drawLine(
                                    color = strokeColor,
                                    start = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.5f),
                                    end = androidx.compose.ui.geometry.Offset(size.width * 0.7f, size.height * 0.5f),
                                    strokeWidth = 4f
                                )
                                drawLine(
                                    color = strokeColor,
                                    start = androidx.compose.ui.geometry.Offset(size.width * 0.4f, size.height * 0.35f),
                                    end = androidx.compose.ui.geometry.Offset(size.width * 0.6f, size.height * 0.35f),
                                    strokeWidth = 4f
                                )
                                drawLine(
                                    color = strokeColor,
                                    start = androidx.compose.ui.geometry.Offset(size.width * 0.45f, size.height * 0.65f),
                                    end = androidx.compose.ui.geometry.Offset(size.width * 0.55f, size.height * 0.65f),
                                    strokeWidth = 4f
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Active Pipelines Match Filters",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Reset constraints or compile new items to begin work in this stream.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.widthIn(max = 240.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            currentUser = currentUser,
                            onClick = {
                                selectedTaskForDetails = task
                                viewModel.selectTask(task.id)
                            },
                            onStatusQuickToggle = { newStatus ->
                                viewModel.updateTaskStatusOnly(task.id, newStatus)
                            },
                            onCyclePriority = { newPriority ->
                                // Interactive prioritizing action.
                                val oldP = task.priority
                                val updated = task.copy(priority = newPriority, lastUpdated = System.currentTimeMillis())
                                viewModel.upsertTask(
                                    id = task.id,
                                    title = task.title,
                                    description = task.description,
                                    status = task.status,
                                    priority = newPriority,
                                    assignedEmail = task.assignedTo,
                                    category = task.category,
                                    subtasks = DatabaseConverters().toSubtaskList(task.subtasksJson),
                                    dueDate = task.dueDate
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Task Details & Comment Drawer/Overlay Dialog
    if (selectedTaskForDetails != null) {
        val activeSelectedTask by viewModel.selectedTask.collectAsState()
        val verifiedTask = activeSelectedTask ?: selectedTaskForDetails!!
        
        TaskDetailsDialog(
            task = verifiedTask,
            currentUser = currentUser,
            onDismiss = {
                selectedTaskForDetails = null
                viewModel.selectTask(null)
            },
            viewModel = viewModel,
            onEditTrigger = {
                taskToEdit = verifiedTask
                selectedTaskForDetails = null
                showUpsertDialog = true
            }
        )
    }

    // Task Create / Editor Dialog
    if (showUpsertDialog) {
        TaskUpsertDialog(
            task = taskToEdit,
            viewModel = viewModel,
            onDismiss = { showUpsertDialog = false }
        )
    }
}

@Composable
fun TaskItemCard(
    task: TaskEntity,
    currentUser: UserEntity?,
    onClick: () -> Unit,
    onStatusQuickToggle: (String) -> Unit,
    onCyclePriority: (String) -> Unit
) {
    val subtasks = remember(task.subtasksJson) {
        DatabaseConverters().toSubtaskList(task.subtasksJson)
    }
    val completedSubtasks = subtasks.count { it.isCompleted }
    val totalSubtasks = subtasks.size

    val priorityColor = when (task.priority) {
        "HIGH" -> Color(0xFFFF5252)
        "MEDIUM" -> Color(0xFFFFAB40)
        else -> Color(0xFF448AFF)
    }

    val isDone = task.status == "DONE"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = if (task.syncState == "PENDING") {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        } else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left Status/Priority Bar Stripe mimicking border-l-4 border-[color] in the design theme
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(priorityColor)
            )

            Column(modifier = Modifier.weight(1f).padding(14.dp)) {
                // Task Tags Header Line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // Category Chip Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = task.category.uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }

                // Synchronization Status Tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.syncState == "PENDING") {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Pending Sync",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PENDING", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Synced",
                            tint = Color(0xFF00C853),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SYNCED", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00C853))
                    }

                    // Collaborator Lock Overlay indicator
                    if (task.activeEditor != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Task Header with Status Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Large tap target checkbox for swift status changes
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDone) Color(0xFF00C853) 
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                        .clickable {
                            val next = if (isDone) "TODO" else "DONE"
                            onStatusQuickToggle(next)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (task.description.isNotEmpty()) {
                        Text(
                            text = task.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Indicator (if subtasks are present)
            if (totalSubtasks > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$completedSubtasks/$totalSubtasks subtasks", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(
                        progress = completedSubtasks.toFloat() / totalSubtasks.toFloat(),
                        modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = if (isDone) Color(0xFF00C853) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Footer metadata row: Assignee, priority interactive arrows, due date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Assignee
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = "Assignee", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.assignedName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Interactive Prioritization cycle triggers
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clickable {
                                // Cycle priorities interactively
                                val nextPriorityIndex = when (task.priority) {
                                    "LOW" -> "MEDIUM"
                                    "MEDIUM" -> "HIGH"
                                    else -> "LOW"
                                }
                                onCyclePriority(nextPriorityIndex)
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Change priority", modifier = Modifier.size(10.dp), tint = priorityColor)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "PRIORITY: ${task.priority}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = priorityColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Date
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    val isOverdue = task.status != "DONE" && task.dueDate < System.currentTimeMillis()
                    Text(
                        text = sdf.format(Date(task.dueDate)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
}

@Composable
fun TaskDetailsDialog(
    task: TaskEntity,
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    viewModel: TaskViewModel,
    onEditTrigger: () -> Unit
) {
    val comments by viewModel.currentComments.collectAsState()
    var commentInput by remember { mutableStateOf("") }
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    val subtasks = remember(task.subtasksJson) {
        DatabaseConverters().toSubtaskList(task.subtasksJson)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close Details")
            }
        },
        dismissButton = {
            if (currentUser?.role == "ADMIN" || currentUser?.role == "MANAGER") {
                Button(onClick = onEditTrigger) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modify Structure")
                }
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(task.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                
                // Trash / Delete icon if admin/manager
                if (currentUser?.role == "ADMIN" || currentUser?.role == "MANAGER") {
                    IconButton(onClick = {
                        viewModel.deleteTask(task.id, task.title)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete task", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // General properties row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("STATUS", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            var showStatusMenu by remember { mutableStateOf(false) }
                            Box {
                                Text(
                                    task.status,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { showStatusMenu = true }
                                )
                                DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                                    listOf("TODO", "IN_PROGRESS", "REVIEW", "DONE").forEach { statusChoice ->
                                        DropdownMenuItem(
                                            text = { Text(statusChoice) },
                                            onClick = {
                                                viewModel.updateTaskStatusOnly(task.id, statusChoice)
                                                showStatusMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("PRIORITY", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(task.priority, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Red)
                        }
                    }

                    OutlinedCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("CATEGORY", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(task.category, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                if (task.activeEditor != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock", tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Active Cloud session locks: ${task.activeEditor} is editing.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Description Title
                Text("Scope Specifications", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (task.description.isEmpty()) "No descriptive details logged." else task.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Subtask interactive check-list
                Text("Operational Checklist", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                if (subtasks.isEmpty()) {
                    Text("No checkpoint criteria declared.", fontSize = 11.sp, color = Color.Gray)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        subtasks.forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val updatedSubtasks = subtasks.map { s ->
                                            if (s.id == sub.id) s.copy(isCompleted = !s.isCompleted) else s
                                        }
                                        viewModel.updateSubtaskChecklist(task.id, updatedSubtasks)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = sub.isCompleted,
                                    onCheckedChange = { ch ->
                                        val updatedSubtasks = subtasks.map { s ->
                                            if (s.id == sub.id) s.copy(isCompleted = ch) else s
                                        }
                                        viewModel.updateSubtaskChecklist(task.id, updatedSubtasks)
                                    }
                                )
                                Text(
                                    text = sub.title,
                                    fontSize = 13.sp,
                                    textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (sub.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Divider()

                // Group Comments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Collaborative Comments (${comments.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.Default.Forum, contentDescription = "Comments tag", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }

                if (comments.isEmpty()) {
                    Text(
                        "No discussions logged. Be the first to coordinate with your division.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        comments.forEach { c ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(c.authorName.take(1).uppercase(), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(c.authorName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            
                                            // Role pill inside comment
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.outlineVariant)
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(c.authorRole, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Text(sdf.format(Date(c.timestamp)), fontSize = 8.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(c.content, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                // Add comment input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        placeholder = { Text("Post core update...", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (commentInput.trim().isNotEmpty()) {
                                viewModel.submitComment(commentInput)
                                commentInput = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    )
}

@Composable
fun TaskUpsertDialog(
    task: TaskEntity?,
    viewModel: TaskViewModel,
    onDismiss: () -> Unit
) {
    val users by viewModel.users.collectAsState()
    val templates by viewModel.templates.collectAsState()

    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var status by remember { mutableStateOf(task?.status ?: "TODO") }
    var priority by remember { mutableStateOf(task?.priority ?: "MEDIUM") }
    var assignedEmail by remember { mutableStateOf(task?.assignedTo ?: "Unassigned") }
    var category by remember { mutableStateOf(task?.category ?: "Features") }
    
    // Subtask lists
    var subtasks by remember {
        mutableStateOf(
            if (task != null) DatabaseConverters().toSubtaskList(task.subtasksJson)
            else emptyList()
        )
    }
    var newSubtaskTitle by remember { mutableStateOf("") }

    var expandedAssignee by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedTemplateSelect by remember { mutableStateOf(false) }
    var showSaveTemplateNameDialog by remember { mutableStateOf(false) }
    var newTemplateNameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().isEmpty()) return@Button
                    viewModel.upsertTask(
                        id = task?.id ?: 0,
                        title = title,
                        description = description,
                        status = status,
                        priority = priority,
                        assignedEmail = assignedEmail,
                        category = category,
                        subtasks = subtasks,
                        dueDate = task?.dueDate ?: (System.currentTimeMillis() + 86400000 * 3) // 3 days default
                    )
                    onDismiss()
                }
            ) {
                Text(if (task == null) "Commit Record" else "Apply Modifications")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(if (task == null) "Schedule New Task" else "Modify Existing Task", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick start task template selector
                Text(
                    text = "Streamline Workflow: Task Templates",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = "Choose Predefined/Custom Template...",
                        onValueChange = {},
                        label = { Text("Workflow Template Actions") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Template Choice") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.primary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedTemplateSelect = true }
                    )
                    DropdownMenu(
                        expanded = expandedTemplateSelect,
                        onDismissRequest = { expandedTemplateSelect = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        if (templates.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No saved templates loaded.", fontSize = 12.sp, color = Color.Gray) },
                                onClick = { expandedTemplateSelect = false },
                                enabled = false
                            )
                        } else {
                            templates.forEach { tmpl ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(tmpl.templateName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(
                                                "Role: ${tmpl.defaultAssigneeRole} • Category: ${tmpl.category} • Priority: ${tmpl.priority}",
                                                fontSize = 9.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    onClick = {
                                        title = tmpl.title
                                        description = tmpl.description
                                        priority = tmpl.priority
                                        category = tmpl.category
                                        
                                        val match = users.find { it.role == tmpl.defaultAssigneeRole }
                                        assignedEmail = match?.email ?: "Unassigned"
                                        
                                        subtasks = DatabaseConverters().toSubtaskList(tmpl.subtasksJson)
                                        expandedTemplateSelect = false
                                    }
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Scope Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Button to save active task configuration as a template
                OutlinedButton(
                    onClick = { showSaveTemplateNameDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Template", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save configuration as reusable template", fontSize = 11.sp)
                }

                // Category dropdown Select
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        label = { Text("Task Category") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedCategory = true }
                    )
                    DropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        listOf("Features", "Bugs", "Design", "Marketing").forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                // Priority Row selectors
                Column {
                    Text("Criticality Urgency", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { p ->
                            val isSel = priority == p
                            FilterChip(
                                selected = isSel,
                                onClick = { priority = p },
                                label = { Text(p, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Status selectors
                Column {
                    Text("Operational Status", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("TODO", "IN_PROGRESS", "REVIEW", "DONE").forEach { s ->
                            val isSel = status == s
                            FilterChip(
                                selected = isSel,
                                onClick = { status = s },
                                label = { Text(s, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                 // Assignee selection dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    val currentAssigneeName = if (assignedEmail == "Unassigned") "Unassigned" 
                    else users.find { it.email == assignedEmail }?.fullName ?: assignedEmail

                    OutlinedTextField(
                        value = currentAssigneeName,
                        onValueChange = {},
                        label = { Text("Responsible Engineer") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedAssignee = true }
                    )
                    DropdownMenu(
                        expanded = expandedAssignee,
                        onDismissRequest = { expandedAssignee = false },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Unassigned") },
                            onClick = {
                                assignedEmail = "Unassigned"
                                expandedAssignee = false
                            }
                        )
                        users.forEach { u ->
                            DropdownMenuItem(
                                text = { Text("${u.fullName} (${u.role})") },
                                onClick = {
                                    assignedEmail = u.email
                                    expandedAssignee = false
                                }
                            )
                        }
                    }
                }

                Divider()

                // Subtask criteria additions
                Text("Operational Checkpoints", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    subtasks.forEach { s ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("• ${s.title}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            IconButton(
                                onClick = { subtasks = subtasks.filter { it.id != s.id } },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove subtask", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newSubtaskTitle,
                            onValueChange = { newSubtaskTitle = it },
                            placeholder = { Text("e.g. Test Docker build scripts", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (newSubtaskTitle.trim().isNotEmpty()) {
                                    val newSub = Subtask(
                                        id = UUID.randomUUID().toString(),
                                        title = newSubtaskTitle.trim(),
                                        isCompleted = false
                                    )
                                    subtasks = subtasks + newSub
                                    newSubtaskTitle = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add subtask", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    )

    if (showSaveTemplateNameDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTemplateNameDialog = false },
            title = { Text("Save Reusable Workflow Template", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newTemplateNameInput.trim()
                        if (name.isNotEmpty()) {
                            val currentAssigneeRole = if (assignedEmail == "Unassigned") "Unassigned"
                            else users.find { it.email == assignedEmail }?.role ?: "Unassigned"

                            viewModel.saveAsTemplate(
                                templateName = name,
                                title = title,
                                description = description,
                                priority = priority,
                                defaultAssigneeRole = currentAssigneeRole,
                                category = category,
                                subtasks = subtasks
                            )
                            newTemplateNameInput = ""
                            showSaveTemplateNameDialog = false
                        }
                    }
                ) {
                    Text("Save Template")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateNameDialog = false }) {
                    Text("Cancel")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a recognizable name for this workflow sequence:", fontSize = 11.sp)
                    OutlinedTextField(
                        value = newTemplateNameInput,
                        onValueChange = { newTemplateNameInput = it },
                        placeholder = { Text("e.g. Bi-Weekly Server Deployment") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}
