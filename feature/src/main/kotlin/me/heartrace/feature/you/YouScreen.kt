package me.heartrace.feature.you

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.heartrace.data.AppLanguage
import me.heartrace.feature.shell.AppSettingsViewModel
import me.heartrace.ui.components.AdaptiveTwoPane
import me.heartrace.ui.components.Coral
import me.heartrace.ui.components.Cream
import me.heartrace.ui.components.Header
import me.heartrace.ui.components.Muted
import me.heartrace.ui.components.Panel
import me.heartrace.ui.components.PanelLight
import me.heartrace.ui.components.SectionLabel
import me.heartrace.ui.components.SegmentedOption
import me.heartrace.ui.components.SettingRow
import me.heartrace.ui.components.SlidingSegmentedControl
import me.heartrace.ui.components.copyImageUriToInternalStorage
import me.heartrace.ui.components.deleteInternalAttachment
import me.heartrace.uikit.R as FeatureR

@Composable
fun YouScreen(
    settingsViewModel: AppSettingsViewModel,
    viewModel: YouViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val settings = settingsViewModel.uiState
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val displayedNickname = state.nickname.ifBlank {
        stringResource(FeatureR.string.profile_nickname_placeholder)
    }
    fun discardDraftAvatar() {
        val current = viewModel.uiState
        if (current.draftAvatarUri != current.avatarUri) {
            deleteInternalAttachment(context, current.draftAvatarUri)
        }
        viewModel.closeProfileDialog()
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
        if (uri != null) {
            viewModel.setCopyingAvatar(true)
            coroutineScope.launch(Dispatchers.IO) {
                val copiedUri = copyImageUriToInternalStorage(context, uri)
                withContext(Dispatchers.Main) {
                    if (copiedUri != null) {
                        val current = viewModel.uiState
                        if (!current.isProfileDialogVisible) {
                            deleteInternalAttachment(context, copiedUri)
                        } else {
                            if (current.draftAvatarUri != current.avatarUri) {
                                deleteInternalAttachment(context, current.draftAvatarUri)
                            }
                            viewModel.setDraftAvatarUri(copiedUri)
                        }
                    }
                    viewModel.setCopyingAvatar(false)
                }
            }
        }
    }
    Column {
        Header(stringResource(FeatureR.string.nav_you))
        Spacer(Modifier.height(27.dp))
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
                PreferencesPane(
                    settings = settings,
                    settingsViewModel = settingsViewModel,
                    onReminderChanged = onReminderChanged,
                )
            },
            second = {
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
            onDismissRequest = ::discardDraftAvatar,
            containerColor = PanelLight,
            title = { Text(stringResource(FeatureR.string.profile_title), color = Cream, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(state.draftAvatarUri, Modifier.size(72.dp), stringResource(FeatureR.string.profile_avatar))
                        Spacer(Modifier.width(14.dp))
                        TextButton(onClick = { avatarPickerLauncher.launch("image/*") }) {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(FeatureR.string.profile_change_avatar), color = Coral)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(
                        value = state.draftNickname,
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
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveProfile, enabled = !state.isCopyingAvatar) {
                    Text(stringResource(FeatureR.string.save), color = Coral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = ::discardDraftAvatar) { Text(stringResource(FeatureR.string.cancel), color = Muted) } },
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
                    SegmentedOption(false, stringResource(FeatureR.string.theme_dark)),
                    SegmentedOption(true, stringResource(FeatureR.string.theme_light)),
                ),
                selectedOption = settings.lightTheme,
                onOptionSelected = settingsViewModel::setLightTheme,
            )
        }
    }
    Spacer(Modifier.height(22.dp))
    SectionLabel(stringResource(FeatureR.string.about))
    Spacer(Modifier.height(10.dp))
    Surface(color = Panel, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            SettingRow(stringResource(FeatureR.string.version), "Heartrace")
            SettingRow(stringResource(FeatureR.string.reset_today), stringResource(FeatureR.string.reset_hint), onClick = onReset)
        }
    }
}
