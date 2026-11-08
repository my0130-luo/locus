package dev.locus.feature.settings

import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.locus.core.designsystem.CardGroup
import dev.locus.core.designsystem.GroupDivider
import dev.locus.core.designsystem.IconSize
import dev.locus.core.designsystem.IconTile
import dev.locus.core.designsystem.ListRow
import dev.locus.core.designsystem.LocusIcons
import dev.locus.core.designsystem.LocusSheet
import dev.locus.core.designsystem.ProviderBadge
import dev.locus.core.designsystem.SectionLabel
import dev.locus.core.designsystem.SectionNote
import dev.locus.core.designsystem.SupportingText
import dev.locus.core.llm.RemoteModel
import dev.locus.core.model.ApiFormat
import dev.locus.core.model.HelperPrompt
import dev.locus.core.model.ProviderConfig
import dev.locus.core.designsystem.Insets
import dev.locus.core.designsystem.RowChevron
import dev.locus.core.designsystem.SwitchRow

/** The helper model and the small chat extras: titles, follow-ups, lessons after tasks, and their prompts. */
@Composable
internal fun ExtrasSettings(
    state: SettingsUiState,
    onBack: () -> Unit,
    onSetHelper: (String?, String, Boolean) -> Unit,
    onSetAutoTitle: (Boolean) -> Unit,
    onSetSuggest: (Boolean) -> Unit,
    onSetReflect: (Boolean) -> Unit,
    onSetPrompt: (HelperPrompt, String?) -> Unit,
    onListModels: (ProviderConfig, String?, (Result<List<RemoteModel>>) -> Unit) -> Unit,
) {
    var editingHelper by remember { mutableStateOf(false) }
    var editingPrompt by remember { mutableStateOf<HelperPrompt?>(null) }
    val helper = state.providers.firstOrNull { it.config.id == state.helperProviderId }?.config

    SettingsPageScaffold(stringResource(R.string.extras_title), onBack) {
        item {
            CardGroup {
                ListRow(
                    title = stringResource(R.string.extras_helper_model),
                    subtitle = helper?.let { "${it.name} · ${dev.locus.core.model.ModelNames.short(state.helperModel.ifBlank { it.model })}" }
                        ?: if (state.helperAutomatic) stringResource(R.string.extras_automatic) else stringResource(R.string.extras_same_as_chat),
                    subtitleMaxLines = 2,
                    onClick = { editingHelper = true },
                    leading = {
                        when {
                            helper != null -> ProviderBadge(helper.name, helper.baseUrl, false, size = IconSize.tile)
                            state.helperAutomatic -> IconTile(LocusIcons.Sparkle)
                            else -> IconTile(LocusIcons.Chats)
                        }
                    },
                    trailing = { RowChevron() },
                )
            }
        }
        item {
            SectionNote(
                "Titles, suggestions and the researcher and explorer helpers run on the helper model; a cheaper one saves cost. " +
                    "The safety reviewer uses it only when you choose one here.",
                NotePadding,
            )
        }
        item { SectionLabel(stringResource(R.string.extras_chat_extras), SectionPadding) }
        item {
            CardGroup {
                SwitchRow(
                    title = stringResource(R.string.extras_auto_name),
                    checked = state.autoTitle,
                    onCheckedChange = onSetAutoTitle,
                    subtitle = stringResource(R.string.extras_auto_name_body),
                    icon = LocusIcons.Chats,
                )
                GroupDivider(inset = Insets.iconRow)
                SwitchRow(
                    title = stringResource(R.string.extras_suggest_followups),
                    checked = state.suggestFollowUps,
                    onCheckedChange = onSetSuggest,
                    subtitle = stringResource(R.string.extras_suggest_body),
                    icon = LocusIcons.Send,
                )
                GroupDivider(inset = Insets.iconRow)
                SwitchRow(
                    title = stringResource(R.string.extras_learn_after_tasks),
                    checked = state.reflectAfterTasks,
                    onCheckedChange = onSetReflect,
                    subtitle = stringResource(R.string.extras_learn_body),
                    icon = LocusIcons.Note,
                )
            }
        }
        item { SectionLabel(stringResource(R.string.extras_prompts), SectionPadding) }
        item {
            CardGroup {
                HelperPrompt.entries.forEachIndexed { i, prompt ->
                    if (i > 0) GroupDivider(inset = Insets.iconRow)
                    ListRow(
                        title = prompt.label,
                        subtitle = if (prompt in state.helperPrompts) stringResource(R.string.extras_edited) else stringResource(R.string.extras_default),
                        onClick = { editingPrompt = prompt },
                        leading = { IconTile(prompt.icon) },
                        trailing = { RowChevron() },
                    )
                }
            }
        }
        item {
            SectionNote(
                "What Locus asks for titles, suggestions and summaries of long chats. " +
                    "The safety reviewer's prompt can't be edited: it decides which actions run without asking you, so a changed prompt could quietly weaken approvals.",
                NotePadding,
            )
        }
    }

    editingPrompt?.let { prompt ->
        HelperPromptSheet(
            prompt = prompt,
            saved = state.helperPrompts[prompt],
            onSave = { text -> onSetPrompt(prompt, text); editingPrompt = null },
            onDismiss = { editingPrompt = null },
        )
    }

    if (editingHelper) {
        HelperSheet(
            providers = state.providers.map { it.config }.filter { it.format != ApiFormat.ON_DEVICE },
            selectedId = state.helperProviderId,
            selectedModel = state.helperModel,
            selectedAutomatic = state.helperAutomatic,
            onListModels = onListModels,
            onSave = { id, model, automatic -> onSetHelper(id, model, automatic); editingHelper = false },
            onDismiss = { editingHelper = false },
        )
    }
}

/** What stringResource(R.string.extras_automatic_label) picks, in a few words for the row and the sheet. */
private const val AUTOMATIC_SHORT = "a cheaper model from each chat's provider"

/** Picks the helper model: Automatic, the same as each chat, or a provider and, optionally, a cheaper model on it. */
@Composable
private fun HelperSheet(
    providers: List<ProviderConfig>,
    selectedId: String?,
    selectedModel: String,
    selectedAutomatic: Boolean,
    onListModels: (ProviderConfig, String?, (Result<List<RemoteModel>>) -> Unit) -> Unit,
    onSave: (String?, String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var id by remember { mutableStateOf(selectedId) }
    var model by remember { mutableStateOf(selectedModel) }
    // Only read while no provider is chosen.
    var automatic by remember { mutableStateOf(selectedAutomatic) }
    LocusSheet(
        title = stringResource(R.string.extras_helper_model),
        subtitle = "Writes chat titles and follow-up suggestions. Researcher and explorer helpers run on it too, and the safety reviewer when you choose a model here.",
        onDismiss = onDismiss,
        action = stringResource(R.string.extras_save),
        onAction = { onSave(id, if (id == null) "" else model, id == null && automatic) },
    ) {
        CardGroup {
            HelperOption(
                stringResource(R.string.extras_automatic_label),
                "A cheaper model of the chat's provider (Haiku for Claude), when one is known",
                selected = id == null && automatic,
                onClick = { id = null; model = ""; automatic = true },
            ) { IconTile(LocusIcons.Sparkle) }
            GroupDivider(inset = Insets.iconRow)
            HelperOption(stringResource(R.string.extras_same_as_chat), "Side jobs and helpers use the chat's own model", selected = id == null && !automatic, onClick = { id = null; model = ""; automatic = false }) {
                IconTile(LocusIcons.Chats)
            }
            providers.forEach { p ->
                GroupDivider(inset = Insets.iconRow)
                HelperOption(p.name, dev.locus.core.model.ModelNames.short(p.model), selected = id == p.id, onClick = { if (id != p.id) { id = p.id; model = "" } }) {
                    ProviderBadge(p.name, p.baseUrl, false, size = IconSize.tile)
                }
            }
        }
        val provider = providers.firstOrNull { it.id == id }
        if (provider != null) {
            ModelField(
                model = model,
                onModel = { m, _ -> model = m },
                canLoad = true,
                load = { callback -> onListModels(provider, null, callback) },
                reloadKey = provider.id,
            )
            SupportingText(
                "Leave empty to use ${provider.model}. A small, fast model (a mini, flash or haiku tier) is plenty here.",
            )
        }
    }
}

@Composable
private fun HelperOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit, leading: @Composable () -> Unit) {
    ListRow(
        title = title,
        subtitle = subtitle,
        titleMaxLines = 2,
        subtitleMaxLines = 6,
        onClick = onClick,
        leading = leading,
        trailing = { RadioButton(selected = selected, onClick = onClick) },
    )
}
