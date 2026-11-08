package dev.locus.feature.settings

import dev.locus.feature.settings.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.locus.core.designsystem.CardGroup
import dev.locus.core.designsystem.IconTile
import dev.locus.core.designsystem.ListRow
import dev.locus.core.designsystem.LocusCard
import dev.locus.core.designsystem.LocusIcons
import dev.locus.core.designsystem.SectionLabel
import dev.locus.core.designsystem.SectionNote
import dev.locus.core.designsystem.RowChevron
import dev.locus.core.designsystem.Segmented
import dev.locus.core.designsystem.SwitchRow
import dev.locus.core.designsystem.TextArea
import dev.locus.core.model.HelperTimeLimit

/** Who the agent is and how it should work with you. */
@Composable
internal fun AgentSettings(
    state: SettingsUiState,
    onBack: () -> Unit,
    onSaveInstructions: (String) -> Unit,
    onOpenAgentSetup: () -> Unit,
    onSetTaskBudget: (dev.locus.core.model.TaskBudget) -> Unit = {},
    onSetHelperReports: (Boolean) -> Unit = {},
    onSetHelperTimeout: (Int) -> Unit = {},
) {
    SettingsPageScaffold(stringResource(R.string.agent_settings_title), onBack) {
        item { SectionLabel(stringResource(R.string.agent_identity), SectionPadding) }
        item {
            CardGroup {
                ListRow(
                    title = stringResource(R.string.agent_identity_title),
                    subtitle = stringResource(R.string.agent_identity_body),
                    subtitleMaxLines = 2,
                    onClick = onOpenAgentSetup,
                    leading = { IconTile(LocusIcons.Edit) },
                    trailing = { RowChevron() },
                )
            }
        }
        item { SectionLabel(stringResource(R.string.agent_custom_instructions), SectionPadding) }
        item { InstructionsEditor(state.customInstructions, onSaveInstructions) }
        item { SectionNote(stringResource(R.string.agent_custom_instructions_body), NotePadding) }
        item { SectionLabel(stringResource(R.string.agent_task_budget), SectionPadding) }
        item {
            LocusCard {
                dev.locus.core.designsystem.TaskBudgetEditor(
                    state.taskBudget,
                    onChange = onSetTaskBudget,
                    costNote = stringResource(R.string.agent_task_budget_prices).takeIf { !state.modelCatalog },
                )
            }
        }
        item {
            SectionNote(
                stringResource(R.string.agent_task_budget_body) +
                    stringResource(R.string.agent_task_budget_note),
                NotePadding,
            )
        }
        item { SectionLabel(stringResource(R.string.agent_helpers), SectionPadding) }
        item {
            CardGroup {
                SwitchRow(
                    title = stringResource(R.string.agent_helpers_continue),
                    checked = state.subagentReportsContinue,
                    onCheckedChange = onSetHelperReports,
                    subtitle = stringResource(R.string.agent_helpers_continue_body),
                    icon = LocusIcons.Branch,
                )
            }
        }
        item { SectionLabel(stringResource(R.string.agent_helper_timeout), SectionPadding) }
        item {
            val choices = HelperTimeLimit.CHOICES
            LocusCard {
                Segmented(
                    options = choices.map(HelperTimeLimit::label),
                    selected = choices.indexOf(HelperTimeLimit.normalized(state.helperTimeoutMinutes)).coerceAtLeast(0),
                    onSelect = { onSetHelperTimeout(choices[it]) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            SectionNote(
                stringResource(R.string.agent_helper_timeout_body) +
                    stringResource(R.string.agent_helper_timeout_body2) +
                    stringResource(R.string.agent_helper_timeout_body3),
                NotePadding,
            )
        }
    }
}

@Composable
private fun InstructionsEditor(saved: String, onSave: (String) -> Unit) {
    var text by rememberSaveable(saved) { mutableStateOf(saved) }
    LocusCard {
        TextArea(
            value = text,
            onValueChange = { text = it },
            placeholder = "e.g. Reply briefly. Keep notes in notes/. I work in UTC+5:45.",
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The note under the card says what this is; here only the button.
            Box(Modifier.weight(1f))
            TextButton(onClick = { onSave(text) }, enabled = text != saved) { Text(stringResource(R.string.agent_save)) }
        }
    }
}
