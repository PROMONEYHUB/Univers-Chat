package com.example.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.rounded.Bolt
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosIndigo
import com.example.ui.theme.IosTeal
import com.example.ui.theme.OnlinePresenceGreen
import com.example.ui.theme.PulseBrushes
import com.google.firebase.auth.PhoneAuthProvider
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
    var verificationId by rememberSaveable { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var resendSecondsRemaining by rememberSaveable { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(authStep, resendSecondsRemaining) {
        if (authStep == PhoneAuthStep.VERIFY_OTP && resendSecondsRemaining > 0) {
            delay(1000L)
            resendSecondsRemaining -= 1
        }
    }

    fun requestOtp(isResend: Boolean = false) {
        val activity = context.findActivity()
        if (activity == null) {
            errorMessage = "Unable to access the active screen for verification."
            return
        }
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
        isLoading = true
        submittedPhoneE164 = formattedE164

        startPhoneNumberVerification(
            activity = activity,
            phoneNumberE164 = formattedE164,
            resendToken = if (isResend) resendToken else null,
            onCodeSent = { vId, token ->
                haptics.success()
                verificationId = vId
                resendToken = token
                resendSecondsRemaining = 60
                isLoading = false
                if (!isResend) {
                    otpCode = ""
                    authStepName = PhoneAuthStep.VERIFY_OTP.name
                }
            },
            onAutoVerified = { credential ->
                coroutineScope.launch {
                    val result = signInWithPhoneCredential(credential, displayName)
                    isLoading = false
                    result.onSuccess {
                        haptics.success()
                        onAuthSuccess()
                    }.onFailure { err ->
                        errorMessage = mapPhoneAuthException(err)
                    }
                }
            },
            onVerificationFailed = { msg ->
                isLoading = false
                errorMessage = msg
            }
        )
    }

    fun submitOtp(code: String = otpCode) {
        val vId = verificationId
        val cleaned = code.filter { it.isDigit() }.take(6)
        if (vId.isNullOrBlank()) {
            errorMessage = "Verification session expired. Please request a new code."
            return
        }
        if (cleaned.length != 6) {
            errorMessage = "Enter the full 6-digit verification code."
            return
        }
        haptics.lightTap()
        errorMessage = null
        isLoading = true
        coroutineScope.launch {
            val result = verifyPhoneOtpAndSignIn(
                verificationId = vId,
                otpCode = cleaned,
                displayName = displayName
            )
            isLoading = false
            result.onSuccess {
                haptics.success()
                onAuthSuccess()
            }.onFailure { err ->
                errorMessage = mapPhoneAuthException(err)
            }
        }
    }

    val primaryGlow = MaterialTheme.colorScheme.primary
    val accentTealGlow = IosTeal
    val accentIndigoGlow = IosIndigo

    val ctaInteraction = remember { MutableInteractionSource() }
    val isCtaPressed by ctaInteraction.collectIsPressedAsState()
    val ctaScale by animateFloatAsState(
        targetValue = if (isCtaPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cta_scale"
    )

    val formattedCandidate = formatE164PhoneNumber(selectedCountry.dialCode, phoneInput)
    val canSendCode = !isLoading && isValidE164PhoneNumber(formattedCandidate)
    val canVerifyOtp = !isLoading && otpCode.length == 6

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryGlow.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.10f),
                        radius = size.maxDimension * 0.52f
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.10f),
                    radius = size.maxDimension * 0.52f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentTealGlow.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.10f, size.height * 0.60f),
                        radius = size.maxDimension * 0.44f
                    ),
                    center = Offset(size.width * 0.10f, size.height * 0.60f),
                    radius = size.maxDimension * 0.44f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentIndigoGlow.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.82f, size.height * 0.90f),
                        radius = size.maxDimension * 0.36f
                    ),
                    center = Offset(size.width * 0.82f, size.height * 0.90f),
                    radius = size.maxDimension * 0.36f
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                modifier = Modifier.border(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PulseBrushes.BrandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(OnlinePresenceGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                        .border(
                            width = 0.8.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Box {
                        Image(
                            painter = painterResource(id = R.drawable.img_auth_hero),
                            contentDescription = stringResource(R.string.auth_hero_desc),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f))
                                    )
                                )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-14).dp, y = 14.dp)
                        .border(
                            width = 0.8.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(IosBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sms,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Instant OTP Sign-In",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Global SMS · Real-Time Chat",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (authStep == PhoneAuthStep.ENTER_PHONE) {
                    stringResource(R.string.auth_headline)
                } else {
                    "Enter verification code"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (authStep == PhoneAuthStep.ENTER_PHONE) {
                    stringResource(R.string.auth_subtitle)
                } else {
                    "We sent a 6-digit SMS code to $submittedPhoneE164"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 0.9.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(24.dp)
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
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
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
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            width = 0.8.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable(enabled = !isLoading) {
                                            haptics.lightTap()
                                            showCountryPicker = true
                                        }
                                        .testTag("country_code_picker_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
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
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("phone_number_input")
                                )
                            }

                            Text(
                                text = "All countries supported. You'll receive a 6-digit SMS code to verify your number.",
                                style = MaterialTheme.typography.labelSmall,
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
                                    enabled = !isLoading,
                                    modifier = Modifier.testTag("change_phone_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Change number", fontWeight = FontWeight.SemiBold)
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
                                            imageVector = Icons.Rounded.MarkEmailRead,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "6-digit OTP",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            OtpDigitBoxes(
                                code = otpCode,
                                enabled = !isLoading,
                                onCodeChange = { updated ->
                                    val digitsOnly = updated.filter { it.isDigit() }.take(6)
                                    if (digitsOnly.length != otpCode.length) {
                                        haptics.selectionTick()
                                    }
                                    otpCode = digitsOnly
                                    errorMessage = null
                                    if (digitsOnly.length == 6 && !isLoading) {
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
                                        enabled = !isLoading,
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
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
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

            Spacer(modifier = Modifier.height(18.dp))

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
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = ctaScale
                        scaleY = ctaScale
                    }
                    .testTag(
                        if (authStep == PhoneAuthStep.ENTER_PHONE) "send_otp_button"
                        else "verify_otp_button"
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (authStep == PhoneAuthStep.ENTER_PHONE) {
                            stringResource(R.string.sending_code)
                        } else {
                            stringResource(R.string.verifying_code)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    Text(
                        text = if (authStep == PhoneAuthStep.ENTER_PHONE) {
                            stringResource(R.string.send_otp_code)
                        } else {
                            stringResource(R.string.verify_and_continue)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
