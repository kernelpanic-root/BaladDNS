package com.eyalm.adns.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.eyalm.adns.data.nextdns.model.BuiltInListIcon
import com.eyalm.adns.data.nextdns.model.ListIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private val imageCache = LruCache<String, ImageBitmap>(50)

@Composable
fun ListIconView(
    icon: ListIcon,
    modifier: Modifier = Modifier
) {
    if (icon is ListIcon.None) return

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
            .background(
                color = Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        when (icon) {
            is ListIcon.BuiltIn -> {
                Icon(
                    imageVector = icon.key.imageVector,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize(0.6f)
                )
            }
            is ListIcon.Text -> {
                Text(
                    text = icon.text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is ListIcon.Url -> {
                val imageBitmap = rememberAsyncImage(url = icon.url)
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxSize(0.5f)
                    )
                }
            }
            ListIcon.None -> {}
        }
    }
}

@Composable
fun rememberAsyncImage(url: String): ImageBitmap? {
    var imageBitmap by remember(url) { mutableStateOf<ImageBitmap?>(imageCache.get(url)) }
    
    if (imageBitmap != null) return imageBitmap

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                URL(url).openStream().use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        val bitmap = bmp.asImageBitmap()
                        imageCache.put(url, bitmap)
                        imageBitmap = bitmap
                    }
                }
            } catch (_: Exception) {

            }
        }
    }
    return imageBitmap
}

private val BuiltInListIcon.imageVector
    get() = when (this) {
        BuiltInListIcon.Shield -> Icons.Default.Shield
        BuiltInListIcon.Computer -> Icons.Default.Computer
        BuiltInListIcon.Smartphone -> Icons.Default.Smartphone
        BuiltInListIcon.Speaker -> Icons.Default.Speaker
        BuiltInListIcon.Devices -> Icons.Default.Devices
        BuiltInListIcon.Block -> Icons.Default.Block
        BuiltInListIcon.Favorite -> Icons.Default.Favorite
        BuiltInListIcon.People -> Icons.Default.People
        BuiltInListIcon.PlayCircle -> Icons.Default.PlayCircle
        BuiltInListIcon.SportsEsports -> Icons.Default.SportsEsports
        BuiltInListIcon.Casino -> Icons.Default.Casino
        BuiltInListIcon.ShoppingBag -> Icons.Default.ShoppingBag
        BuiltInListIcon.Chat -> Icons.AutoMirrored.Filled.Chat
        BuiltInListIcon.MusicNote -> Icons.Default.MusicNote
        BuiltInListIcon.Folder -> Icons.Default.Folder
        BuiltInListIcon.SignalCellular -> Icons.Default.SignalCellularAlt
        BuiltInListIcon.Wifi -> Icons.Default.Wifi
    }
