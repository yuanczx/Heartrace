package me.heartrace.feature.you

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.heartrace.feature.metrics.BadgeKind
import me.heartrace.feature.metrics.BadgeStatus
import me.heartrace.ui.components.Coral
import me.heartrace.ui.components.Cream
import me.heartrace.ui.components.Ink
import me.heartrace.ui.components.Lemon
import me.heartrace.ui.components.Muted
import me.heartrace.ui.components.Panel
import me.heartrace.ui.components.PanelLight
import me.heartrace.uikit.R

@Composable
internal fun AchievementsScreen(
    badges: List<BadgeStatus>,
    celebration: List<BadgeKind>,
    onBack: () -> Unit,
    onDismissCelebration: () -> Unit,
    contentMaxWidth: androidx.compose.ui.unit.Dp,
    horizontalContentPadding: androidx.compose.ui.unit.Dp,
    verticalContentPadding: androidx.compose.ui.unit.Dp,
) {
    var selectedBadge by remember { mutableStateOf<BadgeStatus?>(null) }
    BackHandler(enabled = selectedBadge == null, onBack = onBack)
    val earned = badges.count { it.earned }
    Column(Modifier.fillMaxSize()) {
        Surface(
            color = Ink,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Cream,
                    )
                }
                Text(
                    stringResource(R.string.badges_title),
                    color = Cream,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.badges_earned_count, earned, badges.size),
                    color = Muted,
                    fontSize = 11.sp,
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = contentMaxWidth)
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        horizontal = horizontalContentPadding,
                        vertical = verticalContentPadding,
                    ),
            ) {
                Text(
                    stringResource(R.string.achievements_page_subtitle),
                    color = Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 16.dp),
                )
                Spacer(Modifier.height(23.dp))
                BadgeOverviewCard(earned = earned, total = badges.size)
                Spacer(Modifier.height(22.dp))
                BadgesSection(
                    badges = badges,
                    celebration = celebration,
                    onDismissCelebration = onDismissCelebration,
                    onBadgeClick = { selectedBadge = it },
                    showHeading = false,
                )
            }
        }
    }
    selectedBadge?.let { badge ->
        BadgeDetailSheet(
            badge = badge,
            onDismiss = { selectedBadge = null },
        )
    }
}

@Composable
private fun BadgeOverviewCard(earned: Int, total: Int) {
    val progress = if (total == 0) 0f else earned.toFloat() / total
    Surface(
        color = Panel,
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PanelLight.copy(alpha = .52f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .background(
                    Brush.linearGradient(listOf(Coral.copy(alpha = .2f), Panel.copy(alpha = .98f))),
                    RoundedCornerShape(22.dp),
                )
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.badges_overview_title),
                        color = Cream,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.badges_overview_hint),
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.badges_earned_count, earned, total),
                    color = Coral,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .background(PanelLight.copy(alpha = .82f), RoundedCornerShape(50)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(9.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Coral, Lemon)),
                            RoundedCornerShape(50),
                        ),
                )
            }
        }
    }
}
