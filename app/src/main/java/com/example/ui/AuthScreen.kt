package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.TaskViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(viewModel: TaskViewModel) {
    val isMfaRequired by viewModel.isMfaRequired.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val currentMfaOtp by viewModel.currentMfaOtp.collectAsState()

    var isRegisterMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon Logo placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security App Icon",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CollabTask Workspace",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "Secure Decentralized Replication Network",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = isMfaRequired,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "AuthViewState"
            ) { mfaActive ->
                if (mfaActive) {
                    MfaChallengeView(
                        simulatedOtp = currentMfaOtp,
                        authError = authError,
                        onVerify = { code -> viewModel.verifyMfa(code) },
                        onCancel = { viewModel.logout() }
                    )
                } else {
                    if (isRegisterMode) {
                        RegisterCard(
                            authError = authError,
                            onRegister = { name, email, pass, role ->
                                viewModel.register(name, email, pass, role)
                            },
                            toggleMode = {
                                isRegisterMode = false
                                viewModel.logout()
                            }
                        )
                    } else {
                        LoginCard(
                            authError = authError,
                            onLogin = { email, pass -> viewModel.login(email, pass) },
                            toggleMode = {
                                isRegisterMode = true
                                viewModel.logout()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginCard(
    authError: String?,
    onLogin: (String, String) -> Unit,
    toggleMode: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Account Sign In",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            if (authError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = authError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onLogin(email, password) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Verify Profile Credentials", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = toggleMode) {
                Text("Register team member profile", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Premium UX Autofill Shortcut
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Instant Reviewer Sign-In",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AutofillChip(label = "Admin") {
                    email = "admin@collabtask.com"
                    password = "admin123"
                }
                AutofillChip(label = "Manager") {
                    email = "manager@collabtask.com"
                    password = "manager123"
                }
                AutofillChip(label = "Developer") {
                    email = "member@collabtask.com"
                    password = "member123"
                }
            }
        }
    }
}

@Composable
fun AutofillChip(label: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterCard(
    authError: String?,
    onRegister: (String, String, String, String) -> Unit,
    toggleMode: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("MEMBER") } // "ADMIN", "MANAGER", "MEMBER"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Register Team Account",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Work Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("New Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Role selector row
            Text(
                text = "Access Authorization Level",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("ADMIN", "System Admin"),
                    Pair("MANAGER", "Manager"),
                    Pair("MEMBER", "Core Developer")
                ).forEach { (role, label) ->
                    val isSel = selectedRole == role
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedRole = role },
                        label = { Text(label, fontSize = 11.sp) },
                        leadingIcon = if (isSel) {
                            { Icon(Icons.Default.Check, contentDescription = "Checked", modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }

            if (authError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = authError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onRegister(fullName, email, password, selectedRole) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Verify & Setup MFA", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = toggleMode) {
                Text("Return to sign in", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun MfaChallengeView(
    simulatedOtp: String?,
    authError: String?,
    onVerify: (String) -> Unit,
    onCancel: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Shield",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Multi-Factor Authentication Required",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Enter the 6-digit confirmation code generated below to authorize this session.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Dynamic secure code banner
            if (simulatedOtp != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { 
                            code = simulatedOtp 
                            keyboardController?.hide()
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SIMULATED PHONE SMS / EMAIL RECEIVED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = simulatedOtp,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            letterSpacing = 4.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "(Tap code directly to autofill verification grid)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 6) code = it },
                label = { Text("6-Digit OTP Code") },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = "OTP Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            if (authError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = authError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { onVerify(code) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Authorize")
                }
            }
        }
    }
}
