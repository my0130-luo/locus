package dev.locus.feature.settings

import dev.locus.core.model.SettingsDestination
import dev.locus.core.designsystem.SettingsLink
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.locus.core.data.backup.BackupArchive
import dev.locus.core.data.backup.BackupManifest
import dev.locus.core.designsystem.Banner
import dev.locus.core.designsystem.CardGroup
import dev.locus.core.designsystem.GroupDivider
import dev.locus.core.designsystem.IconTile
import dev.locus.core.designsystem.ListRow
import dev.locus.core.designsystem.Locus
import dev.locus.core.designsystem.LocusCard
import dev.locus.core.designsystem.LocusIcons
import dev.locus.core.designsystem.LocusSheet
import dev.locus.core.designsystem.LocusType
import dev.locus.core.designsystem.Meter
import dev.locus.core.designsystem.SectionLabel
import dev.locus.core.designsystem.SectionNote
import dev.locus.core.designsystem.SettingsActionFor
import dev.locus.core.designsystem.Sizes
import dev.locus.core.designsystem.Spacing
import dev.locus.core.designsystem.SupportingText
import dev.locus.core.designsystem.Tone
import dev.locus.core.designsystem.meterFraction
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date
import dev.locus.core.designsystem.Insets
import dev.locus.core.designsystem.RowChevron
import dev.locus.core.designsystem.SwitchRow

/** Settings → Backup & restore: an encrypted file the user saves wherever they like, and restoring from one. */
@Composable
internal fun BackupRoute(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { viewModel.backup(it) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { viewModel.pickedRestoreFile(it) }
    var askPassword by remember { mutableStateOf(false) }

    BackupSettings(
        state = state,
        onBack = onBack,
        onIncludeWorkspace = viewModel::setIncludeWorkspace,
        onIncludeVault = viewModel::setIncludeVault,
        onBackUp = { askPassword = true },
        onCancelBackup = viewModel::cancelBackup,
        onDismissResult = viewModel::dismissResult,
        onRestore = { open.launch(arrayOf("*/*")) },
    )

    if (askPassword) {
        NewPasswordSheet(
            includesVault = state.includeVault,
            onDismiss = { askPassword = false },
            onDone = { password ->
                askPassword = false
                viewModel.setBackupPassword(password)
                save.launch("locus-${LocalDate.now()}.locusbackup")
            },
        )
    }
    RestoreSheet(
        step = state.restore,
        runActive = state.runActive,
        onUnlock = viewModel::unlock,
        onConfirm = viewModel::confirmRestore,
        onDismiss = viewModel::closeRestore,
        onChooseAnother = { viewModel.closeRestore(); open.launch(arrayOf("*/*")) },
    )
}

@Composable
internal fun BackupSettings(
    state: BackupUiState,
    onBack: () -> Unit,
    onIncludeWorkspace: (Boolean) -> Unit,
    onIncludeVault: (Boolean) -> Unit,
    onBackUp: () -> Unit,
    onCancelBackup: () -> Unit,
    onDismissResult: () -> Unit,
    onRestore: () -> Unit,
) {
    val estimate = state.estimate
    val busy = state.backupProgress != null
    SettingsPageScaffold(stringResource(R.string.backup_title), onBack) {
        item { SectionLabel(stringResource(R.string.backup_back_up), SectionPadding) }
        item {
            CardGroup {
                SwitchRow(
                    title = stringResource(R.string.backup_include_workspace),
                    checked = state.includeWorkspace,
                    onCheckedChange = onIncludeWorkspace,
                    subtitle = estimate?.let { "${formatBytes(it.workspaceBytes)} · the files the agent works on" }
                        ?: if (state.estimating) "The files the agent works on · $WORKING_OUT_SIZE" else "The files the agent works on",
                    icon = LocusIcons.Folder,
                )
                GroupDivider(inset = Insets.iconRow)
                SwitchRow(
                    title = stringResource(R.string.backup_include_secrets),
                    checked = state.includeVault,
                    onCheckedChange = onIncludeVault,
                    subtitle = "API keys, sign-ins, saved logins, SSH and connector secrets. Off: you enter them again after restoring.",
                    icon = LocusIcons.Key,
                )
            }
        }
        if (state.includeVault) {
            item {
                Banner(
                    title = stringResource(R.string.backup_keys_in_file),
                    text = "They're encrypted with your backup password, but anyone who has the file and the password can use your accounts. Keep the password somewhere safe, or leave this off.",
                    tone = Tone.Caution,
                    modifier = NotePadding,
                )
            }
        }
        item {
            CardGroup(NotePadding) {
                ListRow(
                    title = stringResource(R.string.backup_now),
                    subtitle = estimate?.let { "About ${formatBytes(it.baseBytes + if (state.includeWorkspace) it.workspaceBytes else 0)} before compression" }
                        ?: if (state.estimating) "Chats, settings, memory, skills, workflows and helpers · $WORKING_OUT_SIZE" else "Chats, settings, memory, skills, workflows and helpers",
                    subtitleMaxLines = 2,
                    // While a backup runs the row is shown as unavailable, not just silent.
                    onClick = onBackUp,
                    enabled = !busy,
                    leading = { IconTile(LocusIcons.Import) },
                    trailing = { RowChevron() },
                )
            }
        }
        state.backupProgress?.let { progress ->
            item { ProgressCard(progress, onCancel = onCancelBackup) }
        }
        state.result?.let { (message, failed) ->
            item {
                Banner(
                    text = message,
                    tone = if (failed) Tone.Critical else Tone.Live,
                    modifier = NotePadding,
                    actionsBelow = failed,
                    actions = {
                        if (failed) {
                            // Below the text, so the row may take the full width.
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.End)) {
                                TextButton(onClick = onDismissResult) { Text(stringResource(R.string.backup_dismiss)) }
                                // "Free some space … Settings → … → Storage": the way to it.
                                SettingsActionFor(message)
                                TextButton(onClick = { onDismissResult(); onBackUp() }) { Text(stringResource(R.string.backup_try_again)) }
                            }
                        } else {
                            // Beside the text: the button alone, at its own width. A full-width row here left the
                            // message one letter wide.
                            TextButton(onClick = onDismissResult) { Text(stringResource(R.string.backup_ok)) }
                        }
                    },
                )
            }
        }
        item {
            SectionNote(
                "The file is encrypted with your password (AES-256). Locus has no server and can't open a backup without the password, or recover a forgotten one. " +
                    "Browser sign-ins (cookies) aren't backed up; with keys and passwords included, your saved logins are, so the agent can sign you back in.",
                NotePadding,
            )
        }

        item { SectionLabel(stringResource(R.string.backup_restore), SectionPadding) }
        item {
            CardGroup {
                ListRow(
                    title = stringResource(R.string.backup_restore_from),
                    subtitle = "Replaces chats, settings, memory and skills on this phone",
                    subtitleMaxLines = 2,
                    onClick = onRestore,
                    enabled = !busy,
                    leading = { IconTile(LocusIcons.Restore) },
                    trailing = { RowChevron() },
                )
            }
        }
        item { SectionNote("You'll see what's in the backup before anything changes. Locus restarts to finish.", NotePadding) }
    }
}

@Composable
private fun ProgressCard(progress: BackupProgress, onCancel: (() -> Unit)? = null, showLabel: Boolean = true) {
    LocusCard(NotePadding) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            val amount = if (progress.total > 0) "${formatBytes(progress.done)} of ${formatBytes(progress.total)}" else null
            Text(
                when {
                    !showLabel -> amount ?: stringResource(R.string.backup_working)
                    amount != null -> "${progress.label} · $amount"
                    else -> "${progress.label}…"
                },
                style = LocusType.rowSupporting,
            )
            Meter(
                if (progress.total > 0) meterFraction(progress.done, progress.total) else null,
                tone = Tone.Live,
                contentDescription = progress.label,
            )
            if (onCancel != null) TextButton(onClick = onCancel) { Text(stringResource(R.string.backup_cancel)) }
        }
    }
}

/** Choose and confirm the backup password. */
@Composable
private fun NewPasswordSheet(includesVault: Boolean, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val problem = backupPasswordProblem(password, again)
    LocusSheet(
        title = stringResource(R.string.backup_password_title),
        subtitle = if (includesVault) stringResource(R.string.backup_password_protects) else stringResource(R.string.backup_password_protects_file),
        onDismiss = onDismiss,
        action = stringResource(R.string.backup_choose_save_location),
        actionEnabled = problem == null,
        onAction = { onDone(password) },
    ) {
        OutlinedTextField(
            password, { password = it },
            label = { Text(stringResource(R.string.backup_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("At least $MIN_PASSWORD characters. A few random words are easy to type and hard to guess.") },
        )
        OutlinedTextField(
            again, { again = it },
            label = { Text(stringResource(R.string.backup_password_again)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            isError = again.isNotEmpty() && again != password,
            supportingText = if (again.isNotEmpty() && again != password) ({ Text(stringResource(R.string.backup_password_mismatch)) }) else null,
        )
        Banner(text = stringResource(R.string.backup_password_warning), tone = Tone.Caution)
    }
}

internal const val MIN_PASSWORD = 8

/** Why the password can't be used yet, or null when it can. */
internal fun backupPasswordProblem(password: String, again: String): String? = when {
    password.length < MIN_PASSWORD -> stringResource(R.string.backup_too_short)
    password.isBlank() -> stringResource(R.string.backup_blank)
    again != password -> stringResource(R.string.backup_doesnt_match)
    else -> null
}

/** The restore flow in one sheet: password, checking, preview and confirm. */
@Composable
private fun RestoreSheet(
    step: RestoreStep,
    runActive: Boolean,
    onUnlock: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    /** Closes this sheet and opens the file picker again, for a file that isn't a backup. */
    onChooseAnother: () -> Unit = {},
) {
    when (step) {
        RestoreStep.Idle -> Unit
        is RestoreStep.Password -> {
            var password by remember(step.uri) { mutableStateOf("") }
            val notABackup = step.unreadable
            // A file that isn't a backup has no password to ask for: say so, and offer another file.
            LocusSheet(
                title = if (notABackup) stringResource(R.string.backup_not_backup) else stringResource(R.string.backup_open_backup),
                subtitle = if (notABackup) null else stringResource(R.string.backup_enter_password),
                onDismiss = onDismiss,
                action = if (notABackup) stringResource(R.string.backup_choose_another_file) else stringResource(R.string.backup_check),
                actionEnabled = notABackup || password.isNotEmpty(),
                onAction = { if (notABackup) onChooseAnother() else onUnlock(password) },
            ) {
                if (step.noSpace != null) NoSpaceBanner(step.noSpace)
                else if (step.error != null) Banner(text = step.error, tone = Tone.Critical)
                if (!notABackup) {
                    OutlinedTextField(
                        password, { password = it },
                        label = { Text(stringResource(R.string.backup_password_title)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SupportingText(
                        "Nothing on this phone changes until the whole file has been checked and you confirm.",
                    )
                }
            }
        }
        is RestoreStep.Verifying -> LocusSheet(title = stringResource(R.string.backup_checking), subtitle = stringResource(R.string.backup_decrypting), onDismiss = onDismiss) {
            ProgressCard(step.progress, showLabel = false)
        }
        // What gets replaced comes first and the destructive action last, after the user has read it.
        is RestoreStep.Preview -> LocusSheet(
            title = stringResource(R.string.backup_confirm_restore),
            onDismiss = onDismiss,
        ) {
            if (runActive) Banner(title = stringResource(R.string.backup_task_running), text = stringResource(R.string.backup_stop_task_first), tone = Tone.Caution)
            if (step.noSpace != null) NoSpaceBanner(step.noSpace)
            else if (step.error != null) Banner(text = step.error, tone = Tone.Critical)
            Banner(
                text = restoreWarning(step.manifest),
                tone = Tone.Critical,
            )
            ManifestPreview(step.manifest)
            Button(
                onClick = onConfirm,
                // Off while a task runs or the phone hasn't room: the banners above say why.
                enabled = !runActive && step.noSpace == null,
                colors = ButtonDefaults.buttonColors(containerColor = Locus.colors.critical, contentColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.touchTarget),
            ) { Text(stringResource(R.string.backup_restore_restart)) }
        }
        RestoreStep.Restoring -> LocusSheet(title = stringResource(R.string.backup_restoring), subtitle = stringResource(R.string.backup_restarting), onDismiss = {}) {
            Meter(null, tone = Tone.Live, contentDescription = stringResource(R.string.backup_restoring))
        }
    }
}

/** Not enough room to restore: how much it needs, and the Storage page that shows what Locus uses. */
@Composable
private fun NoSpaceBanner(need: SpaceNeeded) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Banner(text = noSpaceText(need), tone = Tone.Critical, title = stringResource(R.string.backup_not_enough_space))
        SettingsLink(SettingsDestination.Storage, hint = "See what Locus uses and clear it in")
    }
}

@Composable
private fun ManifestPreview(manifest: BackupManifest) {
    CardGroup {
        ListRow(
            title = "Made ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(manifest.createdAt))}",
            subtitle = "Locus ${manifest.appVersion}" +
                (manifest.projects?.takeIf { it > 0 }?.let { n -> " · ${if (n == 1) "1 project" else "$n projects"}" } ?: ""),
            titleMaxLines = 2,
            subtitleMaxLines = 2,
            leading = { IconTile(LocusIcons.Clock) },
        )
        manifest.sections.forEach { section ->
            GroupDivider(inset = Insets.iconRow)
            ListRow(
                title = sectionTitle(section.name),
                subtitle = sectionSummary(section.name, section.files, section.bytes),
                titleMaxLines = 2,
                leading = { IconTile(sectionIcon(section.name)) },
            )
        }
    }
}

internal fun sectionTitle(name: String) = when (name) {
    BackupArchive.DATABASE -> stringResource(R.string.backup_chats_activity)
    BackupArchive.SETTINGS -> stringResource(R.string.backup_settings)
    BackupArchive.AGENT_HOME -> "Memory, skills, workflows, helpers and persona"
    BackupArchive.WORKSPACE -> "Workspace files"
    BackupArchive.VAULT -> "Keys and passwords"
    else -> name
}

internal fun sectionSummary(name: String, files: Int, bytes: Long) = when (name) {
    BackupArchive.VAULT -> if (files == 1) "1 entry" else "$files entries"
    BackupArchive.DATABASE, BackupArchive.SETTINGS -> formatBytes(bytes)
    else -> "${if (files == 1) "1 file" else "$files files"} · ${formatBytes(bytes)}"
}

private fun sectionIcon(name: String) = when (name) {
    BackupArchive.DATABASE -> LocusIcons.Chats
    BackupArchive.SETTINGS -> LocusIcons.Settings
    BackupArchive.AGENT_HOME -> LocusIcons.Library
    BackupArchive.WORKSPACE -> LocusIcons.Folder
    BackupArchive.VAULT -> LocusIcons.Key
    else -> LocusIcons.File
}

/** Says exactly what is replaced and what is kept. */
internal fun restoreWarning(manifest: BackupManifest): String = buildString {
    append("This replaces the chats, activity, settings, memory, skills and helpers on this phone with the backup's.")
    append(if (manifest.includesWorkspace) " The workspace is replaced too." else " Your workspace files stay as they are.")
    append(if (manifest.includesVault) " Saved keys are replaced with the backup's." else " Keys saved on this phone are kept; add any that are missing.")
    append(" Locus then restarts.")
}

/** Beside a backup row's text while its size is being counted. */
internal const val WORKING_OUT_SIZE = "Working out the size…"
