package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserEntity
import com.example.data.TaskEntity
import com.example.viewmodel.TaskViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminScreen(viewModel: TaskViewModel) {
    val users by viewModel.users.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    val context = LocalContext.current
    var adminTabSelection by remember { mutableStateOf("users") } // "users", "tasks"
    
    // Bulk state trackers
    val selectedUsers = remember { mutableStateListOf<String>() }
    val selectedTasks = remember { mutableStateListOf<Int>() }

    var showUserDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<UserEntity?>(null) } // null means adding new user
    
    // User Form State
    var formEmail by remember { mutableStateOf("") }
    var formFullName by remember { mutableStateOf("") }
    var formPassword by remember { mutableStateOf("") }
    var formRole by remember { mutableStateOf("VIEWER") } // "ADMIN", "EDITOR", "VIEWER"

    // Task Editing dialog trigger
    var showTaskEditDialog by remember { mutableStateOf(false) }
    var selectedTaskForEdit by remember { mutableStateOf<TaskEntity?>(null) }

    // Dropdown expanded states
    var roleDropdownExpanded by remember { mutableStateOf(false) }

    // Clear selections on tab switch
    LaunchedEffect(adminTabSelection) {
        selectedUsers.clear()
        selectedTasks.clear()
    }

    // Launch user form setup when edit is selected
    LaunchedEffect(selectedUserForEdit) {
        selectedUserForEdit?.let {
            formEmail = it.email
            formFullName = it.fullName
            formPassword = ""
            formRole = it.role.uppercase()
        } ?: run {
            formEmail = ""
            formFullName = ""
            formPassword = ""
            formRole = "VIEWER"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Section Summary
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
                    }
                    Column {
                        Text(
                            text = "Admin Management Console",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Identity directories, pipeline authorization states, and override logs.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Sub tab navigation selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = adminTabSelection == "users",
                onClick = { adminTabSelection = "users" },
                label = { Text("User Directory (${users.size})", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.People, "Users Icon", modifier = Modifier.size(16.dp)) }
            )

            FilterChip(
                selected = adminTabSelection == "tasks",
                onClick = { adminTabSelection = "tasks" },
                label = { Text("Global Task Ledger (${tasks.size})", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Dns, "Tasks Icon", modifier = Modifier.size(16.dp)) }
            )
        }

        // Action & list rendering depending on selection
        if (adminTabSelection == "users") {
            // User Header Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "System User Registry",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (selectedUsers.isNotEmpty()) {
                        Button(
                            onClick = {
                                viewModel.adminDeleteUsers(selectedUsers.toList()) { ok, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    if (ok) {
                                        selectedUsers.clear()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Prune Selected Users", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Selected (${selectedUsers.size})", fontSize = 10.sp)
                        }
                    }
                }
                
                Button(
                    onClick = {
                        selectedUserForEdit = null
                        showUserDialog = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add User", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add User", fontSize = 11.sp)
                }
            }

            // User directory table list
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (users.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No users loaded.", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    val deletableUsers = users.filter { it.email != currentUser?.email }
                    val allUsersSelected = deletableUsers.isNotEmpty() && selectedUsers.size == deletableUsers.size

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            // Table Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1.5f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = allUsersSelected,
                                        onCheckedChange = { checked ->
                                            selectedUsers.clear()
                                            if (checked) {
                                                selectedUsers.addAll(deletableUsers.map { it.email })
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text("USER / CREDENTIALS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("ROLE", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.8f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("ACTIONS", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider()
                        }

                        items(users) { user ->
                            val isCurrentSessionAdmin = user.email == currentUser?.email
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Checkbox(
                                    checked = selectedUsers.contains(user.email),
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (!selectedUsers.contains(user.email)) {
                                                selectedUsers.add(user.email)
                                            }
                                        } else {
                                            selectedUsers.remove(user.email)
                                        }
                                    },
                                    enabled = !isCurrentSessionAdmin,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                // Left details
                                Row(
                                    modifier = Modifier.weight(1.5f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.fullName.take(2).uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Column {
                                        Text(user.fullName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(user.email, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }

                                // Role Display Pill
                                Box(
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (user.role.uppercase()) {
                                                "ADMIN" -> Color(0xFFFFECE0)
                                                "EDITOR", "MANAGER" -> Color(0xFFF0E5FF)
                                                else -> Color(0xFFE3F2FD)
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when(user.role.uppercase()) {
                                            "MANAGER" -> "EDITOR"
                                            "MEMBER" -> "VIEWER"
                                            else -> user.role
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when (user.role.uppercase()) {
                                            "ADMIN" -> Color(0xFFE65100)
                                            "EDITOR", "MANAGER" -> Color(0xFF6200EA)
                                            else -> Color(0xFF0D47A1)
                                        }
                                    )
                                }

                                // Row actions
                                Row(
                                    modifier = Modifier.weight(0.8f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            selectedUserForEdit = user
                                            showUserDialog = true
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit User", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.adminDeleteUser(user.email) { ok, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(30.dp),
                                        enabled = !isCurrentSessionAdmin
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete User",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isCurrentSessionAdmin) Color.LightGray else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Task ledger tab active
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Operational Overrides: All Pipeline Tasks",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (selectedTasks.isNotEmpty()) {
                        Button(
                            onClick = {
                                viewModel.adminDeleteTasks(selectedTasks.toList()) { ok, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    if (ok) {
                                        selectedTasks.clear()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Bulk Delete Tasks", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Selected (${selectedTasks.size})", fontSize = 10.sp)
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (tasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tasks registered in database.", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    val allTasksSelected = tasks.isNotEmpty() && selectedTasks.size == tasks.size

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            // Table Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1.5f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = allTasksSelected,
                                        onCheckedChange = { checked ->
                                            selectedTasks.clear()
                                            if (checked) {
                                                selectedTasks.addAll(tasks.map { it.id })
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text("TASK SPECIFICATION", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("STATE", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.7f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("OVERRIDE ACTIONS", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider()
                        }

                        items(tasks) { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Checkbox(
                                    checked = selectedTasks.contains(task.id),
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (!selectedTasks.contains(task.id)) {
                                                selectedTasks.add(task.id)
                                            }
                                        } else {
                                            selectedTasks.remove(task.id)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                // Task details
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(task.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when(task.priority) {
                                                "HIGH" -> Icons.Default.PriorityHigh
                                                else -> Icons.Default.LowPriority
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(10.dp),
                                            tint = when(task.priority) {
                                                "HIGH" -> Color.Red
                                                "MEDIUM" -> Color.DarkGray
                                                else -> Color.Gray
                                            }
                                        )
                                        Text("${task.category} • Assg: ${task.assignedName}", fontSize = 9.sp, color = Color.Gray)
                                    }
                                }

                                // Status Pill
                                Box(
                                    modifier = Modifier
                                        .weight(0.7f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (task.status) {
                                                "TODO" -> MaterialTheme.colorScheme.outlineVariant
                                                "IN_PROGRESS" -> MaterialTheme.colorScheme.primaryContainer
                                                "REVIEW" -> MaterialTheme.colorScheme.tertiaryContainer
                                                else -> Color(0xFFC8E6C9)
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = task.status,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when (task.status) {
                                            "TODO" -> MaterialTheme.colorScheme.onSurfaceVariant
                                            "IN_PROGRESS" -> MaterialTheme.colorScheme.onPrimaryContainer
                                            "REVIEW" -> MaterialTheme.colorScheme.onTertiaryContainer
                                            else -> Color(0xFF1B5E20)
                                        }
                                    )
                                }

                                // Actions
                                Row(
                                    modifier = Modifier.weight(0.7f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            // Admin directly invokes Delete on VM
                                            viewModel.deleteTask(task.id, task.title)
                                            Toast.makeText(context, "Deleted task: ${task.title}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Prune Task", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // USER MANAGMENT DIALOG
    if (showUserDialog) {
        AlertDialog(
            onDismissRequest = { showUserDialog = false },
            title = {
                Text(
                    text = if (selectedUserForEdit == null) "Add Network Identifier" else "Update User Credentials",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = formFullName,
                        onValueChange = { formFullName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = formEmail,
                        onValueChange = { formEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = selectedUserForEdit == null // Email is primary key
                    )

                    OutlinedTextField(
                        value = formPassword,
                        onValueChange = { formPassword = it },
                        label = { Text(if (selectedUserForEdit == null) "Password Phrase" else "Reset Password Phrase (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Role Picker Dropdown trigger
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formRole,
                            onValueChange = {},
                            label = { Text("Workspace RBAC Role") },
                            modifier = Modifier.fillMaxWidth().clickable { roleDropdownExpanded = true },
                            readOnly = true,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            trailingIcon = {
                                IconButton(onClick = { roleDropdownExpanded = !roleDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Open drop down")
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = roleDropdownExpanded,
                            onDismissRequest = { roleDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Admin (Full Owner Control)") },
                                onClick = {
                                    formRole = "ADMIN"
                                    roleDropdownExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Editor (Manage Pipelines/Subtasks)") },
                                onClick = {
                                    formRole = "MANAGER" // mapped internally to MANAGER
                                    roleDropdownExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Viewer (Read/Observe States Only)") },
                                onClick = {
                                    formRole = "VIEWER" // VIEWER / MEMBER constraint
                                    roleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedUserForEdit == null) {
                            // CREATE
                            viewModel.adminCreateUser(
                                fullName = formFullName,
                                email = formEmail,
                                passwordRaw = formPassword,
                                role = formRole
                            ) { ok, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (ok) showUserDialog = false
                            }
                        } else {
                            // UPDATE
                            viewModel.adminUpdateUser(
                                email = formEmail,
                                fullName = formFullName,
                                passwordRaw = formPassword,
                                role = formRole
                            ) { ok, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (ok) showUserDialog = false
                            }
                        }
                    }
                ) {
                    Text(if (selectedUserForEdit == null) "Register" else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUserDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
