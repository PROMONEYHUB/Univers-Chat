package com.example.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalIsTelegramDark

/**
 * 120fps-tuned native spring motion physics and exponential deceleration curves.
 */
object UniversMotion {
    val FluidOutEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val SoftInOutEasing = CubicBezierEasing(0.32f, 0.08f, 0.24f, 1f)

    val SnappyFloatSpring = spring<Float>(
        dampingRatio = 0.78f,
        stiffness = 580f
    )

    val GentleFloatSpring = spring<Float>(
        dampingRatio = 0.86f,
        stiffness = 380f
    )

    val ListPlacementSpring = spring<IntOffset>(
        dampingRatio = 0.84f,
        stiffness = 440f
    )

    const val PUSH_ENTER_MS = 260
    const val PUSH_EXIT_MS = 240
    const val TAB_SWITCH_MS = 160
}

/**
 * Zero-recomposition tactile press scale modifier.
 */
fun Modifier.bouncyPressScale(
    pressedScale: Float = 0.96f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scaleState = animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = UniversMotion.SnappyFloatSpring,
        label = "bouncy_press_scale"
    )
    this
        .graphicsLayer {
            val s = scaleState.value
            scaleX = s
            scaleY = s
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
 * Clean flat surface brush for headers (zero neon gradients).
 */
@Composable
fun rememberCosmicAuraHeaderBrush(isDark: Boolean): Brush {
    val bg = MaterialTheme.colorScheme.surface
    return remember(isDark, bg) { SolidColor(bg) }
}

/**
 * Clean native circular loader replacing decorative orbit graphics.
 */
@Composable
fun UniversOrbitLoader(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    @Suppress("UNUSED_PARAMETER") secondaryColor: Color = MaterialTheme.colorScheme.primary
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = primaryColor,
        strokeWidth = 2.2.dp
    )
}

@Composable
fun UniversAnimatedSplashOverlay(
    visible: Boolean,
    onFinished: () -> Unit,
    @Suppress("UNUSED_PARAMETER") modifier: Modifier = Modifier
) {
    LaunchedEffect(visible) {
        if (visible) onFinished()
    }
}

@Composable
private fun rememberUniversShimmerProgress(): State<Float> {
    val transition = rememberInfiniteTransition(label = "univers_shimmer")
    return transition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = UniversMotion.SoftInOutEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "univers_shimmer_progress"
    )
}

private fun Modifier.universShimmerBackground(
    progressState: State<Float>,
    isDark: Boolean
): Modifier {
    val baseColor = if (isDark) Color(0xFF17191F) else Color(0xFFE7E9EE)
    val highlightColor = if (isDark) Color(0xFF23262F) else Color(0xFFF3F4F7)
    val colors = listOf(baseColor, highlightColor, baseColor)
    return this.drawBehind {
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)
        val startX = progressState.value * (w + 320f) - 160f
        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(startX, 0f),
                end = Offset(startX + 340f, h * 0.7f)
            )
        )
    }
}

@Composable
fun rememberUniversShimmerBrush(): Brush {
    val isDark = LocalIsTelegramDark.current
    val progress by rememberUniversShimmerProgress()
    val baseColor = if (isDark) Color(0xFF17191F) else Color(0xFFE7E9EE)
    val highlightColor = if (isDark) Color(0xFF23262F) else Color(0xFFF3F4F7)
    val translateX = progress * 1000f
    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 360f, 200f)
    )
}

@Composable
fun UniversChatsSkeletonLoading(
    modifier: Modifier = Modifier
) {
    val shimmerProgress = rememberUniversShimmerProgress()
    val isDark = LocalIsTelegramDark.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp)
    ) {
        items(8, contentType = { "skeleton_row" }) { idx ->
            val titleFrac = listOf(0.46f, 0.58f, 0.38f, 0.62f, 0.42f, 0.52f, 0.40f, 0.55f)[idx % 8]
            val subFrac = listOf(0.84f, 0.72f, 0.90f, 0.66f, 0.78f, 0.85f, 0.68f, 0.76f)[idx % 8]
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .universShimmerBackground(shimmerProgress, isDark)
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
                                    .universShimmerBackground(shimmerProgress, isDark)
                            )
                            Box(
                                modifier = Modifier
                                    .width(34.dp)
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .universShimmerBackground(shimmerProgress, isDark)
                            )
                        }
                        Spacer(modifier = Modifier.height(9.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(subFrac)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .universShimmerBackground(shimmerProgress, isDark)
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

@Composable
fun UniversChatRoomSkeletonLoading(
    modifier: Modifier = Modifier
) {
    val shimmerProgress = rememberUniversShimmerProgress()
    val isDark = LocalIsTelegramDark.current
    val bubbleSpecs = remember {
        listOf(
            Pair(false, 0.64f),
            Pair(true, 0.50f),
            Pair(false, 0.74f),
            Pair(false, 0.44f),
            Pair(true, 0.68f),
            Pair(true, 0.40f)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        bubbleSpecs.forEach { (isOwn, widthFrac) ->
            val shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isOwn) 20.dp else 6.dp,
                bottomEnd = if (isOwn) 6.dp else 20.dp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(widthFrac)
                        .height(48.dp)
                        .clip(shape)
                        .universShimmerBackground(shimmerProgress, isDark)
                )
            }
        }
    }
}

@Composable
fun UniversPeopleSkeletonLoading(
    modifier: Modifier = Modifier
) {
    val shimmerProgress = rememberUniversShimmerProgress()
    val isDark = LocalIsTelegramDark.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        items(7, contentType = { "skeleton_person" }) { idx ->
            val nameFrac = listOf(0.50f, 0.62f, 0.44f, 0.56f, 0.40f, 0.60f, 0.48f)[idx % 7]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .universShimmerBackground(shimmerProgress, isDark)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(nameFrac)
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .universShimmerBackground(shimmerProgress, isDark)
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.36f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .universShimmerBackground(shimmerProgress, isDark)
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 74.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
