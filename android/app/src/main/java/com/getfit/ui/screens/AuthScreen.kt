package com.getfit.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.getfit.R
import com.getfit.data.remote.AuthService
import com.getfit.notification.NotificationHelper
import com.getfit.ui.components.core.GfBrandLogo
import com.getfit.ui.components.core.GfCard
import com.getfit.ui.components.core.GfPrimaryButton
import com.getfit.ui.components.core.GfSegmentedControl
import com.getfit.ui.components.core.GfTextButton
import com.getfit.ui.components.core.GfTextField
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

    Scaffold(
        containerColor = GetFitTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(GetFitTheme.colors.background)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenGutter, vertical = Dimens.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.lg)
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
                            tint = GetFitTheme.colors.textPrimary
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(Dimens.lg))
            }

            // App Logo
            GfBrandLogo(
                iconSize = 36.dp,
                textStyle = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = Dimens.xs)
            )

            // Header titles
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.xs)
            ) {
                Text(
                    text = if (isSignUpMode) "Create Your Account" else "Welcome Back",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = GetFitTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isSignUpMode) {
                        "Sign up to sync your workouts and nutrition safely across devices."
                    } else {
                        "Log in to restore your workouts, nutrition, and water history."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = GetFitTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Mode Selector Toggle (Log In / Sign Up)
            GfSegmentedControl(
                items = listOf("Log In", "Sign Up"),
                selectedIndex = if (isSignUpMode) 1 else 0,
                onSelect = { index ->
                    isSignUpMode = index == 1
                    errorMessage = null
                    otpSent = false
                }
            )

            // Error Banner
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(Dimens.md),
                    color = GetFitTheme.colors.danger.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, GetFitTheme.colors.danger.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = GetFitTheme.colors.danger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = Dimens.md, vertical = Dimens.sm)
                    )
                }
            }

            // Form Content
            if (!isSignUpMode) {
                // LOG IN FORM
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.md)
                ) {
                    GfTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = "Email Address",
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    GfTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = "Password",
                        leadingIcon = Icons.Default.Lock,
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = GetFitTheme.colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Dimens.xs))

                    GfPrimaryButton(
                        text = "Log In",
                        isLoading = uiState.isSyncing,
                        enabled = !uiState.isSyncing && email.isNotBlank() && password.isNotBlank(),
                        onClick = {
                            if (email.isBlank() || !email.contains("@")) {
                                errorMessage = "Please enter a valid email address."
                                return@GfPrimaryButton
                            }
                            if (password.isBlank()) {
                                errorMessage = "Please enter your password."
                                return@GfPrimaryButton
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
                        }
                    )
                }
            } else {
                // SIGN UP FORM
                if (!otpSent) {
                    // Step 1: Enter Name, Email, Password
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.md)
                    ) {
                        GfTextField(
                            value = name,
                            onValueChange = { name = it; errorMessage = null },
                            label = "Your Name",
                            leadingIcon = Icons.Default.Person,
                            imeAction = ImeAction.Next,
                            modifier = Modifier.fillMaxWidth()
                        )

                        GfTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            label = "Email Address",
                            leadingIcon = Icons.Default.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        GfTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = "Password (min 6 characters)",
                            leadingIcon = Icons.Default.Lock,
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = GetFitTheme.colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(Dimens.xs))

                        GfPrimaryButton(
                            text = "Send Verification Code",
                            isLoading = uiState.isSyncing,
                            enabled = !uiState.isSyncing && name.isNotBlank() && email.isNotBlank() && password.length >= 6,
                            onClick = {
                                val trimmedEmail = email.trim()
                                if (name.isBlank()) {
                                    errorMessage = "Please enter your name."
                                    return@GfPrimaryButton
                                }
                                if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
                                    errorMessage = "Please enter a valid email address."
                                    return@GfPrimaryButton
                                }
                                if (AuthService.isDisposableEmail(trimmedEmail)) {
                                    errorMessage = "Temporary or disposable email addresses are not allowed. Please enter your real email address."
                                    return@GfPrimaryButton
                                }
                                if (password.length < 6) {
                                    errorMessage = "Password must be at least 6 characters long."
                                    return@GfPrimaryButton
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
                            }
                        )
                    }
                } else {
                    // Step 2: Enter 6-digit OTP code
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.md)
                    ) {
                        GfCard(
                            cornerRadius = Dimens.lg,
                            contentPadding = PaddingValues(Dimens.lg),
                            useGradient = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                                ) {
                                    Icon(
                                        Icons.Default.MarkEmailRead,
                                        contentDescription = null,
                                        tint = GetFitTheme.colors.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Verification code sent to:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GetFitTheme.colors.textSecondary
                                    )
                                }
                                Text(
                                    text = email.trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GetFitTheme.colors.textPrimary
                                )
                                Text(
                                    text = "Please check your inbox (and spam folder) and enter the 6-digit code below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GetFitTheme.colors.textSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = if (countdownSeconds > 0) "Code expires in ${countdownSeconds}s" else "Code has expired! Please click 'Resend Code'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (countdownSeconds > 0) GetFitTheme.colors.accent else GetFitTheme.colors.danger,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        GfTextField(
                            value = otpCode,
                            onValueChange = {
                                if (it.length <= 6) {
                                    otpCode = it.filter { ch -> ch.isDigit() }
                                    errorMessage = null
                                }
                            },
                            label = "6-Digit Verification Code",
                            placeholder = "123456",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )

                        GfPrimaryButton(
                            text = "Verify & Create Account",
                            isLoading = uiState.isSyncing,
                            enabled = !uiState.isSyncing && otpCode.length == 6 && countdownSeconds > 0,
                            onClick = {
                                if (countdownSeconds <= 0) {
                                    errorMessage = "This verification code has expired. Please tap 'Resend Code'."
                                    return@GfPrimaryButton
                                }
                                if (otpCode.length < 6) {
                                    errorMessage = "Please enter the complete 6-digit verification code."
                                    return@GfPrimaryButton
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
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GfTextButton(
                                text = "Change Email",
                                color = GetFitTheme.colors.textSecondary,
                                onClick = {
                                    otpSent = false
                                    otpCode = ""
                                    resendAttempts = 0
                                    countdownSeconds = 60
                                    errorMessage = null
                                }
                            )

                            val canResend = !uiState.isSyncing && countdownSeconds == 0 && resendAttempts < 3
                            GfTextButton(
                                text = when {
                                    resendAttempts >= 3 -> "Limit Reached"
                                    countdownSeconds > 0 -> "Resend in ${countdownSeconds}s"
                                    else -> "Resend Code (${3 - resendAttempts} left)"
                                },
                                enabled = canResend,
                                onClick = {
                                    if (resendAttempts >= 3) {
                                        errorMessage = "Maximum resend limit reached. Please wait 15 minutes."
                                        return@GfTextButton
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
                                }
                            )
                        }
                    }
                }
            }

            if (!otpSent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = GetFitTheme.colors.hairline
                    )
                    Text(
                        text = "OR",
                        color = GetFitTheme.colors.textTertiary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = Dimens.md)
                    )
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = GetFitTheme.colors.hairline
                    )
                }

                GfTextButton(
                    text = if (onNavigateBack != null) "Back to Profile" else "Continue without signing in",
                    color = GetFitTheme.colors.textSecondary,
                    onClick = {
                        promptNotificationOnceIfNeeded()
                        if (onNavigateBack != null) {
                            onNavigateBack()
                        } else {
                            onContinueWithoutSignIn()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(Dimens.lg))
        }
    }
}
