package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.rounded.Hd
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.data.model.CallQualityPreset
import com.example.data.model.CallSession
import com.example.ui.components.GlassmorphismCard
import com.example.ui.components.UserAvatar
import com.example.ui.components.bouncyPressScale
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.DynamicIslandDarkBorder
import com.example.ui.theme.DynamicIslandDarkSurface
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosRed
import com.example.ui.theme.LocalUniversAccent
import com.example.ui.viewmodel.ActiveCallUiState
import com.example.ui.viewmodel.CallConnectionPhase
import org.webrtc.SurfaceViewRenderer
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
        // 1. Fullscreen HD Video / Voice Call Stage
        AnimatedVisibility(
            visible = activeSession != null && !callState.isMinimized,
            enter = fadeIn(tween(220)) + slideInVertically { it / 8 },
            exit = fadeOut(tween(180)) + slideOutVertically { it / 8 }
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

        // 2. Minimized Floating Dynamic Island Pill
        AnimatedVisibility(
            visible = activeSession != null && callState.isMinimized,
            enter = fadeIn(tween(180)) + slideInVertically { -it },
            exit = fadeOut(tween(150)) + slideOutVertically { -it },
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

        // 3. Cinematic Incoming Call Stage
        AnimatedVisibility(
            visible = incomingCall != null && activeSession == null,
            enter = fadeIn(tween(240)) + slideInVertically { -it / 4 },
            exit = fadeOut(tween(180)) + slideOutVertically { -it / 4 }
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
private fun CallAuroraBackdrop(
    isIncoming: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = LocalUniversAccent.current
    val transition = rememberInfiniteTransition(label = "call_aurora_mesh")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora_drift"
    )

    val topGlow = if (isIncoming) Color(0xFF00D4B4) else accent.primaryDark
    val bottomGlow = if (isIncoming) Color(0xFF007AFF) else Color(0xFF0D9488)
    val accentGlow = if (isIncoming) Color(0xFF10B981) else accent.secondaryAccent

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060A10))
            .drawBehind {
                val c1 = Offset(
                    x = size.width * (0.26f + 0.10f * cos(drift)),
                    y = size.height * (0.22f + 0.06f * sin(drift))
                )
                val c2 = Offset(
                    x = size.width * (0.78f + 0.09f * sin(drift)),
                    y = size.height * (0.74f + 0.07f * cos(drift))
                )
                val c3 = Offset(
                    x = size.width * 0.50f,
                    y = size.height * (0.44f + 0.05f * sin(drift * 1.4f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(topGlow.copy(alpha = 0.34f), Color.Transparent),
                        center = c1,
                        radius = size.minDimension * 0.82f
                    ),
                    center = c1,
                    radius = size.minDimension * 0.82f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(bottomGlow.copy(alpha = 0.30f), Color.Transparent),
                        center = c2,
                        radius = size.minDimension * 0.86f
                    ),
                    center = c2,
                    radius = size.minDimension * 0.86f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentGlow.copy(alpha = 0.16f), Color.Transparent),
                        center = c3,
                        radius = size.minDimension * 0.65f
                    ),
                    center = c3,
                    radius = size.minDimension * 0.65f
                )
            }
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
            .background(Color(0xFF060A10))
    ) {
        if (isVideo) {
            RemoteVideoRendererSurface(
                onAttach = onAttachRemoteRenderer,
                onDetach = onDetachRemoteRenderer,
                modifier = Modifier.fillMaxSize()
            )
            if (!callState.hasRemoteVideo || callState.phase != CallConnectionPhase.CONNECTED) {
                CallAuroraBackdrop(isIncoming = false)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    PulsingAvatarCallStage(
                        name = peerName,
                        photoUrl = peerPhoto,
                        statusLine = callState.phase.label,
                        qualityLabel = callState.qualityPreset.label,
                        showVoiceEqualizer = false
                    )
                }
            }
        } else {
            CallAuroraBackdrop(isIncoming = false)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                PulsingAvatarCallStage(
                    name = peerName,
                    photoUrl = peerPhoto,
                    statusLine = if (callState.phase == CallConnectionPhase.CONNECTED) {
                        formatCallDuration(callState.durationSeconds)
                    } else {
                        callState.phase.label
                    },
                    qualityLabel = CallQualityPreset.HD_VOICE.label,
                    showVoiceEqualizer = callState.phase == CallConnectionPhase.CONNECTED && !callState.isMuted
                )
            }
        }

        // Top Frosted Call HUD
        GlassmorphismCard(
            shape = RoundedCornerShape(26.dp),
            containerColor = Color(0xFF0C121C),
            tintAlpha = 0.76f,
            borderColor = Color.White.copy(alpha = 0.16f),
            blurRadius = 20.dp,
            highlighted = true,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptics.lightTap()
                        onMinimize()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
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
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (callState.phase == CallConnectionPhase.CONNECTED) IosGreen
                                    else AuroraCyan
                                )
                        )
                        Text(
                            text = if (callState.phase == CallConnectionPhase.CONNECTED) {
                                "${callState.phase.label} · ${formatCallDuration(callState.durationSeconds)}"
                            } else {
                                callState.phase.label
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // HD Quality Badge / Switcher
                Surface(
                    shape = CircleShape,
                    color = AuroraCyan.copy(alpha = 0.18f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(0.8.dp, AuroraCyan.copy(alpha = 0.55f), CircleShape)
                        .then(
                            if (isVideo) {
                                Modifier
                                    .minimumInteractiveComponentSize()
                                    .clickable {
                                        haptics.selectionTick()
                                        onCycleQuality()
                                    }
                            } else Modifier
                        )
                        .testTag("call_quality_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Hd,
                            contentDescription = "HD quality stream",
                            tint = AuroraCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isVideo) callState.qualityPreset.shortBadge else CallQualityPreset.HD_VOICE.shortBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Local Self-View PIP Card (Video Calls)
        if (isVideo && callState.isCameraEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = 136.dp)
                    .size(width = 114.dp, height = 162.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DynamicIslandDarkSurface)
                    .border(
                        width = 1.4.dp,
                        color = Color.White.copy(alpha = 0.36f),
                        shape = RoundedCornerShape(24.dp)
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
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Cameraswitch,
                                contentDescription = "Flip camera",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Frosted Control Capsule
        GlassmorphismCard(
            shape = RoundedCornerShape(34.dp),
            containerColor = Color(0xFF0C121C),
            tintAlpha = 0.82f,
            borderColor = Color.White.copy(alpha = 0.18f),
            blurRadius = 24.dp,
            highlighted = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 18.dp)
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
                    label = if (callState.isSpeakerOn) "Speaker" else "Earpiece",
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
                        label = if (callState.isCameraEnabled) "Cam On" else "Cam Off",
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
    val acceptPulseTransition = rememberInfiniteTransition(label = "accept_button_halo")
    val acceptHaloScale by acceptPulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accept_halo_scale"
    )
    val acceptHaloAlpha by acceptPulseTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accept_halo_alpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        CallAuroraBackdrop(isIncoming = true)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Encrypted Incoming Badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GlassmorphismCard(
                    shape = CircleShape,
                    containerColor = Color(0xFF0C1521),
                    tintAlpha = 0.75f,
                    borderColor = AuroraCyan.copy(alpha = 0.45f),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4ADE80))
                        )
                        Icon(
                            imageVector = if (callSession.isVideoCall) Icons.Rounded.Videocam else Icons.Rounded.Call,
                            contentDescription = null,
                            tint = AuroraCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(
                                if (callSession.isVideoCall) R.string.incoming_video_call
                                else R.string.incoming_voice_call
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.68f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "End-to-End Encrypted",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Centerpiece Triple-Wave Caller Stage
            PulsingAvatarCallStage(
                name = callSession.callerName.ifBlank { "Member" },
                photoUrl = callSession.callerPhotoUrl,
                statusLine = if (callSession.isVideoCall) "Ringing · HD Video Call" else "Ringing · HD Voice Call",
                qualityLabel = callSession.qualityEnum().label,
                showVoiceEqualizer = false
            )

            // Bottom Floating Glass Accept / Decline Dock
            GlassmorphismCard(
                shape = RoundedCornerShape(36.dp),
                containerColor = Color(0xFF0C1521),
                tintAlpha = 0.78f,
                borderColor = Color.White.copy(alpha = 0.18f),
                blurRadius = 24.dp,
                highlighted = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(84.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(82.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.18f))
                            )
                            Box(
                                modifier = Modifier
                                    .bouncyPressScale(0.90f)
                                    .size(72.dp)
                                    .shadow(
                                        elevation = 14.dp,
                                        shape = CircleShape,
                                        ambientColor = Color(0xFFEF4444),
                                        spotColor = Color(0xFFEF4444)
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFF453A), Color(0xFFE11D48))
                                        )
                                    )
                                    .border(1.2.dp, Color.White.copy(alpha = 0.28f), CircleShape)
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
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.decline_call),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Accept Button with Pulsing Emerald Halo
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(84.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .graphicsLayer {
                                        scaleX = acceptHaloScale
                                        scaleY = acceptHaloScale
                                        alpha = acceptHaloAlpha
                                    }
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Box(
                                modifier = Modifier
                                    .bouncyPressScale(0.90f)
                                    .size(72.dp)
                                    .shadow(
                                        elevation = 16.dp,
                                        shape = CircleShape,
                                        ambientColor = Color(0xFF10B981),
                                        spotColor = Color(0xFF10B981)
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF10B981), Color(0xFF00C6A7))
                                        )
                                    )
                                    .border(1.2.dp, Color.White.copy(alpha = 0.34f), CircleShape)
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
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.accept_call),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.96f),
                            fontWeight = FontWeight.Bold
                        )
                    }
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
    GlassmorphismCard(
        shape = CircleShape,
        containerColor = DynamicIslandDarkSurface,
        tintAlpha = 0.90f,
        borderColor = AuroraCyan.copy(alpha = 0.42f),
        highlighted = true,
        onClick = {
            haptics.lightTap()
            onExpand()
        },
        modifier = Modifier
            .fillMaxWidth()
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
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (callState.phase == CallConnectionPhase.CONNECTED) {
                        "${callState.qualityPreset.shortBadge} · ${formatCallDuration(callState.durationSeconds)} · Tap to return"
                    } else {
                        "${callState.phase.label} · Tap to expand"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = AuroraCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = CircleShape,
                color = IosRed,
                modifier = Modifier
                    .size(48.dp)
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
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingAvatarCallStage(
    name: String,
    photoUrl: String?,
    statusLine: String,
    qualityLabel: String,
    showVoiceEqualizer: Boolean
) {
    val transition = rememberInfiniteTransition(label = "call_triple_ripple")
    val wave1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )
    val wave2 = (wave1 + 0.33f) % 1f
    val wave3 = (wave1 + 0.66f) % 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(226.dp)) {
            listOf(wave1, wave2, wave3).forEach { progress ->
                val ringScale = 1f + progress * 0.72f
                val ringAlpha = (1f - progress) * 0.42f
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .graphicsLayer {
                            scaleX = ringScale
                            scaleY = ringScale
                            alpha = ringAlpha
                        }
                        .clip(CircleShape)
                        .border(1.5.dp, AuroraCyan, CircleShape)
                        .background(AuroraCyan.copy(alpha = 0.08f))
                )
            }

            Box(
                modifier = Modifier
                    .size(134.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(AuroraCyan, IosBlue, Color(0xFF10B981), AuroraCyan)
                        )
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                UserAvatar(
                    name = name,
                    photoUrl = photoUrl,
                    size = 126.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.displaySmall,
            fontSize = 31.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = statusLine,
            style = MaterialTheme.typography.titleMedium,
            color = AuroraCyan.copy(alpha = 0.94f),
            fontWeight = FontWeight.Medium
        )

        if (showVoiceEqualizer) {
            Spacer(modifier = Modifier.height(14.dp))
            LiveVoiceEqualizerBars()
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.11f),
            modifier = Modifier.border(0.8.dp, Color.White.copy(alpha = 0.24f), CircleShape)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Hd,
                    contentDescription = null,
                    tint = AuroraCyan,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = qualityLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun LiveVoiceEqualizerBars() {
    val transition = rememberInfiniteTransition(label = "voice_eq")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_phase"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(26.dp)
    ) {
        val offsets = remember { listOf(0.0f, 0.8f, 1.6f, 2.4f, 3.2f, 4.0f, 4.8f) }
        offsets.forEachIndexed { index, offset ->
            val factor = (0.35f + 0.65f * ((sin(phase + offset) + 1f) * 0.5f))
            val barHeight = (8.dp + (16.dp * factor))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(CircleShape)
                    .background(if (index % 2 == 0) AuroraCyan else Color.White.copy(alpha = 0.85f))
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
    containerColor: Color = if (active) Color.White else Color.White.copy(alpha = 0.14f),
    iconTint: Color = if (active) Color(0xFF0A1019) else Color.White
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
                .size(56.dp)
                .clip(CircleShape)
                .border(0.9.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.90f),
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
