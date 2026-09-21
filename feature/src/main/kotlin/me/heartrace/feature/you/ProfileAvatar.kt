package me.heartrace.feature.you

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.heartrace.ui.components.Coral
import me.heartrace.ui.components.Ink

@Composable
internal fun ProfileAvatar(avatarUri: String?, modifier: Modifier, contentDescription: String) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = avatarUri) {
        value = avatarUri?.let { decodeProfileBitmap(it) }
    }
    Box(modifier.clip(CircleShape).background(Coral), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Outlined.Person, contentDescription, tint = Ink, modifier = Modifier.size(32.dp))
        }
    }
}

private suspend fun decodeProfileBitmap(avatarUri: String): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val path = Uri.parse(avatarUri).path ?: return@runCatching null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > 256 || bounds.outHeight / sampleSize > 256) sampleSize *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
    }.getOrNull()
}
