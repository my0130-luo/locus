package dev.locus.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import dev.locus.feature.onboarding.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.locus.core.agent.home.AgentHome
import dev.locus.core.agent.home.AgentHome.PersonaFile
import dev.locus.core.designsystem.Banner
import dev.locus.core.designsystem.CardGroup
import dev.locus.core.designsystem.ChipVariant
import dev.locus.core.designsystem.GroupDivider
import dev.locus.core.designsystem.IconSize
import dev.locus.core.designsystem.ListRow
import dev.locus.core.designsystem.LocusCard
import dev.locus.core.designsystem.LocusChip
import dev.locus.core.designsystem.LocusIcons
import dev.locus.core.designsystem.LocusSheet
import dev.locus.core.designsystem.LocusTopBar
import dev.locus.core.designsystem.LocusType
import dev.locus.core.designsystem.MonoCaption
import dev.locus.core.designsystem.SectionLabel
import dev.locus.core.designsystem.Segmented
import dev.locus.core.designsystem.Sizes
import dev.locus.core.designsystem.Spacing
import dev.locus.core.designsystem.StatusLine
import dev.locus.core.designsystem.SupportingText
import dev.locus.core.designsystem.Tone

/** A few friendly questions; Locus writes the persona files from the answers. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColumnScope.IdentityFields(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    StepHeading(stringResource(R.string.onb_identity_make_yours), stringResource(R.string.onb_identity_intro))
    if (!viewModel.personaLoaded) {
        StatusLine(stringResource(R.string.onb_identity_loading), Tone.Neutral)
        return
    }
    val a = viewModel.answers
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    PersonaChanges(viewModel)

    SectionLabel(stringResource(R.string.onb_identity_agent))
    LocusCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OutlinedTextField(
                a.agentName,
                { v -> viewModel.editAnswers { it.copy(agentName = v.take(40)) } },
                label = { Text(stringResource(R.string.onb_identity_its_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Question(stringResource(R.string.onb_identity_talk_style))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TalkStyle.entries.forEach { s ->
                    Choice(s.label, selected = a.style == s) { viewModel.editAnswers { it.copy(style = s) } }
                }
            }
            Question(stringResource(R.string.onb_identity_before_acting))
            Segmented(
                Autonomy.entries.map { it.label },
                selected = a.autonomy.ordinal,
                onSelect = { i -> viewModel.editAnswers { it.copy(autonomy = Autonomy.entries[i]) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Hint("${a.autonomy.summary} Your approval settings still apply on top.")
        }
    }

    SectionLabel(stringResource(R.string.onb_identity_help_with))
    LocusCard {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            HelpArea.entries.forEach { area ->
                Choice(area.label, selected = area in a.areas) {
                    viewModel.editAnswers { it.copy(areas = if (area in it.areas) it.areas - area else it.areas + area) }
                }
            }
        }
    }

    SectionLabel(stringResource(R.string.onb_identity_about_you))
    LocusCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Field(a.userName, stringResource(R.string.onb_identity_your_name), words = true) { v -> viewModel.editAnswers { it.copy(userName = v) } }
            Field(a.callMe, stringResource(R.string.onb_identity_call_you), words = true, placeholder = a.userName.substringBefore(' ').ifBlank { null }) { v -> viewModel.editAnswers { it.copy(callMe = v) } }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Field(a.timeZone, stringResource(R.string.onb_identity_timezone), Modifier.weight(1f)) { v -> viewModel.editAnswers { it.copy(timeZone = v) } }
                Field(a.language, stringResource(R.string.onb_identity_language), Modifier.weight(1f), words = true) { v -> viewModel.editAnswers { it.copy(language = v) } }
            }
            Field(a.location, stringResource(R.string.onb_identity_location), words = true) { v -> viewModel.editAnswers { it.copy(location = v) } }
        }
    }

    SectionLabel(stringResource(R.string.onb_identity_rules))
    LocusCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Rules(a.always, stringResource(R.string.onb_identity_always), stringResource(R.string.onb_identity_always_hint)) { v -> viewModel.editAnswers { it.copy(always = v) } }
            Rules(a.never, stringResource(R.string.onb_identity_never), stringResource(R.string.onb_identity_never_hint)) { v -> viewModel.editAnswers { it.copy(never = v) } }
            Hint(stringResource(R.string.onb_identity_one_per_line))
        }
    }

    SectionLabel("Here's what ${a.agentName.trim().ifBlank { AgentHome.DEFAULT_NAME }} will remember", action = stringResource(R.string.onb_identity_edit_files), onAction = { editing = PersonaFile.IDENTITY.name })
    val override = viewModel.personaOverride
    if (override != null) {
        Banner(
            stringResource(R.string.onb_identity_override_note),
            Tone.Caution,
            actions = { TextButton(onClick = { viewModel.setPersonaText(null) }) { Text(stringResource(R.string.onb_identity_use_answers)) } },
        )
    }
    val preview = viewModel.personaPreview
    CardGroup {
        PersonaFile.entries.forEachIndexed { i, f ->
            if (i > 0) GroupDivider()
            ListRow(
                f.title(),
                subtitle = preview[f].readable().ifBlank { stringResource(R.string.onb_identity_nothing_yet) },
                subtitleMaxLines = 5,
                onClick = { editing = f.name },
                trailing = { SupportingText(f.fileName, style = LocusType.mono) },
            )
        }
    }
    if (viewModel.canPolish(state)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedButton(onClick = viewModel::polishPersona, enabled = !viewModel.personaPolish.running) {
                Icon(LocusIcons.Sparkle, null, Modifier.size(IconSize.medium))
                Text(stringResource(R.string.onb_identity_write_with_model), Modifier.padding(start = Spacing.sm))
            }
        }
        JobStatus(viewModel.personaPolish, stringResource(R.string.onb_identity_asking_model))
    }
    Hint(stringResource(R.string.onb_identity_storage_note))
    JobStatus(viewModel.identitySave, stringResource(R.string.onb_identity_saving))

    editing?.let { name ->
        PersonaEditorSheet(
            initial = preview,
            start = PersonaFile.valueOf(name),
            onSave = { viewModel.setPersonaText(it); editing = null },
            onDismiss = { editing = null },
        )
    }
}

/**
 * Persona files changed outside Locus (docs/design/linux-isolation.md, "Sealed instructions"): one
 * banner each, and a sheet to read the change and keep or discard it. Until then new chats use the
 * version the user saved.
 */
@Composable
private fun PersonaChanges(viewModel: OnboardingViewModel) {
    val changes = viewModel.personaChanges
    var reviewing by rememberSaveable { mutableStateOf<String?>(null) }
    changes.keys.sortedBy { it.ordinal }.forEach { f ->
        Banner(
            "${f.fileName} was changed by something other than Locus, for example a program in Linux. " +
                "Locus isn't using the change: new chats get the version you saved.",
            Tone.Critical,
            title = "${f.title()} changed outside Locus",
            actions = { TextButton(onClick = { reviewing = f.name }) { Text(stringResource(R.string.onb_identity_review)) } },
        )
    }
    JobStatus(viewModel.personaReview, "Saving…")
    val file = reviewing?.let(PersonaFile::valueOf) ?: return
    val text = changes[file] ?: return
    LocusSheet(
        title = "The change to ${file.fileName}",
        subtitle = "Made outside Locus. Keep it only if you made it yourself: your agent reads this file at the start of every chat.",
        onDismiss = { reviewing = null },
        action = stringResource(R.string.onb_identity_keep_version),
        actionTone = Tone.Caution,
        actionEnabled = !viewModel.personaReview.running,
        onAction = { reviewing = null; viewModel.keepPersonaChange(file) },
    ) {
        OutputBlock(text.trim().ifEmpty { "(empty: the file was emptied)" })
        OutlinedButton(
            onClick = { reviewing = null; viewModel.discardPersonaChange(file) },
            enabled = !viewModel.personaReview.running,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.onb_identity_discard_change)) }
    }
}

/** The optional raw editor: the four Markdown files as Locus will write them. */
@Composable
private fun PersonaEditorSheet(initial: PersonaFiles, start: PersonaFile, onSave: (PersonaFiles) -> Unit, onDismiss: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(start.ordinal) }
    var identity by rememberSaveable { mutableStateOf(initial.identity) }
    var soul by rememberSaveable { mutableStateOf(initial.soul) }
    var user by rememberSaveable { mutableStateOf(initial.user) }
    var agents by rememberSaveable { mutableStateOf(initial.agents) }
    val files = PersonaFiles(identity, soul, user, agents)
    LocusSheet(
        title = "Edit files directly",
        subtitle = stringResource(R.string.onb_identity_optional_markdown),
        onDismiss = onDismiss,
        action = "Done",
        onAction = { onSave(files) },
    ) {
        Segmented(PersonaFile.entries.map { it.title() }, selected = tab, onSelect = { tab = it }, compact = true, modifier = Modifier.fillMaxWidth())
        val file = PersonaFile.entries[tab]
        OutlinedTextField(
            files[file],
            { v ->
                when (file) {
                    PersonaFile.IDENTITY -> identity = v
                    PersonaFile.SOUL -> soul = v
                    PersonaFile.USER -> user = v
                    PersonaFile.AGENTS -> agents = v
                }
            },
            label = { Text(file.fileName) },
            textStyle = MonoCaption,
            minLines = 8,
            maxLines = 18,
            supportingText = {
                val n = files[file].length
                Text(if (n > AgentHome.PERSONA_BUDGET) "$n characters; only the first ${AgentHome.PERSONA_BUDGET} are used" else "$n / ${AgentHome.PERSONA_BUDGET}")
            },
            isError = files[file].length > AgentHome.PERSONA_BUDGET,
            modifier = Modifier.fillMaxWidth(),
        )
        if (file == PersonaFile.IDENTITY) Hint("Keep a “- Name: …” line: it's what the agent calls itself.")
    }
}

/** The questionnaire on its own, for Settings → Agent identity. */
@Composable
fun PersonaRoute(onBack: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { LocusTopBar(stringResource(R.string.onb_identity_agent_identity), onBack = onBack) },
        bottomBar = {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(Spacing.lg)) {
                Button(
                    onClick = { viewModel.saveIdentity(onBack) },
                    enabled = viewModel.personaLoaded && !viewModel.identitySave.running,
                    modifier = Modifier.fillMaxWidth().height(Sizes.touchTarget),
                ) { Text(if (viewModel.identitySave.running) stringResource(R.string.onb_identity_saving) else stringResource(R.string.onb_identity_save)) }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) { IdentityFields(state, viewModel) }
    }
}

// ---- Pieces --------------------------------------------------------------------------------------

@Composable
private fun PersonaFile.title() = when (this) {
    PersonaFile.IDENTITY -> stringResource(R.string.onb_identity_agent_identity)
    PersonaFile.SOUL -> stringResource(R.string.onb_identity_personality)
    PersonaFile.USER -> "About you"
    PersonaFile.AGENTS -> stringResource(R.string.onb_identity_rules_tab)
}

/** The file without Markdown headings and bullets, for the preview rows. */
private fun String.readable() = lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
    .joinToString("\n") { it.removePrefix("- ").removePrefix("* ") }

@Composable
private fun Question(text: String) {
    Text(text, style = LocusType.cardTitle)
}

@Composable
private fun Hint(text: String) {
    SupportingText(text)
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    LocusChip(label, variant = ChipVariant.Filter, selected = selected, onClick = onClick)
}

@Composable
private fun Field(
    value: String,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    words: Boolean = false,
    placeholder: String? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value,
        { onChange(it.take(80)) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = if (words) KeyboardCapitalization.Words else KeyboardCapitalization.None,
            imeAction = ImeAction.Next,
        ),
        modifier = modifier,
    )
}

@Composable
private fun Rules(value: String, label: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value,
        { onChange(it.take(1_000)) },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        minLines = 2,
        maxLines = 6,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
}
