package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.OnlinePresenceGreen
import com.example.ui.theme.PulseBrushes
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramBlueBright
import com.example.ui.theme.TelegramGreen
import com.example.ui.theme.TelegramIndigo
import com.example.ui.theme.TelegramOrange
import com.example.ui.theme.TelegramPink
import com.example.ui.theme.TelegramRed
import com.example.ui.theme.TelegramTeal
import kotlin.math.absoluteValue

private val TelegramAvatarGradients = listOf(
    listOf(TelegramBlueBright, TelegramBlue),
    listOf(TelegramOrange, TelegramPink),
    listOf(TelegramGreen, TelegramTeal),
    listOf(TelegramIndigo, TelegramBlue),
    listOf(TelegramPink, TelegramRed),
    listOf(TelegramTeal, TelegramBlueBright)
)

@Composable
fun UserAvatar(
    name: String,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isGroup: Boolean = false,
    isSavedMessages: Boolean = false,
    showPresence: Boolean = false,
    isOnline: Boolean = false,
    showStoryRing: Boolean = false
) {
    val gradientBrush = remember(name, isSavedMessages) {
        val gradient = if (isSavedMessages) {
            listOf(TelegramBlueBright, TelegramBlue)
        } else {
            val paletteIndex = (name.hashCode().absoluteValue) % TelegramAvatarGradients.size
            TelegramAvatarGradients[paletteIndex]
        }
        Brush.verticalGradient(gradient)
    }
    val initials = remember(name) {
        name.trim()
            .split(' ', '\t', '\n')
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "U" }
    }

    val decodedDataUriBitmap = remember(photoUrl) {
        if (photoUrl != null && ImageUtils.isDataUri(photoUrl)) {
            ImageUtils.decodeDataUriToImageBitmap(photoUrl)
        } else {
            null
        }
    }

    val innerPadding = if (showStoryRing) 4.dp else 0.dp

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (showStoryRing) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 2.dp.toPx()
                drawCircle(
                    brush = PulseBrushes.StoryRingActive,
                    radius = (this.size.minDimension - strokeWidth) / 2f,
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(
                    if (showStoryRing) {
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    } else {
                        Modifier
                    }
                )
                .clip(CircleShape)
                .background(gradientBrush),
            contentAlignment = Alignment.Center
        ) {
            when {
                isSavedMessages -> {
                    Icon(
                        imageVector = Icons.Rounded.Bookmark,
                        contentDescription = "Saved Messages",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.48f)
                    )
                }
                isGroup -> {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = "$name group avatar",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.50f)
                    )
                }
                decodedDataUriBitmap != null -> {
                    Image(
                        bitmap = decodedDataUriBitmap,
                        contentDescription = "$name profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                !photoUrl.isNullOrBlank() && !ImageUtils.isDataUri(photoUrl) -> {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "$name profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = (size.value * 0.34f).coerceIn(11f, 28f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        if (showPresence && !isSavedMessages) {
            val dotSize = (size * 0.27f).coerceAtLeast(10.dp).coerceAtMost(16.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (isOnline) OnlinePresenceGreen else MaterialTheme.colorScheme.outline)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}
