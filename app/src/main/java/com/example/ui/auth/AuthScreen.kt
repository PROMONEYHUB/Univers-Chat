package com.example.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.components.TelegramLogoMark
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramDarkBar
import com.example.ui.theme.TelegramSteelBlue
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PhoneAuthStep {
    ENTER_PHONE,
    VERIFY_OTP
}

private enum class OtpChannel {
    TELEGRAM_BOT,
    SMS
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
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var authStepName by rememberSaveable { mutableStateOf(PhoneAuthStep.ENTER_PHONE.name) }
    val authStep = remember(authStepName) { PhoneAuthStep.valueOf(authStepName) }
    var otpChannelName by rememberSaveable { mutableStateOf(OtpChannel.TELEGRAM_BOT.name) }
    val otpChannel = remember(otpChannelName) { OtpChannel.valueOf(otpChannelName) }
    var selectedCountryIso by rememberSaveable { mutableStateOf(defaultCountryDialCode().iso) }
    val selectedCountry = remember(selectedCountryIso) {
        AllCountryCodes.firstOrNull { it.iso == selectedCountryIso } ?: defaultCountryDialCode()
    }
    var showCountryPicker by rememberSaveable { mutableStateOf(false) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var phoneInput by rememberSaveable { mutableStateOf("") }
    var otpCode by rememberSaveable { mutableStateOf("") }
    var submittedPhoneE164 by rememberSaveable { mutableStateOf("") }
    var verificationId by rememberSaveable { mutableStateOf<String?>(null) }
    var telegramSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var telegramBotDeepLink by rememberSaveable { mutableStateOf("") }
    var telegramDeliveryModeName by rememberSaveable { mutableStateOf(TelegramOtpDeliveryMode.OPEN_BOT.name) }
    val telegramDeliveryMode = remember(telegramDeliveryModeName) {
        TelegramOtpDeliveryMode.valueOf(telegramDeliveryModeName)
    }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var resendSecondsRemaining by rememberSaveable { mutableIntStateOf(0) }
    var isPhoneLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val isBusy = isPhoneLoading || isGoogleLoading

    BackHandler(enabled = authStep == PhoneAuthStep.VERIFY_OTP && !isBusy) {
        errorMessage = null
        otpCode = ""
        authStepName = PhoneAuthStep.ENTER_PHONE.name
    }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = coroutineScope
        )
    }

    LaunchedEffect(authStep, resendSecondsRemaining) {
        if (authStep == PhoneAuthStep.VERIFY_OTP && resendSecondsRemaining > 0) {
            delay(1000L)
            resendSecondsRemaining -= 1
        }
    }

    fun requestOtp(isResend: Boolean = false, overrideChannel: OtpChannel? = null) {
        val activeChannel = overrideChannel ?: otpChannel
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

        if (activeChannel == OtpChannel.TELEGRAM_BOT) {
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
                        ?: "Unable to request Telegram OTP. Try switching to SMS OTP."
                }
            }
            return
        }

        val activity = context.findActivity()
        if (activity == null) {
            isPhoneLoading = false
            errorMessage = "Unable to access the active screen for verification."
            return
        }

        startPhoneNumberVerification(
            activity = activity,
            phoneNumberE164 = formattedE164,
            resendToken = if (isResend) resendToken else null,
            onCodeSent = { vId, token ->
                haptics.success()
                verificationId = vId
                resendToken = token
                resendSecondsRemaining = 60
                isPhoneLoading = false
                if (!isResend) {
                    otpCode = ""
                    authStepName = PhoneAuthStep.VERIFY_OTP.name
                }
            },
            onAutoVerified = { credential ->
                coroutineScope.launch {
                    val result = signInWithPhoneCredential(credential, displayName)
                    isPhoneLoading = false
                    result.onSuccess {
                        haptics.success()
                        onAuthSuccess()
                    }.onFailure { err ->
                        errorMessage = mapPhoneAuthException(err)
                    }
                }
            },
            onVerificationFailed = { msg ->
                isPhoneLoading = false
                errorMessage = msg
            }
        )
    }

    fun submitOtp(code: String = otpCode) {
        val cleaned = code.filter { it.isDigit() }.take(6)
        if (cleaned.length != 6) {
            errorMessage = "Enter the full 6-digit verification code."
            return
        }
        haptics.lightTap()
        errorMessage = null
        isPhoneLoading = true

        if (otpChannel == OtpChannel.TELEGRAM_BOT) {
            val sessionId = telegramSessionId
            if (sessionId.isNullOrBlank()) {
                isPhoneLoading = false
                errorMessage = "Telegram OTP session expired. Please request a new code."
                return
            }
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
                        ?: "Invalid or expired Telegram OTP. Please check Telegram and try again."
                }
            }
            return
        }

        val vId = verificationId
        if (vId.isNullOrBlank()) {
            isPhoneLoading = false
            errorMessage = "Verification session expired. Please request a new code."
            return
        }
        coroutineScope.launch {
            val result = verifyPhoneOtpAndSignIn(
                verificationId = vId,
                otpCode = cleaned,
                displayName = displayName
            )
            isPhoneLoading = false
            result.onSuccess {
                haptics.success()
                onAuthSuccess()
            }.onFailure { err ->
                errorMessage = mapPhoneAuthException(err)
            }
        }
    }

    fun triggerGoogleSignIn() {
        if (isBusy) return
        haptics.lightTap()
        errorMessage = null
        isGoogleLoading = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isGoogleLoading = false
                haptics.success()
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isGoogleLoading = false
                errorMessage = msg
            },
            scope = coroutineScope,
            onAuthCancelled = {
                isGoogleLoading = false
            }
        )
    }

    val ctaInteraction = remember { MutableInteractionSource() }
    val isCtaPressed by ctaInteraction.collectIsPressedAsState()
    val ctaScale by animateFloatAsState(
        targetValue = if (isCtaPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cta_scale"
    )

    val formattedCandidate = formatE164PhoneNumber(selectedCountry.dialCode, phoneInput)
    val canSendCode = !isBusy && isValidE164PhoneNumber(formattedCandidate)
    val canVerifyOtp = !isBusy && otpCode.length == 6
    val headerBrush = if (isDark) {
        Brush.verticalGradient(listOf(TelegramDarkBar, MaterialTheme.colorScheme.background))
    } else {
        Brush.verticalGradient(listOf(TelegramSteelBlue, TelegramBlue))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Classic Telegram Steel-Blue Header Banner
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
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.size(82.dp)
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
                    } else if (otpChannel == OtpChannel.TELEGRAM_BOT) {
                        "Verify $submittedPhoneE164 with your 6-digit Telegram Bot code"
                    } else {
                        "We sent a 6-digit SMS code to $submittedPhoneE164"
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
                    stringResource(R.string.auth_headline)
                } else {
                    "Enter verification code"
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
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "phone_otp_step"
                ) { step ->
                    if (step == PhoneAuthStep.ENTER_PHONE) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Segmented verification channel toggle: Telegram Bot (free & instant) vs SMS OTP
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    OtpChannelChip(
                                        selected = otpChannel == OtpChannel.TELEGRAM_BOT,
                                        label = stringResource(R.string.otp_channel_telegram),
                                        icon = Icons.AutoMirrored.Rounded.Send,
                                        enabled = !isBusy,
                                        onClick = {
                                            haptics.selectionTick()
                                            errorMessage = null
                                            otpChannelName = OtpChannel.TELEGRAM_BOT.name
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("channel_telegram_bot")
                                    )
                                    OtpChannelChip(
                                        selected = otpChannel == OtpChannel.SMS,
                                        label = stringResource(R.string.otp_channel_sms),
                                        icon = Icons.Rounded.Sms,
                                        enabled = !isBusy,
                                        onClick = {
                                            haptics.selectionTick()
                                            errorMessage = null
                                            otpChannelName = OtpChannel.SMS.name
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("channel_sms_otp")
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
                                        .clickable(enabled = !isBusy) {
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
                                text = if (otpChannel == OtpChannel.TELEGRAM_BOT) {
                                    "Receive your free 6-digit verification code instantly inside Telegram."
                                } else {
                                    "Please select your country code and enter your phone number."
                                },
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
                                    enabled = !isBusy,
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
                                            imageVector = if (otpChannel == OtpChannel.TELEGRAM_BOT) {
                                                Icons.AutoMirrored.Rounded.Send
                                            } else {
                                                Icons.Rounded.MarkEmailRead
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (otpChannel == OtpChannel.TELEGRAM_BOT) {
                                                stringResource(R.string.telegram_otp_badge)
                                            } else {
                                                "6-digit SMS code"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (otpChannel == OtpChannel.TELEGRAM_BOT) {
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
                                                        errorMessage = "Could not open Telegram. Install Telegram or tap Use SMS OTP instead."
                                                    }
                                                },
                                                enabled = !isBusy,
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
                            }

                            OtpDigitBoxes(
                                code = otpCode,
                                enabled = !isBusy,
                                onCodeChange = { updated ->
                                    val digitsOnly = updated.filter { it.isDigit() }.take(6)
                                    if (digitsOnly.length != otpCode.length) {
                                        haptics.selectionTick()
                                    }
                                    otpCode = digitsOnly
                                    errorMessage = null
                                    if (digitsOnly.length == 6 && !isBusy) {
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
                                        text = "Resend code in ${resendSecondsRemaining}s",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    TextButton(
                                        onClick = { requestOtp(isResend = true) },
                                        enabled = !isBusy,
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

                            if (otpChannel == OtpChannel.TELEGRAM_BOT) {
                                TextButton(
                                    onClick = {
                                        haptics.lightTap()
                                        errorMessage = null
                                        otpCode = ""
                                        otpChannelName = OtpChannel.SMS.name
                                        requestOtp(isResend = false, overrideChannel = OtpChannel.SMS)
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier.testTag("switch_to_sms_otp_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Sms,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.switch_to_sms_otp),
                                        fontWeight = FontWeight.SemiBold
                                    )
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
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when {
                            authStep == PhoneAuthStep.VERIFY_OTP -> stringResource(R.string.verifying_code)
                            otpChannel == OtpChannel.TELEGRAM_BOT -> stringResource(R.string.sending_telegram_otp)
                            else -> stringResource(R.string.sending_code)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    if (authStep == PhoneAuthStep.ENTER_PHONE && otpChannel == OtpChannel.TELEGRAM_BOT) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = when {
                            authStep == PhoneAuthStep.VERIFY_OTP -> stringResource(R.string.verify_and_continue)
                            otpChannel == OtpChannel.TELEGRAM_BOT -> stringResource(R.string.send_telegram_otp)
                            else -> stringResource(R.string.send_otp_code)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "OR",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = ::triggerGoogleSignIn,
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isGoogleLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = TelegramBlue,
                        strokeWidth = 2.4.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.signing_in_google),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    GoogleMarkIcon(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.sign_in_with_google),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
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

@Composable
private fun OtpChannelChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GoogleMarkIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.21f
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)

        drawArc(
            color = GoogleRed,
            startAngle = -145f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawArc(
            color = GoogleYellow,
            startAngle = 145f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawArc(
            color = GoogleGreen,
            startAngle = 45f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawArc(
            color = GoogleBlue,
            startAngle = -5f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        val centerY = size.height / 2f
        drawLine(
            color = GoogleBlue,
            start = Offset(size.width * 0.50f, centerY),
            end = Offset(size.width - inset * 0.4f, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt
        )
    }
}
