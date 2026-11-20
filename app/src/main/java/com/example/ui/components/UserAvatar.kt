package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.AvatarToneClay
import com.example.ui.theme.AvatarToneIndigo
import com.example.ui.theme.AvatarToneSage
import com.example.ui.theme.AvatarToneSlate
import com.example.ui.theme.AvatarToneSteelBlue
import com.example.ui.theme.AvatarToneTeal
import com.example.ui.theme.OnlinePresenceGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

private val FlatAvatarTones = listOf(
    AvatarToneSteelBlue,
    AvatarToneSlate,
    AvatarToneSage,
    AvatarToneIndigo,
    AvatarToneClay,
    AvatarToneTeal
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
    @Suppress("UNUSED_PARAMETER") showStoryRing: Boolean = false
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val fallbackTone = remember(name, isSavedMessages, primaryColor) {
        if (isSavedMessages) {
            primaryColor
        } else {
            val paletteIndex = (name.hashCode().absoluteValue) % FlatAvatarTones.size
            FlatAvatarTones[paletteIndex]
        }
    }
    val initials = remember(name) {
        name.trim()
            .split(' ', '\t', '\n')
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "U" }
    }

    val isDataUriPhoto = remember(photoUrl) { ImageUtils.isDataUri(photoUrl) }
    val cachedBitmap = remember(photoUrl, isDataUriPhoto) {
        if (isDataUriPhoto) ImageUtils.getCachedDataUriImageBitmap(photoUrl) else null
    }
    val decodedDataUriBitmap: ImageBitmap? by produceState(
        initialValue = cachedBitmap,
        key1 = photoUrl,
        key2 = isDataUriPhoto
    ) {
        if (!isDataUriPhoto || photoUrl == null) {
            value = null
            return@produceState
        }
        if (value == null) {
            value = withContext(Dispatchers.Default) {
                ImageUtils.decodeDataUriToImageBitmap(photoUrl)
            }
        }
    }

    val remoteRequest = remember(photoUrl, isDataUriPhoto, context) {
        if (!photoUrl.isNullOrBlank() && !isDataUriPhoto) {
            ImageRequest.Builder(context)
                .data(photoUrl)
                .memoryCacheKey(photoUrl)
                .diskCacheKey(photoUrl)
                .crossfade(160)
                .build()
        } else {
            null
        }
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(fallbackTone),
            contentAlignment = Alignment.Center
        ) {
            when {
                isSavedMessages -> {
                    Icon(
                        imageVector = Icons.Rounded.Bookmark,
                        contentDescription = "Saved Messages",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.46f)
                    )
                }
                isGroup -> {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = "$name group avatar",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.48f)
                    )
                }
                decodedDataUriBitmap != null -> {
                    val resolvedBitmap = decodedDataUriBitmap
                    if (resolvedBitmap != null) {
                        Image(
                            bitmap = resolvedBitmap,
                            contentDescription = "$name profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                remoteRequest != null -> {
                    AsyncImage(
                        model = remoteRequest,
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
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        if (showPresence && isOnline && !isSavedMessages) {
            val dotSize = (size * 0.26f).coerceAtLeast(10.dp).coerceAtMost(15.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(OnlinePresenceGreen)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            )
        }
    }
}
