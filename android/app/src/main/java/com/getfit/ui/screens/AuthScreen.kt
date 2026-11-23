package com.getfit.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.R
import com.getfit.data.remote.AuthService
import com.getfit.notification.NotificationHelper
import com.getfit.ui.components.CleanProgressSpinner
import com.getfit.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(
    viewModel: ProfileViewModel,
    onAuthSuccess: (isNewUser: Boolean) -> Unit,
    onContinueWithoutSignIn: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(60) }
    var resendAttempts by remember { mutableIntStateOf(0) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(otpSent, resendAttempts) {
        if (otpSent) {
            countdownSeconds = 60
            while (countdownSeconds > 0) {
                delay(1000L)
                countdownSeconds--
            }
        }
    }

    // Request notification permission once right after login/signup
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        NotificationHelper.setPromptedNotification(context, true)
    }

    fun promptNotificationOnceIfNeeded() {
        if (!NotificationHelper.hasPromptedNotification(context)) {
            NotificationHelper.setPromptedNotification(context, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (onNavigateBack != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Profile",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // App Logo
            Image(
                painter = painterResource(id = R.drawable.ic_getfit_logo),
                contentDescription = "GetFit Logo",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                modifier = Modifier
                    .height(48.dp)
                    .padding(vertical = 4.dp),
                contentScale = ContentScale.Fit
            )

            // Header titles
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isSignUpMode) "Create Your Account" else "Welcome Back",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isSignUpMode) {
                        "Sign up to sync your workouts and nutrition safely across devices."
                    } else {
                        "Log in to restore your workouts, nutrition, and water history."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }

            // Mode Selector Toggle (Log In / Sign Up)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isSignUpMode) MaterialTheme.colorScheme.surface else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isSignUpMode = false
                                errorMessage = null
                                otpSent = false
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Log In",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (!isSignUpMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isSignUpMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSignUpMode) MaterialTheme.colorScheme.surface else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isSignUpMode = true
                                errorMessage = null
                                otpSent = false
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign Up",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSignUpMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSignUpMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Error Banner
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,

                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // Form Content
            if (!isSignUpMode) {
                // LOG IN FORM
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (email.isBlank() || !email.contains("@")) {
                                errorMessage = "Please enter a valid email address."
                                return@Button
                            }
                            if (password.isBlank()) {
                                errorMessage = "Please enter your password."
                                return@Button
                            }
                            errorMessage = null
                            viewModel.login(email.trim(), password) { success, msg ->
                                if (success) {
                                    promptNotificationOnceIfNeeded()
                                    onAuthSuccess(false) // existing user -> goes straight to dashboard
                                } else {
                                    errorMessage = msg
                                }
                            }
                        },
                        enabled = !uiState.isSyncing && email.isNotBlank() && password.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uiState.isSyncing) {
                            CleanProgressSpinner(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (uiState.isSyncing) "Logging In..." else "Log In",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // SIGN UP FORM
                if (!otpSent) {
                    // Step 1: Enter Name, Email, Password
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; errorMessage = null },
                            label = { Text("Your Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = { Text("Password (min 6 characters)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val trimmedEmail = email.trim()
                                if (name.isBlank()) {
                                    errorMessage = "Please enter your name."
                                    return@Button
                                }
                                if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
                                    errorMessage = "Please enter a valid email address."
                                    return@Button
                                }
                                if (AuthService.isDisposableEmail(trimmedEmail)) {
                                    errorMessage = "Temporary or disposable email addresses are not allowed. Please enter your real email address."
                                    return@Button
                                }
                                if (password.length < 6) {
                                    errorMessage = "Password must be at least 6 characters long."
                                    return@Button
                                }

                                errorMessage = null
                                viewModel.sendOtp(trimmedEmail) { success, msg ->
                                    if (success) {
                                        resendAttempts = 0
                                        countdownSeconds = 60
                                        otpSent = true
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            },
                            enabled = !uiState.isSyncing && name.isNotBlank() && email.isNotBlank() && password.length >= 6,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (uiState.isSyncing) {
                                CleanProgressSpinner(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (uiState.isSyncing) "Sending Verification Code..." else "Send Verification Code",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Step 2: Enter 6-digit OTP code
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MarkEmailRead,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Verification code sent to:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = email.trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Please check your inbox (and spam folder) and enter the 6-digit code below.",
                                    style = MaterialTheme.typography.bodySmall,

                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = if (countdownSeconds > 0) "Code expires in ${countdownSeconds}s" else "Code has expired! Please click 'Resend Code'.",
                                    style = MaterialTheme.typography.bodySmall,

                                    fontWeight = FontWeight.SemiBold,
                                    color = if (countdownSeconds > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = {
                                if (it.length <= 6) {
                                    otpCode = it.filter { ch -> ch.isDigit() }
                                    errorMessage = null
                                }
                            },
                            label = { Text("6-Digit Verification Code") },
                            placeholder = { Text("123456") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (countdownSeconds <= 0) {
                                    errorMessage = "This verification code has expired. Please tap 'Resend Code'."
                                    return@Button
                                }
                                if (otpCode.length < 6) {
                                    errorMessage = "Please enter the complete 6-digit verification code."
                                    return@Button
                                }
                                errorMessage = null
                                viewModel.register(email.trim(), password, name.trim(), otpCode.trim()) { success, msg ->
                                    if (success) {
                                        promptNotificationOnceIfNeeded()
                                        onAuthSuccess(true) // new user
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            },
                            enabled = !uiState.isSyncing && otpCode.length == 6 && countdownSeconds > 0,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (uiState.isSyncing) {
                                CleanProgressSpinner(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (uiState.isSyncing) "Creating Account..." else "Verify & Create Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    otpSent = false
                                    otpCode = ""
                                    resendAttempts = 0
                                    countdownSeconds = 60
                                    errorMessage = null
                                }
                            ) {
                                Text("Change Email", style = MaterialTheme.typography.labelMedium)
                            }

                            val canResend = !uiState.isSyncing && countdownSeconds == 0 && resendAttempts < 3
                            TextButton(
                                onClick = {
                                    if (resendAttempts >= 3) {
                                        errorMessage = "Maximum resend limit reached. Please wait 15 minutes."
                                        return@TextButton
                                    }
                                    errorMessage = null
                                    viewModel.sendOtp(email.trim()) { success, msg ->
                                        if (success) {
                                            resendAttempts++
                                            otpCode = ""
                                            errorMessage = "New code sent to your email!"
                                        } else {
                                            errorMessage = msg
                                        }
                                    }
                                },
                                enabled = canResend
                            ) {
                                Text(
                                    text = when {
                                        resendAttempts >= 3 -> "Limit Reached"
                                        countdownSeconds > 0 -> "Resend in ${countdownSeconds}s"
                                        else -> "Resend Code (${3 - resendAttempts} left)"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            if (!otpSent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "OR",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }

                TextButton(
                    onClick = {
                        promptNotificationOnceIfNeeded()
                        if (onNavigateBack != null) {
                            onNavigateBack()
                        } else {
                            onContinueWithoutSignIn()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (onNavigateBack != null) "Back to Profile" else "Continue without signing in",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
