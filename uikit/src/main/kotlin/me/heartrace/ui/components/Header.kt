package me.heartrace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.heartrace.uikit.R

@Composable
fun Header(section: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Coral),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.app_name),
                color = Cream,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.2.sp,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                section.uppercase(),
                color = Muted,
                fontSize = 10.sp,
                letterSpacing = 1.1.sp,
            )
            Spacer(Modifier.width(14.dp))
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .border(1.dp, Muted.copy(alpha = .45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Cream,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
