package dev.locus.feature.onboarding

import dev.locus.feature.onboarding.R
import dev.locus.core.designsystem.CodeSurface
import dev.locus.core.designsystem.Emphasis
import dev.locus.core.designsystem.LocusType
import dev.locus.core.designsystem.Meter
import dev.locus.core.designsystem.Sizes
import dev.locus.core.designsystem.SupportingText
import dev.locus.core.model.SettingsDestination
import dev.locus.core.designsystem.RowChevron
import dev.locus.core.designsystem.IconSize
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.locus.core.designsystem.CardGroup
import dev.locus.core.designsystem.GroupDivider
import dev.locus.core.designsystem.Insets
import dev.locus.core.designsystem.IconTile
import dev.locus.core.designsystem.ListRow
import dev.locus.core.designsystem.Locus
import dev.locus.core.designsystem.LocusCard
import dev.locus.core.designsystem.LocusIcons
import dev.locus.core.designsystem.LocusTopBar
import dev.locus.core.designsystem.SectionLabel
import dev.locus.core.designsystem.Spacing
import dev.locus.core.designsystem.StatusLine
import dev.locus.core.designsystem.SwitchRow
import dev.locus.core.designsystem.Tone
import dev.locus.core.designsystem.entrance

/**
 * First-run setup, also re-runnable from Settings. [onFinish] gets the id of a first chat when one
 * was started; [onExit] leaves from the first step (closing the app on a first run).
 */
@Composable
fun OnboardingRoute(
    onFinish: (chatId: String?) -> Unit,
    onExit: () -> Unit,
    /** Opens the Workspace, where Linux can be installed later; back returns to this step. */
    onOpenWorkspace: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val step = viewModel.step
    BackHandler { if (!viewModel.back()) onExit() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                LocusTopBar(
                    step.title,
                    onBack = { if (!viewModel.back()) onExit() },
                    status = { SupportingText("Step ${step.ordinal + 1} of ${Step.entries.size}", style = LocusType.mono) },
                    actions = { if (step.skippable) TextButton(onClick = viewModel::next) { Text(stringResource(R.string.onboarding_skip)) } },
                )
                StepProgress(step)
            }
        },
        bottomBar = { BottomAction(step, state, viewModel, onFinish) },
    ) { padding ->
        AnimatedContent(step, Modifier.padding(padding), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "step") { current ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                when (current) {
                    Step.Intro -> IntroStep()
                    Step.Privacy -> PrivacyStep(viewModel)
                    Step.Provider -> ProviderStep(state, viewModel)
                    Step.Identity -> IdentityFields(state, viewModel)
                    Step.Linux -> LinuxStep(state, viewModel, onOpenWorkspace)
                    Step.Ssh -> SshStep(state, viewModel)
                    Step.Connectors -> ConnectorsStep(state, viewModel)
                    Step.Test -> TestStep(state, viewModel)
                    Step.Done -> DoneStep(state, viewModel, onOpenWorkspace)
                }
                Box(Modifier.height(Spacing.xl))
            }
        }
    }
}

/** One thin track per step: done steps filled, the current one filling in. */
@Composable
private fun StepProgress(step: Step) {
    Meter(
        (step.ordinal + 1f) / Step.entries.size,
        Modifier.padding(horizontal = Spacing.lg),
        tone = Tone.Brand,
        contentDescription = stringResource(R.string.onboarding_setup_progress),
    )
}

@Composable
private fun BottomAction(step: Step, state: OnboardingUiState, viewModel: OnboardingViewModel, onFinish: (String?) -> Unit) {
    val (label, enabled) = when (step) {
        Step.Intro -> stringResource(R.string.onboarding_get_started) to true
        Step.Privacy -> stringResource(R.string.onboarding_continue) to viewModel.consent
        Step.Provider -> (if (state.providers.isEmpty()) stringResource(R.string.onboarding_set_up_model_continue) else stringResource(R.string.onboarding_continue)) to state.providers.isNotEmpty()
        Step.Identity -> (if (viewModel.identitySave.running) stringResource(R.string.onboarding_saving) else stringResource(R.string.onboarding_save_continue)) to (viewModel.personaLoaded && !viewModel.identitySave.running)
        Step.Done -> (if (state.providers.isEmpty()) stringResource(R.string.onboarding_finish) else stringResource(R.string.onboarding_start_chatting)) to !viewModel.finishing
        Step.Linux -> (if (state.linux == dev.locus.core.linux.LinuxStatus.Ready) stringResource(R.string.onboarding_continue) else stringResource(R.string.onboarding_continue_without_linux)) to true
        else -> stringResource(R.string.onboarding_continue) to true
    }
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).navigationBarsPadding().imePadding().padding(Spacing.lg)) {
        // With a model set up, the model step offers Quick start as the main way on; the full setup stays
        // one tap away (docs/design/onboarding-quick-start.md).
        if (step == Step.Provider && state.providers.isNotEmpty()) {
            Button(
                onClick = { viewModel.quickStart(state, onFinish) },
                enabled = !viewModel.finishing,
                modifier = Modifier.fillMaxWidth().height(Sizes.touchTarget),
            ) { Text(stringResource(R.string.onboarding_quick_start)) }
            TextButton(
                onClick = viewModel::next,
                enabled = !viewModel.finishing,
                modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.touchTarget),
            ) { Text(stringResource(R.string.onboarding_continue_full_setup)) }
            return@Column
        }
        Button(
            onClick = {
                when (step) {
                    Step.Privacy -> { viewModel.applyProfile(); viewModel.next() }
                    Step.Identity -> viewModel.saveIdentity { viewModel.next() }
                    Step.Done -> viewModel.finish(startChat = true, onDone = onFinish)
                    else -> viewModel.next()
                }
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(Sizes.touchTarget),
        ) { Text(label) }
    }
}

// ---- Steps ---------------------------------------------------------------------------------------

@Composable
private fun ColumnScope.IntroStep() {
    StepHeading(stringResource(R.string.onboarding_intro_title), stringResource(R.string.onboarding_intro_body), Modifier.entrance(0))
    CardGroup(Modifier.entrance(1)) {
        Fact(LocusIcons.Phone, stringResource(R.string.onboarding_runs_here), stringResource(R.string.onboarding_runs_here_body))
        GroupDivider(Insets.iconRow)
        Fact(LocusIcons.Key, stringResource(R.string.onboarding_your_keys), stringResource(R.string.onboarding_your_keys_body))
        GroupDivider(Insets.iconRow)
        Fact(LocusIcons.Shield, stringResource(R.string.onboarding_you_decide), stringResource(R.string.onboarding_you_decide_body))
    }
    SupportingText(
        stringResource(R.string.onboarding_intro_footer),
        Modifier.entrance(2),
        style = LocusType.rowSupporting,
    )
}

@Composable
private fun ColumnScope.PrivacyStep(viewModel: OnboardingViewModel) {
    StepHeading(stringResource(R.string.onboarding_privacy_title), stringResource(R.string.onboarding_privacy_body))
    CardGroup {
        Fact(LocusIcons.Send, stringResource(R.string.onboarding_provider_privacy_title), stringResource(R.string.onboarding_provider_privacy_body))
        GroupDivider(Insets.iconRow)
        Fact(LocusIcons.Key, stringResource(R.string.onboarding_keys_privacy_title), stringResource(R.string.onboarding_keys_privacy_body))
        GroupDivider(Insets.iconRow)
        Fact(LocusIcons.Globe, stringResource(R.string.onboarding_network_privacy_title), stringResource(R.string.onboarding_network_privacy_body))
    }
    // The one request Locus would make on its own, so it's asked here, off by default (docs/design/updates.md).
    if (viewModel.offerUpdateCheck) {
        val updateCheck by viewModel.updateCheck.collectAsStateWithLifecycle()
        CardGroup {
            SwitchRow(
                title = stringResource(R.string.onboarding_update_check_title),
                subtitle = stringResource(R.string.onboarding_update_check_body),
                checked = updateCheck,
                onCheckedChange = viewModel::setUpdateCheck,
                icon = LocusIcons.Regenerate,
            )
        }
    }
    SectionLabel(stringResource(R.string.onboarding_approvals))
    CardGroup {
        Fact(LocusIcons.Check, stringResource(R.string.onboarding_allow), stringResource(R.string.onboarding_allow_body))
        GroupDivider(Insets.iconRow)
        Fact(LocusIcons.Shield, stringResource(R.string.onboarding_ask), stringResource(R.string.onboarding_ask_body))
        GroupDivider(Insets.iconRow)
        Fact(LocusIcons.Close, stringResource(R.string.onboarding_deny), stringResource(R.string.onboarding_deny_body))
    }
    SectionLabel(stringResource(R.string.onboarding_starting_point))
    CardGroup {
        PermissionProfile.entries.forEachIndexed { i, p ->
            // The text starts after the radio button's touch target.
            if (i > 0) GroupDivider(Spacing.sm + Sizes.touchTarget + Spacing.xs)
            Row(
                Modifier.fillMaxWidth().clickable { viewModel.pickProfile(p) }.padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = (viewModel.profile ?: PermissionProfile.Balanced) == p, onClick = { viewModel.pickProfile(p) })
                Column(Modifier.weight(1f).padding(start = Spacing.xs, end = Spacing.sm)) {
                    Text(p.title, style = LocusType.rowTitle)
                    SupportingText(p.summary, style = LocusType.rowSupporting)
                }
            }
        }
    }
    SupportingText(stringResource(R.string.onboarding_permissions_hint))
    LocusCard(outline = if (viewModel.consent) MaterialTheme.colorScheme.primary.copy(alpha = Emphasis.toneOutline) else null, onClick = { viewModel.agree(!viewModel.consent) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = viewModel.consent, onCheckedChange = viewModel::agree)
            Text(
                stringResource(R.string.onboarding_consent),
                Modifier.padding(start = Spacing.xs),
                style = LocusType.rowSupporting,
            )
        }
    }
}

@Composable
private fun ColumnScope.DoneStep(state: OnboardingUiState, viewModel: OnboardingViewModel, onOpenWorkspace: () -> Unit = {}) {
    StepHeading("${viewModel.answers.agentName.trim().ifBlank { "Locus" }} is ready", "Here is what you set up. All of it can be changed in Settings.")
    CardGroup {
        Summary(LocusIcons.Sparkle, stringResource(R.string.onboarding_summary_model), state.defaultProvider?.let { "${it.name} · ${dev.locus.core.model.ModelNames.short(it.model)}" }, stringResource(R.string.onboarding_not_set_up), link = SettingsDestination.Models)
        GroupDivider(Insets.iconRow)
        Summary(LocusIcons.Note, stringResource(R.string.onboarding_summary_agent), "${viewModel.answers.agentName.trim().ifBlank { "Locus" }} · ${viewModel.answers.style.label.lowercase()}", null)
        GroupDivider(Insets.iconRow)
        Summary(LocusIcons.Shield, stringResource(R.string.onboarding_approvals), viewModel.profile?.title ?: "Unchanged", null)
        GroupDivider(Insets.iconRow)
        Summary(
            LocusIcons.Terminal, stringResource(R.string.onboarding_summary_linux), if (state.linux == dev.locus.core.linux.LinuxStatus.Ready) stringResource(R.string.onboarding_installed) else null,
            stringResource(R.string.onboarding_not_installed),
            onOpen = onOpenWorkspace,
        )
        GroupDivider(Insets.iconRow)
        Summary(LocusIcons.Server, stringResource(R.string.onboarding_summary_machines), state.sshHosts.takeIf { it.isNotEmpty() }?.joinToString { it.alias }, stringResource(R.string.onboarding_none))
        GroupDivider(Insets.iconRow)
        Summary(LocusIcons.Plug, stringResource(R.string.onboarding_summary_integrations), state.integrations.filter { it.second }.takeIf { it.isNotEmpty() }?.joinToString { it.first.name }, stringResource(R.string.onboarding_none))
    }
}

// ---- Shared pieces -------------------------------------------------------------------------------

@Composable
internal fun StepHeading(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = Spacing.sm, bottom = Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        SupportingText(body, style = LocusType.rowTitle)
    }
}

@Composable
private fun Fact(icon: ImageVector, title: String, body: String) {
    // The facts are the step's content: never cut.
    ListRow(title, subtitle = body, titleMaxLines = 3, subtitleMaxLines = Int.MAX_VALUE, leading = { IconTile(icon) })
}

@Composable
private fun Summary(
    icon: ImageVector,
    title: String,
    value: String?,
    fallback: String?,
    link: SettingsDestination? = null,
    onOpen: (() -> Unit)? = null,
) {
    // Something not set up whose fallback names a place (a Settings page, the Workspace) opens it; back
    // returns to this step.
    val settings = Locus.settings
    val open = if (value != null) null else onOpen ?: link?.let { page -> settings?.let { { it.open(page) } } }
    ListRow(
        title,
        subtitle = value ?: fallback,
        subtitleMaxLines = 2,
        onClick = open,
        leading = { IconTile(icon, tint = if (value != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
        trailing = when {
            value != null -> { { Icon(LocusIcons.Check, contentDescription = stringResource(R.string.onboarding_done), Modifier.size(IconSize.medium), tint = Locus.colors.live) } }
            open != null -> { { RowChevron() } }
            else -> null
        },
    )
}

/** The state of a one-off action, in one line (or a short block for longer results). */
@Composable
internal fun JobStatus(job: Job, running: String, modifier: Modifier = Modifier) {
    when {
        job.running -> StatusLine(running, Tone.Live, modifier)
        job.error != null -> Text(job.error, modifier, style = LocusType.supporting, color = Locus.colors.critical)
        job.result != null -> Row(modifier, verticalAlignment = Alignment.Top) {
            Icon(LocusIcons.Check, null, Modifier.padding(top = Spacing.xxs).size(IconSize.small), tint = Locus.colors.live)
            SupportingText(
                job.result,
                Modifier.padding(start = Spacing.sm),
                style = if ('\n' in job.result) LocusType.mono else LocusType.supporting,
            )
        }
    }
}

/** A quiet panel for command output and fingerprints. */
@Composable
internal fun OutputBlock(text: String, modifier: Modifier = Modifier) {
    CodeSurface(modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(Spacing.md), style = LocusType.mono)
    }
}
