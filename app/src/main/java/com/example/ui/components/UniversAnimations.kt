package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.LocalUniversAccent
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramBlueBright
import com.example.ui.theme.TelegramBlueDeep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tactile spring press scale micro-interaction modifier for cards, FABs, and avatars.
 */
fun Modifier.bouncyPressScale(
    pressedScale: Float = 0.94f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncy_press_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            while (true) {
                awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}

/**
 * Dynamic accent-matched header gradient brush (deep refined obsidian in dark mode,
 * vibrant curated ocean/accent gradient in light mode).
 */
@Composable
fun rememberCosmicAuraHeaderBrush(isDark: Boolean): Brush {
    val accent = LocalUniversAccent.current
    return remember(isDark, accent) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0E1520),
                    accent.containerDark
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    accent.gradientStartLight,
                    accent.gradientEndLight
                )
            )
        }
    }
}

/**
 * Signature Univers Chat Saturn-orbit dual-ring + comet animated loader.
 */
@Composable
fun UniversOrbitLoader(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    primaryColor: Color = TelegramBlueBright,
    secondaryColor: Color = TelegramBlue
) {
    val transition = rememberInfiniteTransition(label = "univers_orbit_loader")
    val outerAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing)
        ),
        label = "outer_orbit_angle"
    )
    val innerAngle by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing)
        ),
        label = "inner_orbit_angle"
    )
    val corePulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val strokeWidth = (this.size.minDimension * 0.11f).coerceAtLeast(1.8.dp.toPx())
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // Subtle track ring
        drawCircle(
            color = primaryColor.copy(alpha = 0.18f),
            radius = radius - strokeWidth,
            center = center,
            style = Stroke(width = strokeWidth * 0.85f)
        )

        // Outer spinning arc
        rotate(degrees = outerAngle, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        secondaryColor.copy(alpha = 0.45f),
                        primaryColor
                    )
                ),
                startAngle = 0f,
                sweepAngle = 250f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Orbiting comet head
            val cometRad = 250.0 * PI / 180.0
            val cometCenter = Offset(
                x = center.x + ((radius - strokeWidth / 2f) * cos(cometRad)).toFloat(),
                y = center.y + ((radius - strokeWidth / 2f) * sin(cometRad)).toFloat()
            )
            drawCircle(
                color = Color.White,
                radius = strokeWidth * 0.72f,
                center = cometCenter
            )
        }

        // Counter-rotating inner orbital arc
        rotate(degrees = innerAngle, pivot = center) {
            val inset = radius * 0.34f
            drawArc(
                color = primaryColor.copy(alpha = 0.55f),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    this.size.width - inset * 2f,
                    this.size.height - inset * 2f
                ),
                style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round)
            )
        }

        // Breathing core star
        drawCircle(
            color = primaryColor.copy(alpha = 0.9f),
            radius = radius * 0.16f * corePulse,
            center = center
        )
    }
}

/**
 * Clean, fast launch splash overlay showing the Univers Chat app logo.
 */
@Composable
fun UniversAnimatedSplashOverlay(
    visible: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoScale = remember { Animatable(0.90f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        launch {
            contentAlpha.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
        }
        delay(260L)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(80)),
        exit = fadeOut(tween(180, easing = FastOutSlowInEasing)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF09111E)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = contentAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
                    .padding(horizontal = 32.dp)
            ) {
                TelegramLogoMark(size = 92.dp)
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Univers Chat",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.4).sp
                )
            }
        }
    }
}

/**
 * Reusable silky diagonal shimmer brush adapted for Light & Dark modes.
 */
@Composable
fun rememberUniversShimmerBrush(): Brush {
    val isDark = LocalIsTelegramDark.current
    val transition = rememberInfiniteTransition(label = "univers_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "univers_shimmer_x"
    )
    val baseColor = if (isDark) Color(0xFF141C27) else Color(0xFFE6ECF4)
    val highlightColor = if (isDark) Color(0xFF1E2B3B) else Color(0xFFF5F8FC)
    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim, 0f),
        end = Offset(translateAnim + 380f, 220f)
    )
}

/**
 * Premium animated shimmer skeleton loading screen for ChatsScreen.
 */
@Composable
fun UniversChatsSkeletonLoading(
    modifier: Modifier = Modifier
) {
    val shimmer = rememberUniversShimmerBrush()
    val isDark = LocalIsTelegramDark.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp)
    ) {
        // Stories shimmer strip
        item(key = "skeleton_stories") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                userScrollEnabled = false
            ) {
                items(6) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(shimmer)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(shimmer)
                        )
                    }
                }
            }
            HorizontalDivider(
                thickness = 0.6.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        // Conversation skeleton rows
        items(7) { idx ->
            val titleFrac = listOf(0.46f, 0.58f, 0.38f, 0.62f, 0.42f, 0.52f, 0.40f)[idx % 7]
            val subFrac = listOf(0.84f, 0.72f, 0.90f, 0.66f, 0.78f, 0.85f, 0.68f)[idx % 7]
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(shimmer)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(titleFrac)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(shimmer)
                            )
                            Box(
                                modifier = Modifier
                                    .width(34.dp)
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(shimmer)
                            )
                        }
                        Spacer(modifier = Modifier.height(9.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(subFrac)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmer)
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 80.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

/**
 * Premium animated chat-bubble shimmer skeleton for ChatRoomScreen.
 */
@Composable
fun UniversChatRoomSkeletonLoading(
    modifier: Modifier = Modifier
) {
    val shimmer = rememberUniversShimmerBrush()
    val isDark = LocalIsTelegramDark.current
    val bubbleSpecs = listOf(
        Pair(false, 0.68f),
        Pair(true, 0.54f),
        Pair(false, 0.78f),
        Pair(false, 0.46f),
        Pair(true, 0.72f),
        Pair(true, 0.42f)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        bubbleSpecs.forEach { (isOwn, widthFrac) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(widthFrac)
                        .height(52.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isOwn) 18.dp else 5.dp,
                                bottomEnd = if (isOwn) 5.dp else 18.dp
                            )
                        )
                        .background(shimmer)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (isDark) 0.06f else 0.35f),
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isOwn) 18.dp else 5.dp,
                                bottomEnd = if (isOwn) 5.dp else 18.dp
                            )
                        )
                )
            }
        }
    }
}

/**
 * Premium animated contact-list shimmer skeleton for PeopleScreen.
 */
@Composable
fun UniversPeopleSkeletonLoading(
    modifier: Modifier = Modifier
) {
    val shimmer = rememberUniversShimmerBrush()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        items(7) { idx ->
            val nameFrac = listOf(0.50f, 0.62f, 0.44f, 0.56f, 0.40f, 0.60f, 0.48f)[idx % 7]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(shimmer)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(nameFrac)
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(shimmer)
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.36f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmer)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(shimmer)
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
