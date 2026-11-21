package com.example.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.TelegramLogoMark
import com.example.ui.components.UniversMotion
import com.example.ui.components.bouncyPressScale
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIosSurfaces
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.LocalUniversAccent
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.data.repository.MessagingRepository
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AuthFlowStage {
    WELCOME,
    ENTER_PHONE,
    VERIFY_OTP
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberIosHaptics()
    val isDark = LocalIsTelegramDark.current
    val accent = LocalUniversAccent.current
    val iosSurfaces = LocalIosSurfaces.current
    val accentPrimary = accent.primary(isDark)

    var stageName by rememberSaveable { mutableStateOf(AuthFlowStage.WELCOME.name) }
    val stage = remember(stageName) {
        AuthFlowStage.entries.firstOrNull { it.name == stageName } ?: AuthFlowStage.WELCOME
    }
    var isSignUpMode by rememberSaveable { mutableStateOf(true) }

    var selectedCountryIso by rememberSaveable { mutableStateOf(defaultCountryDialCode().iso) }
    val selectedCountry = remember(selectedCountryIso) {
        AllCountryCodes.firstOrNull { it.iso == selectedCountryIso } ?: defaultCountryDialCode()
    }
    var showCountryPicker by rememberSaveable { mutableStateOf(false) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var phoneInput by rememberSaveable { mutableStateOf("") }
    var otpCode by rememberSaveable { mutableStateOf("") }
    var submittedPhoneE164 by rememberSaveable { mutableStateOf("") }
    var telegramSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var telegramBotDeepLink by rememberSaveable { mutableStateOf("") }
    var telegramDeliveryModeName by rememberSaveable { mutableStateOf(TelegramOtpDeliveryMode.OPEN_BOT.name) }
    val telegramDeliveryMode = remember(telegramDeliveryModeName) {
        TelegramOtpDeliveryMode.entries.firstOrNull { it.name == telegramDeliveryModeName }
            ?: TelegramOtpDeliveryMode.OPEN_BOT
    }
    var resendSecondsRemaining by rememberSaveable { mutableIntStateOf(0) }
    var isPhoneLoading by remember { mutableStateOf(false) }
    var isServerWakingUp by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // Pre-warm the Render OTP server as soon as the welcome/auth experience opens
    LaunchedEffect(Unit) {
        TelegramOtpClient.warmUpBackend()
    }

    LaunchedEffect(isPhoneLoading) {
        if (isPhoneLoading) {
            isServerWakingUp = false
            delay(5000L)
            isServerWakingUp = true
        } else {
            isServerWakingUp = false
        }
    }

    BackHandler(enabled = stage != AuthFlowStage.WELCOME && !isPhoneLoading) {
        errorMessage = null
        when (stage) {
            AuthFlowStage.VERIFY_OTP -> {
                otpCode = ""
                stageName = AuthFlowStage.ENTER_PHONE.name
            }
            AuthFlowStage.ENTER_PHONE -> {
                stageName = AuthFlowStage.WELCOME.name
            }
            AuthFlowStage.WELCOME -> Unit
        }
    }

    LaunchedEffect(stage, resendSecondsRemaining) {
        if (stage == AuthFlowStage.VERIFY_OTP && resendSecondsRemaining > 0) {
            delay(1000L)
            resendSecondsRemaining -= 1
        }
    }

    fun requestOtp(isResend: Boolean = false) {
        val formattedE164 = if (isResend && submittedPhoneE164.isNotBlank()) {
            submittedPhoneE164
        } else {
            formatE164PhoneNumber(selectedCountry.dialCode, phoneInput)
        }
        if (!isValidE164PhoneNumber(formattedE164)) {
            errorMessage = "Enter a valid phone number (7–15 digits with country code)."
            return
        }

        haptics.lightTap()
        errorMessage = null
        isPhoneLoading = true
        submittedPhoneE164 = formattedE164

        coroutineScope.launch {
            val result = TelegramOtpClient.requestOtp(
                phoneNumberE164 = formattedE164,
                displayName = displayName
            )
            isPhoneLoading = false
            result.onSuccess { response ->
                haptics.success()
                telegramSessionId = response.sessionId
                telegramBotDeepLink = response.botDeepLink
                telegramDeliveryModeName = response.deliveryMode.name
                resendSecondsRemaining = 45
                if (!isResend) {
                    otpCode = ""
                    stageName = AuthFlowStage.VERIFY_OTP.name
                }
                if (response.deliveryMode == TelegramOtpDeliveryMode.OPEN_BOT &&
                    response.botDeepLink.isNotBlank()
                ) {
                    TelegramOtpClient.openTelegramBot(context, response.botDeepLink)
                }
            }.onFailure { err ->
                errorMessage = err.localizedMessage
                    ?: "Unable to request OTP from our Telegram bot. Please try again."
            }
        }
    }

    fun submitOtp(code: String = otpCode) {
        val cleaned = code.filter { it.isDigit() }.take(6)
        if (cleaned.length != 6) {
            errorMessage = "Enter the full 6-digit OTP sent by our Telegram bot."
            return
        }
        val sessionId = telegramSessionId
        if (sessionId.isNullOrBlank()) {
            errorMessage = "Verification session expired. Please request a new Telegram OTP."
            return
        }
        haptics.lightTap()
        errorMessage = null
        isPhoneLoading = true

        coroutineScope.launch {
            val result = TelegramOtpClient.verifyOtpAndSignIn(
                context = context,
                sessionId = sessionId,
                phoneNumberE164 = submittedPhoneE164,
                otpCode = cleaned,
                fallbackDisplayName = displayName
            )
            isPhoneLoading = false
            result.onSuccess {
                haptics.success()
                onAuthSuccess()
            }.onFailure { err ->
                errorMessage = err.localizedMessage
                    ?: "Invalid or expired OTP. Please check your Telegram bot message and try again."
            }
        }
    }

    val bypassDemoLogin: () -> Unit = {
        haptics.success()
        isPhoneLoading = true
        errorMessage = null
        coroutineScope.launch {
            runCatching {
                val authResult = Firebase.auth.signInAnonymously().await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
                    val repo = MessagingRepository(db, Firebase.auth)
                    AuthProfilePrefill.pendingDisplayName = displayName.ifBlank { "Univers Demo User" }
                    AuthProfilePrefill.pendingPhoneNumber = submittedPhoneE164.ifBlank { "+15550199" }
                    repo.syncCurrentUserProfile(firebaseUser).getOrThrow()
                }
            }.onSuccess {
                isPhoneLoading = false
                onAuthSuccess()
            }.onFailure { err ->
                isPhoneLoading = false
                errorMessage = "Demo login failed: ${err.localizedMessage}"
            }
        }
    }

    val canvasSurfaceColor = if (isDark) Color(0xFF090B0E) else Color.White
    val formattedCandidate = formatE164PhoneNumber(selectedCountry.dialCode, phoneInput)
    val canSendCode = !isPhoneLoading && isValidE164PhoneNumber(formattedCandidate)
    val canVerifyOtp = !isPhoneLoading && otpCode.length == 6

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasSurfaceColor)
    ) {
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(
                    animationSpec = tween(UniversMotion.PUSH_ENTER_MS, easing = UniversMotion.FluidOutEasing),
                    initialOffsetX = { if (forward) (it * 0.28f).toInt() else -(it * 0.22f).toInt() }
                ) + fadeIn(tween(220, easing = UniversMotion.FluidOutEasing)) +
                    scaleIn(
                        initialScale = if (forward) 0.97f else 0.985f,
                        animationSpec = tween(UniversMotion.PUSH_ENTER_MS, easing = UniversMotion.FluidOutEasing)
                    )) togetherWith (slideOutHorizontally(
                    animationSpec = tween(UniversMotion.PUSH_EXIT_MS, easing = UniversMotion.FluidOutEasing),
                    targetOffsetX = { if (forward) -(it * 0.20f).toInt() else (it * 0.26f).toInt() }
                ) + fadeOut(tween(170, easing = UniversMotion.SoftInOutEasing)) +
                    scaleOut(
                        targetScale = if (forward) 0.965f else 0.98f,
                        animationSpec = tween(UniversMotion.PUSH_EXIT_MS, easing = UniversMotion.FluidOutEasing)
                    ))
            },
            label = "univers_flow_auth_stage"
        ) { currentStage ->
            when (currentStage) {
                AuthFlowStage.WELCOME -> {
                    UniversFlowWelcomeStage(
                        canvasColor = canvasSurfaceColor,
                        accentPrimary = accentPrimary,
                        onContinue = {
                            haptics.lightTap()
                            stageName = AuthFlowStage.ENTER_PHONE.name
                        }
                    )
                }

                AuthFlowStage.ENTER_PHONE -> {
                    UniversFlowSignUpStage(
                        canvasColor = canvasSurfaceColor,
                        accentPrimary = accentPrimary,
                        inputUnderlineColor = iosSurfaces.inputUnderline,
                        isSignUpMode = isSignUpMode,
                        displayName = displayName,
                        onDisplayNameChange = { displayName = it.take(40) },
                        selectedCountry = selectedCountry,
                        onOpenCountryPicker = {
                            haptics.lightTap()
                            showCountryPicker = true
                        },
                        phoneInput = phoneInput,
                        onPhoneInputChange = { next ->
                            val filtered = next.filterIndexed { index, ch ->
                                ch.isDigit() || (index == 0 && ch == '+') || ch == ' ' || ch == '-'
                            }
                            phoneInput = filtered.take(18)
                            errorMessage = null
                        },
                        canSendCode = canSendCode,
                        isPhoneLoading = isPhoneLoading,
                        isServerWakingUp = isServerWakingUp,
                        errorMessage = errorMessage,
                        onBack = {
                            haptics.lightTap()
                            errorMessage = null
                            stageName = AuthFlowStage.WELCOME.name
                        },
                        onSubmit = { requestOtp(isResend = false) },
                        onToggleAuthMode = {
                            haptics.selectionTick()
                            errorMessage = null
                            isSignUpMode = !isSignUpMode
                        }
                    )
                }

                AuthFlowStage.VERIFY_OTP -> {
                    UniversFlowVerifyOtpStage(
                        canvasColor = canvasSurfaceColor,
                        accentPrimary = accentPrimary,
                        submittedPhoneE164 = submittedPhoneE164,
                        telegramDeliveryMode = telegramDeliveryMode,
                        telegramBotDeepLink = telegramBotDeepLink,
                        otpCode = otpCode,
                        resendSecondsRemaining = resendSecondsRemaining,
                        canVerifyOtp = canVerifyOtp,
                        isPhoneLoading = isPhoneLoading,
                        isServerWakingUp = isServerWakingUp,
                        errorMessage = errorMessage,
                        onOtpCodeChange = { updated ->
                            val digitsOnly = updated.filter { it.isDigit() }.take(6)
                            if (digitsOnly.length != otpCode.length) {
                                haptics.selectionTick()
                            }
                            otpCode = digitsOnly
                            errorMessage = null
                            if (digitsOnly.length == 6 && !isPhoneLoading) {
                                submitOtp(digitsOnly)
                            }
                        },
                        onBack = {
                            haptics.lightTap()
                            errorMessage = null
                            otpCode = ""
                            stageName = AuthFlowStage.ENTER_PHONE.name
                        },
                        onOpenTelegramBot = {
                            haptics.lightTap()
                            val opened = TelegramOtpClient.openTelegramBot(context, telegramBotDeepLink)
                            if (!opened) {
                                errorMessage = "Could not open Telegram. Make sure Telegram is installed and try again."
                            }
                        },
                        onResendOtp = { requestOtp(isResend = true) },
                        onVerifyOtp = { submitOtp() },
                        onDemoLogin = bypassDemoLogin
                    )
                }
            }
        }
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            selectedCountry = selectedCountry,
            onSelect = { country ->
                haptics.selectionTick()
                selectedCountryIso = country.iso
                showCountryPicker = false
            },
            onDismiss = {
                haptics.lightTap()
                showCountryPicker = false
            }
        )
    }
}

@Composable
private fun UniversFlowWelcomeStage(
    canvasColor: Color,
    accentPrimary: Color,
    onContinue: () -> Unit
) {
    val isDark = LocalIsTelegramDark.current
    val accent = LocalUniversAccent.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
    ) {
        val heroHeight = (maxHeight * 0.56f).coerceIn(300.dp, 490.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            UniversFlowTopographicWaveHeader(
                height = heroHeight,
                cutoutColor = canvasColor,
                showBackButton = false,
                onBack = {},
                showBrandPill = true
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .widthIn(max = 520.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 28.dp)
                    .padding(top = 6.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Welcome",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Private messaging, voice notes, and calls.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                }

                // Bottom-right signature "Continue ──(→)" control matching reference layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .bouncyPressScale(0.95f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onContinue)
                            .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                            .testTag("auth_welcome_continue_button")
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // Clean high-contrast monochrome Continue control
                        Box(
                            contentAlignment = Alignment.CenterEnd,
                            modifier = Modifier.width(56.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .width(24.dp)
                                    .height(2.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface)
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Continue to Sign Up",
                                    tint = canvasColor,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UniversFlowSignUpStage(
    canvasColor: Color,
    accentPrimary: Color,
    inputUnderlineColor: Color,
    isSignUpMode: Boolean,
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    selectedCountry: CountryDialCode,
    onOpenCountryPicker: () -> Unit,
    phoneInput: String,
    onPhoneInputChange: (String) -> Unit,
    canSendCode: Boolean,
    isPhoneLoading: Boolean,
    isServerWakingUp: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onToggleAuthMode: () -> Unit
) {
    val isDark = LocalIsTelegramDark.current
    val accent = LocalUniversAccent.current
    val ctaInteraction = remember { MutableInteractionSource() }
    val isCtaPressed by ctaInteraction.collectIsPressedAsState()
    val ctaScale by animateFloatAsState(
        targetValue = if (isCtaPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "signup_cta_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UniversFlowTopographicWaveHeader(
            height = 248.dp,
            cutoutColor = canvasColor,
            showBackButton = true,
            onBack = onBack,
            showBrandPill = true
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 28.dp)
                .padding(bottom = 26.dp)
        ) {
            // Bold Heading + signature colored accent underline bar
            Column(modifier = Modifier.offset(y = (-8).dp)) {
                Text(
                    text = if (isSignUpMode) "Sign up" else "Sign in",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Name Field (emphasized in Sign Up, collapsible in Sign In)
            AnimatedVisibility(
                visible = isSignUpMode,
                enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                Column {
                    EditorialUnderlineField(
                        label = "Name",
                        value = displayName,
                        onValueChange = onDisplayNameChange,
                        placeholder = "Your name",
                        leadingIcon = Icons.Rounded.PersonOutline,
                        accentPrimary = MaterialTheme.colorScheme.onSurface,
                        unfocusedLineColor = inputUnderlineColor,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        inputTestTag = "auth_display_name_input"
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                }
            }

            // Phone Number Underline Field with inline Country Code selector
            EditorialPhoneUnderlineField(
                label = "Phone number",
                selectedCountry = selectedCountry,
                onOpenCountryPicker = onOpenCountryPicker,
                phoneInput = phoneInput,
                onPhoneInputChange = onPhoneInputChange,
                enabled = !isPhoneLoading,
                accentPrimary = MaterialTheme.colorScheme.onSurface,
                unfocusedLineColor = inputUnderlineColor,
                onDone = { if (canSendCode) onSubmit() }
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.8.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .testTag("auth_error_banner")
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Full-width monochrome high-contrast primary button
            val buttonShape = RoundedCornerShape(14.dp)
            Button(
                onClick = onSubmit,
                enabled = canSendCode,
                interactionSource = ctaInteraction,
                shape = buttonShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = canvasColor,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .graphicsLayer {
                        scaleX = ctaScale
                        scaleY = ctaScale
                    }
                    .testTag("send_otp_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isPhoneLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = canvasColor,
                            strokeWidth = 2.2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isServerWakingUp) {
                                stringResource(R.string.waking_up_otp_server)
                            } else {
                                stringResource(R.string.sending_telegram_otp)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = if (isSignUpMode) "Sign up" else "Sign in",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (isPhoneLoading && isServerWakingUp) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.waking_up_otp_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Footer Sign In / Sign Up Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isSignUpMode) "Sign in" else "Sign up",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleAuthMode)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("toggle_auth_mode_button")
                )
            }
        }
    }
}

@Composable
private fun UniversFlowVerifyOtpStage(
    canvasColor: Color,
    accentPrimary: Color,
    submittedPhoneE164: String,
    telegramDeliveryMode: TelegramOtpDeliveryMode,
    telegramBotDeepLink: String,
    otpCode: String,
    resendSecondsRemaining: Int,
    canVerifyOtp: Boolean,
    isPhoneLoading: Boolean,
    isServerWakingUp: Boolean,
    errorMessage: String?,
    onOtpCodeChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenTelegramBot: () -> Unit,
    onResendOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onDemoLogin: () -> Unit
) {
    val isDark = LocalIsTelegramDark.current
    val accent = LocalUniversAccent.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UniversFlowTopographicWaveHeader(
            height = 224.dp,
            cutoutColor = canvasColor,
            showBackButton = true,
            onBack = onBack,
            showBrandPill = true
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 28.dp)
                .padding(bottom = 26.dp)
        ) {
            Column(modifier = Modifier.offset(y = (-8).dp)) {
                Text(
                    text = "Verify OTP",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Enter the 6-digit code sent via Telegram for $submittedPhoneE164",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Telegram Bot Delivery Helper Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.42f else 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (telegramDeliveryMode == TelegramOtpDeliveryMode.SENT_DIRECTLY) {
                            stringResource(R.string.telegram_otp_direct_hint)
                        } else {
                            stringResource(R.string.telegram_otp_open_bot_hint)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (telegramBotDeepLink.isNotBlank()) {
                        OutlinedButton(
                            onClick = onOpenTelegramBot,
                            enabled = !isPhoneLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_telegram_bot_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.open_telegram_bot),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OtpDigitBoxes(
                code = otpCode,
                enabled = !isPhoneLoading,
                onCodeChange = onOtpCodeChange
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onDemoLogin,
                enabled = !isPhoneLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("demo_login_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "⚡ Instant Demo Login (Skip OTP)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack,
                    enabled = !isPhoneLoading,
                    modifier = Modifier.testTag("change_phone_button")
                ) {
                    Text(
                        text = "Wrong number?",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (resendSecondsRemaining > 0) {
                    Text(
                        text = "Resend in ${resendSecondsRemaining}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    TextButton(
                        onClick = onResendOtp,
                        enabled = !isPhoneLoading,
                        modifier = Modifier.testTag("resend_otp_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resend OTP", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.8.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .testTag("auth_error_banner")
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val buttonShape = RoundedCornerShape(14.dp)
            Button(
                onClick = onVerifyOtp,
                enabled = canVerifyOtp,
                shape = buttonShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = canvasColor,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("verify_otp_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isPhoneLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = canvasColor,
                            strokeWidth = 2.2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isServerWakingUp) {
                                stringResource(R.string.waking_up_otp_server)
                            } else {
                                stringResource(R.string.verifying_code)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.verify_and_continue),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Editorial minimalist underline input matching the reference aesthetic:
 * bold small label above, subtle leading icon + crisp "|" vertical divider + borderless input + animated bottom line.
 */
@Composable
private fun EditorialUnderlineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    accentPrimary: Color,
    unfocusedLineColor: Color,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    inputTestTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val lineColor by animateColorAsState(
        targetValue = if (isFocused || value.isNotBlank()) accentPrimary else unfocusedLineColor,
        animationSpec = tween(180),
        label = "underline_color"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isFocused) accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(16.dp)
                    .background(unfocusedLineColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    interactionSource = interactionSource,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(accentPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(inputTestTag)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFocused) 1.8.dp else 1.1.dp)
                .background(lineColor)
        )
    }
}

@Composable
private fun EditorialPhoneUnderlineField(
    label: String,
    selectedCountry: CountryDialCode,
    onOpenCountryPicker: () -> Unit,
    phoneInput: String,
    onPhoneInputChange: (String) -> Unit,
    enabled: Boolean,
    accentPrimary: Color,
    unfocusedLineColor: Color,
    onDone: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val lineColor by animateColorAsState(
        targetValue = if (isFocused || phoneInput.isNotBlank()) accentPrimary else unfocusedLineColor,
        animationSpec = tween(180),
        label = "phone_underline_color"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .defaultMinSize(minHeight = 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled, onClick = onOpenCountryPicker)
                    .padding(end = 4.dp)
                    .testTag("country_code_picker_button")
            ) {
                Text(text = selectedCountry.flag, fontSize = 17.sp)
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = selectedCountry.dialCode,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Choose country code",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(unfocusedLineColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Rounded.PhoneAndroid,
                contentDescription = null,
                tint = if (isFocused) accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (phoneInput.isEmpty()) {
                    Text(
                        text = "98765 43210",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                    )
                }
                BasicTextField(
                    value = phoneInput,
                    onValueChange = onPhoneInputChange,
                    enabled = enabled,
                    singleLine = true,
                    interactionSource = interactionSource,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(accentPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("phone_number_input")
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFocused) 1.8.dp else 1.1.dp)
                .background(lineColor)
        )
    }
}

/**
 * Clean flat-color topographic contour header with the sweeping bottom S-wave cutout.
 */
@Composable
private fun UniversFlowTopographicWaveHeader(
    height: Dp,
    cutoutColor: Color,
    showBackButton: Boolean,
    onBack: () -> Unit,
    showBrandPill: Boolean
) {
    val isDark = LocalIsTelegramDark.current
    val headerFillColor = if (isDark) Color(0xFF161920) else Color(0xFF1B2028)

    val scratchPath = remember { Path() }
    val wavePath = remember { Path() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Calm matte charcoal editorial backdrop
            drawRect(color = headerFillColor)

            // 2. Subtle static topographic contour lines
            drawTopographicContours(
                w = w,
                h = h,
                drift = 0f,
                isDark = true,
                scratchPath = scratchPath
            )

            // 3. Sweeping S-wave cutout into the form canvas
            wavePath.reset()
            wavePath.moveTo(0f, h * 0.80f)
            wavePath.cubicTo(
                w * 0.24f, h * 0.69f,
                w * 0.44f, h * 0.88f,
                w * 0.68f, h * 0.94f
            )
            wavePath.cubicTo(
                w * 0.84f, h * 0.98f,
                w * 0.94f, h * 0.94f,
                w, h * 0.87f
            )
            wavePath.lineTo(w, h + 4f)
            wavePath.lineTo(0f, h + 4f)
            wavePath.close()
            drawPath(path = wavePath, color = cutoutColor)
        }

        // Top bar inside safe status bar area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier
                        .bouncyPressScale(0.92f)
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack)
                        .testTag("auth_stage_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (showBrandPill) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("auth_app_logo")
                ) {
                    TelegramLogoMark(size = 26.dp)
                    Text(
                        text = "Univers Chat",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private val ClusterAScales = floatArrayOf(0.14f, 0.24f, 0.36f, 0.50f, 0.66f)
private val ClusterBScales = floatArrayOf(0.12f, 0.22f, 0.34f, 0.48f)

private fun DrawScope.drawTopographicContours(
    w: Float,
    h: Float,
    drift: Float,
    isDark: Boolean,
    scratchPath: Path
) {
    val strokeAlphaBase = if (isDark) 0.14f else 0.24f
    val shiftX = drift * w * 0.018f
    val shiftY = drift * h * 0.014f

    // Cluster A: Upper-right organic topographic rings
    val centerAX = w * 0.68f + shiftX
    val centerAY = h * 0.18f - shiftY
    for (i in ClusterAScales.indices) {
        val scale = ClusterAScales[i]
        val rx = w * scale * 0.82f
        val ry = h * scale * 0.68f
        scratchPath.reset()
        scratchPath.moveTo(centerAX - rx, centerAY + ry * 0.08f)
        scratchPath.cubicTo(
            centerAX - rx * 0.92f, centerAY - ry * 0.98f,
            centerAX + rx * 0.52f, centerAY - ry * 1.12f,
            centerAX + rx * 0.96f, centerAY - ry * 0.14f
        )
        scratchPath.cubicTo(
            centerAX + rx * 1.15f, centerAY + ry * 0.72f,
            centerAX + rx * 0.18f, centerAY + ry * 1.18f,
            centerAX - rx * 0.64f, centerAY + ry * 0.76f
        )
        scratchPath.cubicTo(
            centerAX - rx * 1.05f, centerAY + ry * 0.54f,
            centerAX - rx * 1.06f, centerAY + ry * 0.30f,
            centerAX - rx, centerAY + ry * 0.08f
        )
        scratchPath.close()
        drawPath(
            path = scratchPath,
            color = Color.White.copy(alpha = (strokeAlphaBase - i * 0.02f).coerceAtLeast(0.08f)),
            style = Stroke(width = (1.6f - i * 0.12f).dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // Cluster B: Mid-left flowing contour island
    val centerBX = w * 0.20f - shiftX
    val centerBY = h * 0.50f + shiftY
    for (i in ClusterBScales.indices) {
        val scale = ClusterBScales[i]
        val rx = w * scale * 0.88f
        val ry = h * scale * 0.54f
        scratchPath.reset()
        scratchPath.moveTo(centerBX - rx * 0.9f, centerBY - ry * 0.15f)
        scratchPath.cubicTo(
            centerBX - rx * 0.55f, centerBY - ry * 1.05f,
            centerBX + rx * 0.65f, centerBY - ry * 0.85f,
            centerBX + rx * 0.98f, centerBY + ry * 0.12f
        )
        scratchPath.cubicTo(
            centerBX + rx * 0.78f, centerBY + ry * 1.08f,
            centerBX - rx * 0.48f, centerBY + ry * 0.92f,
            centerBX - rx * 0.9f, centerBY - ry * 0.15f
        )
        scratchPath.close()
        drawPath(
            path = scratchPath,
            color = Color.White.copy(alpha = (strokeAlphaBase - i * 0.022f).coerceAtLeast(0.07f)),
            style = Stroke(width = (1.5f - i * 0.1f).dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // Cluster C: Sweeping lower-right ridge ribbon flowing along the S-wave crest
    val ribbonStroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
    for (i in 0..3) {
        val yOffset = h * (0.56f + i * 0.085f) + shiftY * (if (i % 2 == 0) 0.7f else -0.5f)
        scratchPath.reset()
        scratchPath.moveTo(-w * 0.06f, yOffset + h * 0.08f)
        scratchPath.cubicTo(
            w * 0.28f, yOffset - h * 0.12f,
            w * 0.56f, yOffset + h * 0.16f,
            w * 1.08f, yOffset - h * 0.04f
        )
        drawPath(
            path = scratchPath,
            color = Color.White.copy(alpha = (strokeAlphaBase * 0.9f - i * 0.02f).coerceAtLeast(0.07f)),
            style = ribbonStroke
        )
    }
}
