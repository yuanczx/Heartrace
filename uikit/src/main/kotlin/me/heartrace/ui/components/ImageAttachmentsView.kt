package me.heartrace.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.heartrace.uikit.R

@Composable
fun ImageAttachmentsView(
    imageUris: List<String>,
    onRemoveImage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (imageUris.isEmpty()) return
    val context = LocalContext.current
    var selectedPreviewUri by remember { mutableStateOf<String?>(null) }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        items(imageUris) { uriString ->
            val bitmap by produceState<Bitmap?>(initialValue = null, uriString) {
                value = decodeBitmap(context, uriString, 512)
            }
            Box(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).background(PanelLight)
                    .clickable { selectedPreviewUri = uriString },
            ) {
                bitmap?.let { loadedBitmap ->
                    Image(
                        bitmap = loadedBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Image, contentDescription = null, tint = Muted, modifier = Modifier.size(24.dp))
                }
                if (onRemoveImage != null) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .clickable { onRemoveImage(uriString) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.remove_attachment),
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
    }
    selectedPreviewUri?.let { uri ->
        FullScreenImageDialog(imageUri = uri, onDismiss = { selectedPreviewUri = null })
    }
}
