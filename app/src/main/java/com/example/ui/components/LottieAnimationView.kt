package com.example.ui.components

import androidx.annotation.RawRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.R
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.TelegramBlue

/**
 * Type-safe source descriptor for [LottieAnimationView].
 */
sealed interface LottieAnimationSource {
    data class Raw(@RawRes val resId: Int) : LottieAnimationSource
    data class Asset(val assetName: String) : LottieAnimationSource
    data class Url(val url: String) : LottieAnimationSource
}

/**
 * Reusable, lightweight Jetpack Compose wrapper around Airbnb's Lottie library.
 *
 * Designed for onboarding illustrations, empty states, and smooth loading animations
 * with built-in accessibility semantics, Compose testTag support, completion callbacks,
 * and graceful fallback rendering while the composition parses.
 */
@Composable
fun LottieAnimationView(
    @RawRes rawRes: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    isPlaying: Boolean = true,
    iterations: Int = LottieConstants.IterateForever,
    speed: Float = 1f,
    restartOnPlay: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit,
    clipToCompositionBounds: Boolean = true,
    testTag: String? = null,
    onAnimationFinished: (() -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null
) {
    LottieAnimationView(
        source = LottieAnimationSource.Raw(rawRes),
        modifier = modifier,
        contentDescription = contentDescription,
        isPlaying = isPlaying,
        iterations = iterations,
        speed = speed,
        restartOnPlay = restartOnPlay,
        contentScale = contentScale,
        clipToCompositionBounds = clipToCompositionBounds,
        testTag = testTag,
        onAnimationFinished = onAnimationFinished,
        placeholder = placeholder
    )
}

/**
 * Core [LottieAnimationView] supporting raw resources, bundled assets, or remote URLs.
 */
@Composable
fun LottieAnimationView(
    source: LottieAnimationSource,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    isPlaying: Boolean = true,
    iterations: Int = LottieConstants.IterateForever,
    speed: Float = 1f,
    restartOnPlay: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit,
    clipToCompositionBounds: Boolean = true,
    testTag: String? = null,
    onAnimationFinished: (() -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null
) {
    val spec = when (source) {
        is LottieAnimationSource.Raw -> LottieCompositionSpec.RawRes(source.resId)
        is LottieAnimationSource.Asset -> LottieCompositionSpec.Asset(source.assetName)
        is LottieAnimationSource.Url -> LottieCompositionSpec.Url(source.url)
    }

    val compositionResult = rememberLottieComposition(spec = spec)
    val composition = compositionResult.value

    val animState = animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = iterations,
        speed = speed,
        restartOnPlay = restartOnPlay
    )

    val progress by animState

    LaunchedEffect(animState.isAtEnd, animState.isPlaying, composition) {
        if (composition != null &&
            iterations != LottieConstants.IterateForever &&
            animState.isAtEnd &&
            !animState.isPlaying
        ) {
            onAnimationFinished?.invoke()
        }
    }

    val containerModifier = modifier
        .let { if (testTag != null) it.testTag(testTag) else it }
        .let {
            if (contentDescription != null) {
                it.semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                    this.role = Role.Image
                }
            } else {
                it
            }
        }

    Box(
        modifier = containerModifier,
        contentAlignment = Alignment.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                contentScale = contentScale,
                clipToCompositionBounds = clipToCompositionBounds,
                renderMode = RenderMode.AUTOMATIC,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                placeholder?.invoke()
            }
        }
    }
}

/**
 * Compact pill loading indicator powered by [LottieAnimationView], suited for
 * list syncing headers and background status banners.
 */
@Composable
fun UniversLottieSyncPill(
    label: String,
    modifier: Modifier = Modifier,
    animationSize: Dp = 22.dp,
    testTag: String = "lottie_sync_pill"
) {
    val isDark = LocalIsTelegramDark.current
    Surface(
        shape = CircleShape,
        color = if (isDark) Color(0xFF132238) else Color(0xFFEAF4FF),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = TelegramBlue.copy(alpha = if (isDark) 0.34f else 0.24f)
        ),
        modifier = modifier.testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            LottieAnimationView(
                rawRes = R.raw.lottie_cosmic_orbit_loader,
                contentDescription = null,
                modifier = Modifier.size(animationSize),
                placeholder = { UniversOrbitLoader(size = animationSize) }
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Full centered loading state powered by [LottieAnimationView] for initial fetch
 * or blocking transition screens.
 */
@Composable
fun UniversLottieLoadingState(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    animationSize: Dp = 112.dp,
    testTag: String = "lottie_loading_state"
) {
    Column(
        modifier = modifier
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimationView(
            rawRes = R.raw.lottie_cosmic_orbit_loader,
            contentDescription = title,
            modifier = Modifier.size(animationSize),
            placeholder = { UniversOrbitLoader(size = 48.dp) }
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
