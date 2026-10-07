package me.heartrace.feature.you

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.heartrace.data.AppLanguage
import me.heartrace.data.ThemeMode
import me.heartrace.feature.shell.AppSettingsViewModel
import me.heartrace.ui.components.AdaptiveTwoPane
import me.heartrace.ui.components.Coral
import me.heartrace.ui.components.Cream
import me.heartrace.ui.components.Muted
import me.heartrace.ui.components.Panel
import me.heartrace.ui.components.PanelLight
import me.heartrace.ui.components.SectionLabel
import me.heartrace.ui.components.SegmentedOption
import me.heartrace.ui.components.SettingRow
import me.heartrace.ui.components.SlidingSegmentedControl
import me.heartrace.uikit.R as FeatureR

@Composable
fun YouScreen(
    settingsViewModel: AppSettingsViewModel,
    isActive: Boolean,
    contentMaxWidth: androidx.compose.ui.unit.Dp,
    horizontalContentPadding: androidx.compose.ui.unit.Dp,
    verticalContentPadding: androidx.compose.ui.unit.Dp,
    viewModel: YouViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val settings = settingsViewModel.uiState
    val settingsScrollState = rememberScrollState()
    AnimatedContent(
        targetState = state.isAchievementsVisible,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val slideSpec = tween<IntOffset>(durationMillis = 280)
            val fadeSpec = tween<Float>(durationMillis = 180)
            val direction = if (targetState) {
                AnimatedContentTransitionScope.SlideDirection.Left
            } else {
                AnimatedContentTransitionScope.SlideDirection.Right
            }
            (slideIntoContainer(direction, slideSpec) + fadeIn(fadeSpec))
                .togetherWith(slideOutOfContainer(direction, slideSpec) + fadeOut(fadeSpec))
                .using(null)
        },
        label = "you page transition",
    ) { showAchievements ->
        if (showAchievements) {
            AchievementsScreen(
                badges = state.badges,
                celebration = state.badgeCelebration,
                isActive = isActive && state.isAchievementsVisible,
                onBack = viewModel::closeAchievements,
                onDismissCelebration = viewModel::dismissBadgeCelebration,
                contentMaxWidth = contentMaxWidth,
                horizontalContentPadding = horizontalContentPadding,
                verticalContentPadding = verticalContentPadding,
            )
        } else {
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = contentMaxWidth)
                        .align(Alignment.TopCenter)
                        .verticalScroll(settingsScrollState)
                        .padding(
                            horizontal = horizontalContentPadding,
                            vertical = verticalContentPadding,
                        ),
                ) {
                    YouSettingsContent(
                        settingsViewModel = settingsViewModel,
                        viewModel = viewModel,
                        state = state,
                        settings = settings,
                    )
                }
            }
        }
    }
}

@Composable
private fun YouSettingsContent(
    settingsViewModel: AppSettingsViewModel,
    viewModel: YouViewModel,
    state: YouUiState,
    settings: me.heartrace.feature.shell.AppSettingsUiState,
) {
    val context = LocalContext.current
    val displayedNickname = state.nickname.ifBlank {
        stringResource(FeatureR.string.profile_nickname_placeholder)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) settingsViewModel.setRemindersEnabled(true)
    }
    val onReminderChanged = remember(notificationPermissionLauncher, context) {
        { enabled: Boolean ->
            if (!enabled) {
                settingsViewModel.setRemindersEnabled(false)
            } else if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                settingsViewModel.setRemindersEnabled(true)
            }
        }
    }
    val avatarPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.copyAvatar(it.toString()) }
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = viewModel::openProfileDialog).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(state.avatarUri, Modifier.size(58.dp), stringResource(FeatureR.string.profile_avatar))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(displayedNickname, color = Cream, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(FeatureR.string.you_subtitle), color = Muted, fontSize = 13.sp)
            }
            Icon(Icons.Outlined.Edit, contentDescription = stringResource(FeatureR.string.profile_edit), tint = Muted, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(28.dp))
        AdaptiveTwoPane(
            compactSpacing = 22.dp,
            first = {
                AchievementEntryCard(
                    badges = state.badges,
                    onClick = viewModel::openAchievements,
                )
            },
            second = {
                PreferencesPane(
                    settings = settings,
                    settingsViewModel = settingsViewModel,
                    onReminderChanged = onReminderChanged,
                )
                Spacer(Modifier.height(22.dp))
                AppearancePane(
                    settings = settings,
                    settingsViewModel = settingsViewModel,
                    onReset = viewModel::openResetDialog,
                )
            },
        )
    }
    if (state.isResetDialogVisible) {
        AlertDialog(
            onDismissRequest = viewModel::closeResetDialog,
            containerColor = PanelLight,
            title = { Text(stringResource(FeatureR.string.reset_dialog), color = Cream, fontWeight = FontWeight.SemiBold) },
            text = { Text(stringResource(FeatureR.string.reset_message), color = Muted) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetToday()
                    viewModel.closeResetDialog()
                }) { Text(stringResource(FeatureR.string.reset_action), color = Coral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = viewModel::closeResetDialog) { Text(stringResource(FeatureR.string.cancel), color = Muted) } },
        )
    }
    if (state.isProfileDialogVisible) {
        AlertDialog(
            onDismissRequest = viewModel::closeProfileDialog,
            containerColor = PanelLight,
            title = { Text(stringResource(FeatureR.string.profile_title), color = Cream, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(state.draftAvatarUri, Modifier.size(72.dp), stringResource(FeatureR.string.profile_avatar))
                        Spacer(Modifier.width(14.dp))
                        TextButton(onClick = { avatarPickerLauncher.launch("image/*") }, enabled = !state.isCopyingAvatar && !state.isSavingProfile) {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(FeatureR.string.profile_change_avatar), color = Coral)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(
                        value = state.draftNickname,
                        enabled = !state.isSavingProfile,
                        onValueChange = viewModel::setDraftNickname,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(FeatureR.string.profile_nickname)) },
                        placeholder = { Text(stringResource(FeatureR.string.profile_nickname_placeholder), color = Muted) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Cream,
                            unfocusedTextColor = Cream,
                            focusedContainerColor = Panel,
                            unfocusedContainerColor = Panel,
                            focusedIndicatorColor = Coral,
                            unfocusedIndicatorColor = Muted,
                            focusedLabelColor = Coral,
                            unfocusedLabelColor = Muted,
                            cursorColor = Coral,
                        ),
                    )
                    state.profileErrorMessage?.let { error ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(if (error == "avatar_copy_failed") FeatureR.string.avatar_copy_failed else FeatureR.string.save_failed),
                            color = Coral,
                            fontSize = 12.sp,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveProfile, enabled = !state.isCopyingAvatar && !state.isSavingProfile) {
                    Text(stringResource(FeatureR.string.save), color = Coral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::closeProfileDialog, enabled = !state.isSavingProfile) { Text(stringResource(FeatureR.string.cancel), color = Muted) } },
        )
    }
}

@Composable
private fun PreferencesPane(
    settings: me.heartrace.feature.shell.AppSettingsUiState,
    settingsViewModel: AppSettingsViewModel,
    onReminderChanged: (Boolean) -> Unit,
) {
    SectionLabel(stringResource(FeatureR.string.preferences))
    Spacer(Modifier.height(10.dp))
    Surface(color = Panel, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text(stringResource(FeatureR.string.language), color = Cream, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(stringResource(FeatureR.string.language_hint), color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(13.dp))
            SlidingSegmentedControl(
                options = listOf(
                    SegmentedOption(AppLanguage.SYSTEM, stringResource(FeatureR.string.language_system)),
                    SegmentedOption(AppLanguage.ENGLISH, stringResource(FeatureR.string.language_english)),
                    SegmentedOption(AppLanguage.CHINESE, stringResource(FeatureR.string.language_chinese)),
                ),
                selectedOption = settings.language,
                onOptionSelected = settingsViewModel::setLanguage,
            )
            Spacer(Modifier.height(18.dp))
            Text(stringResource(FeatureR.string.weekly_goal_settings), color = Cream, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(stringResource(FeatureR.string.weekly_goal_settings_hint), color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(13.dp))
            SlidingSegmentedControl(
                options = listOf(
                    SegmentedOption(3, stringResource(FeatureR.string.weekly_goal_days, 3)),
                    SegmentedOption(5, stringResource(FeatureR.string.weekly_goal_days, 5)),
                    SegmentedOption(7, stringResource(FeatureR.string.weekly_goal_days, 7)),
                ),
                selectedOption = settings.weeklyGoalDays,
                onOptionSelected = settingsViewModel::setWeeklyGoalDays,
            )
        }
    }
    Spacer(Modifier.height(22.dp))
    SectionLabel(stringResource(FeatureR.string.reminders))
    Spacer(Modifier.height(10.dp))
    Surface(color = Panel, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(FeatureR.string.reminder_title), color = Cream, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(stringResource(FeatureR.string.reminder_hint), color = Muted, fontSize = 12.sp)
            }
            Switch(checked = settings.remindersEnabled, onCheckedChange = onReminderChanged)
        }
    }
}

@Composable
private fun AppearancePane(
    settings: me.heartrace.feature.shell.AppSettingsUiState,
    settingsViewModel: AppSettingsViewModel,
    onReset: () -> Unit,
) {
    val context = LocalContext.current
    val versionName = remember(context) {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName.orEmpty()
    }
    SectionLabel(stringResource(FeatureR.string.appearance))
    Spacer(Modifier.height(10.dp))
    Surface(color = Panel, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text(stringResource(FeatureR.string.appearance), color = Cream, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(stringResource(FeatureR.string.appearance_hint), color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(13.dp))
            SlidingSegmentedControl(
                options = listOf(
                    SegmentedOption(ThemeMode.SYSTEM, stringResource(FeatureR.string.theme_system)),
                    SegmentedOption(ThemeMode.DARK, stringResource(FeatureR.string.theme_dark)),
                    SegmentedOption(ThemeMode.LIGHT, stringResource(FeatureR.string.theme_light)),
                ),
                selectedOption = settings.themeMode,
                onOptionSelected = settingsViewModel::setThemeMode,
            )
        }
    }
    Spacer(Modifier.height(22.dp))
    SectionLabel(stringResource(FeatureR.string.about))
    Spacer(Modifier.height(10.dp))
    Surface(color = Panel, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            SettingRow(stringResource(FeatureR.string.version), versionName)
            SettingRow(stringResource(FeatureR.string.reset_today), stringResource(FeatureR.string.reset_hint), onClick = onReset)
        }
    }
}
