package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.model.CallSession
import com.example.ui.components.UniversMotion
import com.example.ui.components.UserAvatar
import com.example.ui.components.bouncyPressScale
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.DynamicIslandDarkBorder
import com.example.ui.theme.DynamicIslandDarkSurface
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosRed
import com.example.ui.viewmodel.ActiveCallUiState
import com.example.ui.viewmodel.CallConnectionPhase
import org.webrtc.SurfaceViewRenderer
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

private val CallStageBgTop = Color(0xFF14171D)
private val CallStageBgBottom = Color(0xFF0A0C10)
private val CallHudSurface = Color(0xE0181B22)
private val CallControlSurface = Color(0xFF262A34)

@Composable
fun CallHostOverlay(
    currentUserId: String,
    incomingCall: CallSession?,
    callState: ActiveCallUiState,
    onAcceptIncoming: (CallSession) -> Unit,
    onDeclineIncoming: (CallSession) -> Unit,
    onHangUp: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onCycleQuality: () -> Unit,
    onSetMinimized: (Boolean) -> Unit,
    onAttachLocalRenderer: (SurfaceViewRenderer) -> Unit,
    onDetachLocalRenderer: (SurfaceViewRenderer) -> Unit,
    onAttachRemoteRenderer: (SurfaceViewRenderer) -> Unit,
    onDetachRemoteRenderer: (SurfaceViewRenderer) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSession = callState.session

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = activeSession != null && !callState.isMinimized,
            enter = fadeIn(tween(220, easing = UniversMotion.FluidOutEasing)) +
                slideInVertically(tween(260, easing = UniversMotion.FluidOutEasing)) { it / 12 },
            exit = fadeOut(tween(180, easing = UniversMotion.FluidOutEasing)) +
                slideOutVertically(tween(200, easing = UniversMotion.FluidOutEasing)) { it / 12 }
        ) {
            if (activeSession != null) {
                BackHandler {
                    onSetMinimized(true)
                }
                ActiveCallFullScreen(
                    currentUserId = currentUserId,
                    session = activeSession,
                    callState = callState,
                    onHangUp = onHangUp,
                    onToggleMute = onToggleMute,
                    onToggleSpeaker = onToggleSpeaker,
                    onToggleCamera = onToggleCamera,
                    onSwitchCamera = onSwitchCamera,
                    onCycleQuality = onCycleQuality,
                    onMinimize = { onSetMinimized(true) },
                    onAttachLocalRenderer = onAttachLocalRenderer,
                    onDetachLocalRenderer = onDetachLocalRenderer,
                    onAttachRemoteRenderer = onAttachRemoteRenderer,
                    onDetachRemoteRenderer = onDetachRemoteRenderer
                )
            }
        }

        AnimatedVisibility(
            visible = activeSession != null && callState.isMinimized,
            enter = fadeIn(tween(180, easing = UniversMotion.FluidOutEasing)) +
                slideInVertically(tween(220, easing = UniversMotion.FluidOutEasing)) { -it },
            exit = fadeOut(tween(150, easing = UniversMotion.FluidOutEasing)) +
                slideOutVertically(tween(180, easing = UniversMotion.FluidOutEasing)) { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            if (activeSession != null) {
                MinimizedCallIslandBar(
                    currentUserId = currentUserId,
                    session = activeSession,
                    callState = callState,
                    onExpand = { onSetMinimized(false) },
                    onHangUp = onHangUp
                )
            }
        }

        AnimatedVisibility(
            visible = incomingCall != null && activeSession == null,
            enter = fadeIn(tween(240, easing = UniversMotion.FluidOutEasing)) +
                slideInVertically(tween(260, easing = UniversMotion.FluidOutEasing)) { -it / 8 },
            exit = fadeOut(tween(180, easing = UniversMotion.FluidOutEasing)) +
                slideOutVertically(tween(200, easing = UniversMotion.FluidOutEasing)) { -it / 8 }
        ) {
            if (incomingCall != null) {
                IncomingCallFullOverlay(
                    callSession = incomingCall,
                    onAccept = { onAcceptIncoming(incomingCall) },
                    onDecline = { onDeclineIncoming(incomingCall) }
                )
            }
        }
    }
}

@Composable
private fun NeutralCallBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CallStageBgTop, CallStageBgBottom)
                )
            )
    )
}

@Composable
private fun ActiveCallFullScreen(
    currentUserId: String,
    session: CallSession,
    callState: ActiveCallUiState,
    onHangUp: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onCycleQuality: () -> Unit,
    onMinimize: () -> Unit,
    onAttachLocalRenderer: (SurfaceViewRenderer) -> Unit,
    onDetachLocalRenderer: (SurfaceViewRenderer) -> Unit,
    onAttachRemoteRenderer: (SurfaceViewRenderer) -> Unit,
    onDetachRemoteRenderer: (SurfaceViewRenderer) -> Unit
) {
    val haptics = rememberIosHaptics()
    val peerName = session.peerNameFor(currentUserId)
    val peerPhoto = session.peerPhotoUrlFor(currentUserId)
    val isVideo = session.isVideoCall

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CallStageBgBottom)
    ) {
        if (isVideo) {
            RemoteVideoRendererSurface(
                onAttach = onAttachRemoteRenderer,
                onDetach = onDetachRemoteRenderer,
                modifier = Modifier.fillMaxSize()
            )
            if (!callState.hasRemoteVideo || callState.phase != CallConnectionPhase.CONNECTED) {
                NeutralCallBackdrop()
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CalmAvatarCallStage(
                        name = peerName,
                        photoUrl = peerPhoto,
                        statusLine = callState.phase.label,
                        showVoiceEqualizer = false
                    )
                }
            }
        } else {
            NeutralCallBackdrop()
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CalmAvatarCallStage(
                    name = peerName,
                    photoUrl = peerPhoto,
                    statusLine = if (callState.phase == CallConnectionPhase.CONNECTED) {
                        formatCallDuration(callState.durationSeconds)
                    } else {
                        callState.phase.label
                    },
                    showVoiceEqualizer = callState.phase == CallConnectionPhase.CONNECTED && !callState.isMuted
                )
            }
        }

        // Top Clean Call Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    haptics.lightTap()
                    onMinimize()
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CallControlSurface)
                    .testTag("call_minimize_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = "Minimize call",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (callState.phase == CallConnectionPhase.CONNECTED) {
                            formatCallDuration(callState.durationSeconds)
                        } else {
                            callState.phase.label
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }

            if (isVideo) {
                Surface(
                    shape = CircleShape,
                    color = CallControlSurface,
                    modifier = Modifier
                        .clip(CircleShape)
                        .minimumInteractiveComponentSize()
                        .clickable {
                            haptics.selectionTick()
                            onCycleQuality()
                        }
                        .testTag("call_quality_toggle_button")
                ) {
                    Text(
                        text = callState.qualityPreset.shortBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Local Self-View PIP Card (Video Calls)
        if (isVideo && callState.isCameraEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = 132.dp)
                    .size(width = 112.dp, height = 158.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(DynamicIslandDarkSurface)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                LocalVideoRendererSurface(
                    onAttach = onAttachLocalRenderer,
                    onDetach = onDetachLocalRenderer,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptics.selectionTick()
                            onSwitchCamera()
                        }
                        .testTag("call_pip_flip_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.62f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Cameraswitch,
                                contentDescription = "Flip camera",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Clean Control Bar
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = CallHudSurface,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .border(0.6.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallControlCircleButton(
                    icon = if (callState.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    label = if (callState.isMuted) "Unmute" else "Mute",
                    active = callState.isMuted,
                    onClick = {
                        haptics.selectionTick()
                        onToggleMute()
                    },
                    testTag = "call_mute_button"
                )

                CallControlCircleButton(
                    icon = if (callState.isSpeakerOn) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                    label = if (callState.isSpeakerOn) "Speaker" else "Audio",
                    active = callState.isSpeakerOn,
                    onClick = {
                        haptics.selectionTick()
                        onToggleSpeaker()
                    },
                    testTag = "call_speaker_button"
                )

                if (isVideo) {
                    CallControlCircleButton(
                        icon = if (callState.isCameraEnabled) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                        label = if (callState.isCameraEnabled) "Video" else "Off",
                        active = !callState.isCameraEnabled,
                        onClick = {
                            haptics.selectionTick()
                            onToggleCamera()
                        },
                        testTag = "call_camera_button"
                    )

                    CallControlCircleButton(
                        icon = Icons.Rounded.Cameraswitch,
                        label = "Flip",
                        active = false,
                        onClick = {
                            haptics.selectionTick()
                            onSwitchCamera()
                        },
                        testTag = "call_flip_camera_button"
                    )
                }

                CallControlCircleButton(
                    icon = Icons.Rounded.CallEnd,
                    label = "End",
                    active = false,
                    containerColor = IosRed,
                    iconTint = Color.White,
                    onClick = {
                        haptics.impactMedium()
                        onHangUp()
                    },
                    testTag = "call_end_button"
                )
            }
        }
    }
}

@Composable
private fun IncomingCallFullOverlay(
    callSession: CallSession,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val haptics = rememberIosHaptics()

    Box(modifier = Modifier.fillMaxSize()) {
        NeutralCallBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(
                        if (callSession.isVideoCall) R.string.incoming_video_call
                        else R.string.incoming_voice_call
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "End-to-end encrypted",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }

            CalmAvatarCallStage(
                name = callSession.callerName.ifBlank { "Member" },
                photoUrl = callSession.callerPhotoUrl,
                statusLine = "Ringing…",
                showVoiceEqualizer = false
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .bouncyPressScale(0.92f)
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(IosRed)
                            .clickable {
                                haptics.impactMedium()
                                onDecline()
                            }
                            .testTag("incoming_call_decline_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = stringResource(R.string.decline_call),
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.decline_call),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .bouncyPressScale(0.92f)
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(IosGreen)
                            .clickable {
                                haptics.success()
                                onAccept()
                            }
                            .testTag("incoming_call_accept_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (callSession.isVideoCall) Icons.Rounded.Videocam else Icons.Rounded.Call,
                            contentDescription = stringResource(R.string.accept_call),
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.accept_call),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimizedCallIslandBar(
    currentUserId: String,
    session: CallSession,
    callState: ActiveCallUiState,
    onExpand: () -> Unit,
    onHangUp: () -> Unit
) {
    val haptics = rememberIosHaptics()
    Surface(
        shape = CircleShape,
        color = DynamicIslandDarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .border(0.8.dp, DynamicIslandDarkBorder, CircleShape)
            .clickable {
                haptics.lightTap()
                onExpand()
            }
            .testTag("minimized_call_pill")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = session.peerNameFor(currentUserId),
                photoUrl = session.peerPhotoUrlFor(currentUserId),
                size = 38.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.peerNameFor(currentUserId),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (callState.phase == CallConnectionPhase.CONNECTED) {
                        "${formatCallDuration(callState.durationSeconds)} · Tap to return"
                    } else {
                        "${callState.phase.label} · Tap to return"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = IosGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = CircleShape,
                color = IosRed,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptics.impactMedium()
                        onHangUp()
                    }
                    .testTag("minimized_call_hangup_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.CallEnd,
                        contentDescription = stringResource(R.string.end_call),
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalmAvatarCallStage(
    name: String,
    photoUrl: String?,
    statusLine: String,
    showVoiceEqualizer: Boolean
) {
    val transition = rememberInfiniteTransition(label = "call_ripple")
    val waveState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseRadius = 60.dp.toPx()
                val strokeWidthPx = 1.dp.toPx()
                val w = waveState.value
                for (i in 0..1) {
                    val progress = (w + i * 0.5f) % 1f
                    val ringRadius = baseRadius * (1f + progress * 0.55f)
                    val ringAlpha = (1f - progress) * 0.22f
                    drawCircle(
                        color = Color.White.copy(alpha = ringAlpha),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = strokeWidthPx)
                    )
                }
            }

            UserAvatar(
                name = name,
                photoUrl = photoUrl,
                size = 116.dp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = statusLine,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.72f)
        )

        if (showVoiceEqualizer) {
            Spacer(modifier = Modifier.height(16.dp))
            LiveVoiceEqualizerBars()
        }
    }
}

@Composable
private fun LiveVoiceEqualizerBars() {
    val transition = rememberInfiniteTransition(label = "voice_eq")
    val phaseState = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_phase"
    )
    val offsets = remember { floatArrayOf(0.0f, 0.9f, 1.8f, 2.7f, 3.6f) }
    val barColor = remember { Color.White.copy(alpha = 0.65f) }

    Canvas(
        modifier = Modifier
            .width(42.dp)
            .height(20.dp)
    ) {
        val phase = phaseState.value
        val barWidth = 3.5.dp.toPx()
        val gap = 4.5.dp.toPx()
        val minH = 6.dp.toPx()
        val extraH = 12.dp.toPx()
        val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
        val totalW = offsets.size * barWidth + (offsets.size - 1) * gap
        val startX = (size.width - totalW) / 2f

        for (index in offsets.indices) {
            val factor = 0.35f + 0.65f * ((sin(phase + offsets[index]) + 1f) * 0.5f)
            val h = minH + extraH * factor
            val x = startX + index * (barWidth + gap)
            val y = (size.height - h) / 2f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = corner
            )
        }
    }
}

@Composable
private fun CallControlCircleButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String,
    containerColor: Color = if (active) Color.White else CallControlSurface,
    iconTint: Color = if (active) Color(0xFF111318) else Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(testTag)
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            modifier = Modifier
                .bouncyPressScale(0.92f)
                .size(54.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.82f),
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun RemoteVideoRendererSurface(
    onAttach: (SurfaceViewRenderer) -> Unit,
    onDetach: (SurfaceViewRenderer) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val renderer = remember { SurfaceViewRenderer(context) }

    DisposableEffect(renderer) {
        onAttach(renderer)
        onDispose {
            onDetach(renderer)
        }
    }

    AndroidView(
        factory = { renderer },
        modifier = modifier
    )
}

@Composable
private fun LocalVideoRendererSurface(
    onAttach: (SurfaceViewRenderer) -> Unit,
    onDetach: (SurfaceViewRenderer) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val renderer = remember { SurfaceViewRenderer(context) }

    DisposableEffect(renderer) {
        onAttach(renderer)
        onDispose {
            onDetach(renderer)
        }
    }

    AndroidView(
        factory = { renderer },
        modifier = modifier
    )
}

fun formatCallDuration(totalSeconds: Long): String {
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
