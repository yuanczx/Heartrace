package me.heartrace.feature.you

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.heartrace.feature.patterns.BadgeKind
import me.heartrace.feature.patterns.BadgeStatus
import me.heartrace.ui.components.Coral
import me.heartrace.ui.components.Cream
import me.heartrace.ui.components.Lemon
import me.heartrace.ui.components.Muted
import me.heartrace.ui.components.Panel
import me.heartrace.ui.components.PanelLight
import me.heartrace.ui.components.SectionLabel
import me.heartrace.ui.theme.HeartraceTheme
import me.heartrace.uikit.R
import kotlin.time.Duration.Companion.milliseconds

private data class BadgeGroup(
    val titleRes: Int,
    val kinds: List<BadgeKind>,
)

private val badgeGroups = listOf(
    BadgeGroup(
        titleRes = R.string.badges_category_streak,
        kinds = listOf(
            BadgeKind.THREE_DAY_STREAK,
            BadgeKind.SEVEN_DAY_STREAK,
            BadgeKind.FOURTEEN_DAY_STREAK,
        ),
    ),
    BadgeGroup(
        titleRes = R.string.badges_category_records,
        kinds = listOf(
            BadgeKind.FIRST_RECORD,
            BadgeKind.TEN_RECORDS,
            BadgeKind.THIRTY_RECORDS,
        ),
    ),
    BadgeGroup(
        titleRes = R.string.badges_category_goals,
        kinds = listOf(
            BadgeKind.FIRST_WEEKLY_GOAL,
            BadgeKind.FOUR_WEEKLY_GOALS,
        ),
    ),
    BadgeGroup(
        titleRes = R.string.badges_category_exploration,
        kinds = listOf(
            BadgeKind.MOOD_EXPLORER,
            BadgeKind.MOMENT_KEEPER,
        ),
    ),
)

@Composable
internal fun AchievementEntryCard(
    badges: List<BadgeStatus>,
    onClick: () -> Unit,
) {
    val earned = badges.count { it.earned }
    val shape = RoundedCornerShape(14.dp)
    Surface(
        color = Panel,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BadgeMedallion(
                kind = BadgeKind.FIRST_RECORD,
                earned = earned > 0,
                size = 44.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.badges_title),
                    color = Cream,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.achievements_entry_subtitle, earned, badges.size),
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Muted)
        }
    }
}

@Composable
internal fun BadgesSection(
    badges: List<BadgeStatus>,
    celebration: List<BadgeKind>,
    onDismissCelebration: () -> Unit,
    onBadgeClick: (BadgeStatus) -> Unit,
    showHeading: Boolean = true,
) {
    LaunchedEffect(celebration) {
        if (celebration.isNotEmpty()) {
            delay(3_600.milliseconds)
            onDismissCelebration()
        }
    }
    Column {
        AnimatedVisibility(
            visible = celebration.isNotEmpty(),
            enter = fadeIn(tween(350)) + expandVertically(tween(450)),
            exit = fadeOut(tween(250)) + shrinkVertically(tween(300)),
        ) {
            BadgeCelebrationCard(celebration)
        }
        if (celebration.isNotEmpty()) Spacer(Modifier.height(18.dp))
        if (showHeading) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SectionLabel(stringResource(R.string.badges_title))
                Text(
                    stringResource(R.string.badges_earned_count, badges.count { it.earned }, badges.size),
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        badgeGroups.forEachIndexed { index, group ->
            if (index > 0) Spacer(Modifier.height(16.dp))
            BadgeGroupSection(
                group = group,
                badges = badges,
                onBadgeClick = onBadgeClick,
            )
        }
    }
}

@Composable
private fun BadgeGroupSection(
    group: BadgeGroup,
    badges: List<BadgeStatus>,
    onBadgeClick: (BadgeStatus) -> Unit,
) {
    val groupBadges = group.kinds.map { kind ->
        badges.firstOrNull { it.kind == kind } ?: BadgeStatus(kind = kind, earned = false)
    }
    val earned = groupBadges.count { it.earned }
    Surface(
        color = Panel,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, PanelLight.copy(alpha = .45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .background(
                    Brush.verticalGradient(listOf(PanelLight.copy(alpha = .17f), Panel.copy(alpha = .98f))),
                    androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                )
                .padding(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(group.titleRes),
                    color = Cream,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.badges_earned_count, earned, groupBadges.size),
                    color = Muted,
                    fontSize = 10.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            BadgeGrid(groupBadges, onBadgeClick)
        }
    }
}

@Composable
private fun BadgeGrid(
    badges: List<BadgeStatus>,
    onBadgeClick: (BadgeStatus) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columnCount = if (maxWidth >= 600.dp) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            badges.chunked(columnCount).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { badge ->
                        BadgeTile(
                            badge = badge,
                            modifier = Modifier.weight(1f),
                            onClick = { onBadgeClick(badge) },
                        )
                    }
                    repeat(columnCount - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeTile(
    badge: BadgeStatus,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val title = badgeTitle(badge.kind)
    val description = badgeDescription(badge.kind)
    val status = stringResource(
        if (badge.earned) R.string.badge_unlocked_status else R.string.badge_locked_status,
    )
    val accessibilityLabel = stringResource(R.string.badge_accessibility, title, status)
    val shape = RoundedCornerShape(17.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = accessibilityLabel
                role = Role.Button
            },
        color = Panel,
        shape = shape,
        border = BorderStroke(1.dp, Muted.copy(alpha = .16f)),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BadgeMedallion(badge.kind, badge.earned, 82.dp)
            }
            Text(
                title,
                color = if (badge.earned) Cream else Muted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                description,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Muted.copy(alpha = if (badge.earned) .86f else .58f),
                fontSize = 10.sp,
                lineHeight = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BadgeStatusPill() {
    Surface(
        color = Muted.copy(alpha = .12f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(11.dp),
            )
            Text(
                stringResource(R.string.badge_locked_status),
                color = Muted,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun BadgeMedallion(kind: BadgeKind, earned: Boolean, size: androidx.compose.ui.unit.Dp) {
    val grayMatrix = remember { ColorMatrix().apply { setToSaturation(0f) } }
    Box(
        Modifier
            .size(size)
            .then(
                if (earned) {
                    Modifier
                } else {
                    Modifier.graphicsLayer { alpha = .48f }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(badgeAsset(kind)),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Fit,
            colorFilter = if (earned) null else ColorFilter.colorMatrix(grayMatrix),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun BadgeDetailSheet(
    badge: BadgeStatus,
    onDismiss: () -> Unit,
) {
    val title = badgeTitle(badge.kind)
    val description = badgeDescription(badge.kind)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Panel,
        contentWindowInsets = { WindowInsets(0.dp) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BadgeMedallion(badge.kind, badge.earned, 144.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                color = Cream,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            if (!badge.earned) BadgeStatusPill()
            Spacer(Modifier.height(14.dp))
            Text(
                description,
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            if (!badge.earned) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.badge_locked_hint),
                    color = Muted.copy(alpha = .82f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.badge_detail_close),
                    color = Coral,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun BadgeCelebrationCard(kinds: List<BadgeKind>) {
    val pulse by rememberInfiniteTransition(label = "badge celebration pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "badge celebration scale",
    )
    val names = badgeNames(kinds)
    Surface(
        Modifier.fillMaxWidth().graphicsLayer {
            scaleX = pulse
            scaleY = pulse
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        color = Color.Transparent,
        shadowElevation = 10.dp,
    ) {
        Box(
            Modifier
                .background(
                    Brush.linearGradient(listOf(Coral.copy(alpha = .92f), Lemon.copy(alpha = .84f))),
                    androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                )
                .padding(horizontal = 16.dp, vertical = 15.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BadgeCelebrationMedallions(kinds)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.badge_celebration_title),
                        color = HeartraceTheme.colors.ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        names,
                        color = HeartraceTheme.colors.ink.copy(alpha = .82f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.badge_celebration_message),
                        color = HeartraceTheme.colors.ink.copy(alpha = .72f),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeCelebrationMedallions(kinds: List<BadgeKind>) {
    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
        kinds.take(3).forEach { kind ->
            BadgeMedallion(kind, true, 48.dp)
        }
    }
}

@Composable
private fun badgeNames(kinds: List<BadgeKind>): String = when {
    kinds.size >= 3 -> "${badgeTitle(kinds[0])} · ${badgeTitle(kinds[1])} · ${badgeTitle(kinds[2])}"
    kinds.size == 2 -> "${badgeTitle(kinds[0])} · ${badgeTitle(kinds[1])}"
    kinds.size == 1 -> badgeTitle(kinds[0])
    else -> ""
}

@Composable
private fun badgeTitle(kind: BadgeKind): String = stringResource(
    when (kind) {
        BadgeKind.FIRST_RECORD -> R.string.badge_first_record
        BadgeKind.THREE_DAY_STREAK -> R.string.badge_three_day_streak
        BadgeKind.SEVEN_DAY_STREAK -> R.string.badge_seven_day_streak
        BadgeKind.FOURTEEN_DAY_STREAK -> R.string.badge_fourteen_day_streak
        BadgeKind.TEN_RECORDS -> R.string.badge_ten_records
        BadgeKind.FIRST_WEEKLY_GOAL -> R.string.badge_first_weekly_goal
        BadgeKind.FOUR_WEEKLY_GOALS -> R.string.badge_four_weekly_goals
        BadgeKind.MOOD_EXPLORER -> R.string.badge_mood_explorer
        BadgeKind.MOMENT_KEEPER -> R.string.badge_moment_keeper
        BadgeKind.THIRTY_RECORDS -> R.string.badge_thirty_records
    },
)

@Composable
private fun badgeDescription(kind: BadgeKind): String = stringResource(
    when (kind) {
        BadgeKind.FIRST_RECORD -> R.string.badge_first_record_desc
        BadgeKind.THREE_DAY_STREAK -> R.string.badge_three_day_streak_desc
        BadgeKind.SEVEN_DAY_STREAK -> R.string.badge_seven_day_streak_desc
        BadgeKind.FOURTEEN_DAY_STREAK -> R.string.badge_fourteen_day_streak_desc
        BadgeKind.TEN_RECORDS -> R.string.badge_ten_records_desc
        BadgeKind.FIRST_WEEKLY_GOAL -> R.string.badge_first_weekly_goal_desc
        BadgeKind.FOUR_WEEKLY_GOALS -> R.string.badge_four_weekly_goals_desc
        BadgeKind.MOOD_EXPLORER -> R.string.badge_mood_explorer_desc
        BadgeKind.MOMENT_KEEPER -> R.string.badge_moment_keeper_desc
        BadgeKind.THIRTY_RECORDS -> R.string.badge_thirty_records_desc
    },
)

private fun badgeAsset(kind: BadgeKind): Int = when (kind) {
    BadgeKind.FIRST_RECORD -> R.drawable.badge_first_record
    BadgeKind.THREE_DAY_STREAK -> R.drawable.badge_three_day_streak
    BadgeKind.SEVEN_DAY_STREAK -> R.drawable.badge_seven_day_streak
    BadgeKind.FOURTEEN_DAY_STREAK -> R.drawable.badge_fourteen_day_streak
    BadgeKind.TEN_RECORDS -> R.drawable.badge_ten_records
    BadgeKind.FIRST_WEEKLY_GOAL -> R.drawable.badge_first_weekly_goal
    BadgeKind.FOUR_WEEKLY_GOALS -> R.drawable.badge_four_weekly_goals
    BadgeKind.MOOD_EXPLORER -> R.drawable.badge_mood_explorer
    BadgeKind.MOMENT_KEEPER -> R.drawable.badge_moment_keeper
    BadgeKind.THIRTY_RECORDS -> R.drawable.badge_thirty_records
}
