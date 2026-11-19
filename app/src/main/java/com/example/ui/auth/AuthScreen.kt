package com.example.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.TelegramLogoMark
import com.example.ui.components.rememberCosmicAuraHeaderBrush
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.TelegramBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PhoneAuthStep {
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

    var authStepName by rememberSaveable { mutableStateOf(PhoneAuthStep.ENTER_PHONE.name) }
    val authStep = remember(authStepName) { PhoneAuthStep.valueOf(authStepName) }
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
        TelegramOtpDeliveryMode.valueOf(telegramDeliveryModeName)
    }
    var resendSecondsRemaining by rememberSaveable { mutableIntStateOf(0) }
    var isPhoneLoading by remember { mutableStateOf(false) }
    var isServerWakingUp by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // Pre-warm the Render OTP server as soon as the sign-in screen opens
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

    BackHandler(enabled = authStep == PhoneAuthStep.VERIFY_OTP && !isPhoneLoading) {
        errorMessage = null
        otpCode = ""
        authStepName = PhoneAuthStep.ENTER_PHONE.name
    }

    LaunchedEffect(authStep, resendSecondsRemaining) {
        if (authStep == PhoneAuthStep.VERIFY_OTP && resendSecondsRemaining > 0) {
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
                    authStepName = PhoneAuthStep.VERIFY_OTP.name
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

    val ctaInteraction = remember { MutableInteractionSource() }
    val isCtaPressed by ctaInteraction.collectIsPressedAsState()
    val ctaScale by animateFloatAsState(
        targetValue = if (isCtaPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cta_scale"
    )

    val formattedCandidate = formatE164PhoneNumber(selectedCountry.dialCode, phoneInput)
    val canSendCode = !isPhoneLoading && isValidE164PhoneNumber(formattedCandidate)
    val canVerifyOtp = !isPhoneLoading && otpCode.length == 6
    val headerBrush = rememberCosmicAuraHeaderBrush(isDark = isDark)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cosmic Header Banner with App Logo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBrush)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 520.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier
                        .size(84.dp)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.24f),
                            shape = CircleShape
                        )
                        .testTag("auth_app_logo")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        TelegramLogoMark(size = 66.dp)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (authStep == PhoneAuthStep.ENTER_PHONE) {
                        stringResource(R.string.auth_subtitle)
                    } else {
                        "Check Telegram for the 6-digit OTP sent by our bot for $submittedPhoneE164"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Main Form Column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (authStep == PhoneAuthStep.ENTER_PHONE) {
                    "Sign in with Telegram Bot OTP"
                } else {
                    "Enter your Telegram OTP"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 0.8.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                AnimatedContent(
                    targetState = authStep,
                    transitionSpec = {
                        val forward = targetState == PhoneAuthStep.VERIFY_OTP
                        (slideInHorizontally(
                            animationSpec = tween(260),
                            initialOffsetX = { if (forward) it / 4 else -it / 4 }
                        ) + fadeIn(tween(240))) togetherWith (slideOutHorizontally(
                            animationSpec = tween(220),
                            targetOffsetX = { if (forward) -it / 5 else it / 5 }
                        ) + fadeOut(tween(180)))
                    },
                    label = "phone_otp_step"
                ) { step ->
                    if (step == PhoneAuthStep.ENTER_PHONE) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "You will receive your 6-digit login OTP on Telegram via our official bot.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it.take(40) },
                                label = { Text("Your Name (optional)") },
                                placeholder = { Text("e.g. Alex Rivera") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.PersonOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_display_name_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .border(
                                            width = 0.8.dp,
                                            color = MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !isPhoneLoading) {
                                            haptics.lightTap()
                                            showCountryPicker = true
                                        }
                                        .testTag("country_code_picker_button")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .height(56.dp)
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = selectedCountry.flag, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = selectedCountry.dialCode,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "Choose country code",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = phoneInput,
                                    onValueChange = { next ->
                                        val filtered = next.filterIndexed { index, ch ->
                                            ch.isDigit() || (index == 0 && ch == '+') || ch == ' ' || ch == '-'
                                        }
                                        phoneInput = filtered.take(18)
                                        errorMessage = null
                                    },
                                    label = { Text("Phone Number") },
                                    placeholder = { Text("555 123 4567") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.PhoneAndroid,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { if (canSendCode) requestOtp(isResend = false) }
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("phone_number_input")
                                )
                            }

                            Text(
                                text = "Enter your Telegram phone number. Our bot will send your 6-digit OTP directly on Telegram.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        haptics.lightTap()
                                        errorMessage = null
                                        otpCode = ""
                                        authStepName = PhoneAuthStep.ENTER_PHONE.name
                                    },
                                    enabled = !isPhoneLoading,
                                    modifier = Modifier.testTag("change_phone_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Wrong number?", fontWeight = FontWeight.SemiBold)
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Send,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = stringResource(R.string.telegram_otp_badge),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
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
                                            onClick = {
                                                haptics.lightTap()
                                                val opened = TelegramOtpClient.openTelegramBot(
                                                    context,
                                                    telegramBotDeepLink
                                                )
                                                if (!opened) {
                                                    errorMessage = "Could not open Telegram. Make sure Telegram is installed and try again."
                                                }
                                            },
                                            enabled = !isPhoneLoading,
                                            shape = RoundedCornerShape(10.dp),
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

                            OtpDigitBoxes(
                                code = otpCode,
                                enabled = !isPhoneLoading,
                                onCodeChange = { updated ->
                                    val digitsOnly = updated.filter { it.isDigit() }.take(6)
                                    if (digitsOnly.length != otpCode.length) {
                                        haptics.selectionTick()
                                    }
                                    otpCode = digitsOnly
                                    errorMessage = null
                                    if (digitsOnly.length == 6 && !isPhoneLoading) {
                                        submitOtp(digitsOnly)
                                    }
                                }
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (resendSecondsRemaining > 0) {
                                    Text(
                                        text = "Resend Telegram OTP in ${resendSecondsRemaining}s",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    TextButton(
                                        onClick = { requestOtp(isResend = true) },
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
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.8.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("auth_error_banner")
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (authStep == PhoneAuthStep.ENTER_PHONE) {
                        requestOtp(isResend = false)
                    } else {
                        submitOtp()
                    }
                },
                enabled = if (authStep == PhoneAuthStep.ENTER_PHONE) canSendCode else canVerifyOtp,
                interactionSource = ctaInteraction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TelegramBlue,
                    contentColor = Color.White,
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
                    .testTag(
                        if (authStep == PhoneAuthStep.ENTER_PHONE) "send_otp_button"
                        else "verify_otp_button"
                    )
            ) {
                if (isPhoneLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when {
                            isServerWakingUp -> stringResource(R.string.waking_up_otp_server)
                            authStep == PhoneAuthStep.VERIFY_OTP -> stringResource(R.string.verifying_code)
                            else -> stringResource(R.string.sending_telegram_otp)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    if (authStep == PhoneAuthStep.ENTER_PHONE) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (authStep == PhoneAuthStep.VERIFY_OTP) {
                            stringResource(R.string.verify_and_continue)
                        } else {
                            stringResource(R.string.send_telegram_otp)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isPhoneLoading && isServerWakingUp) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.waking_up_otp_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.auth_privacy_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
