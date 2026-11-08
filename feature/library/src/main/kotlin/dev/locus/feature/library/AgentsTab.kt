package dev.locus.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import dev.locus.core.agent.home.AgentHome
import dev.locus.core.agent.tools.AgentProfile
import dev.locus.core.data.repository.Library
import dev.locus.core.designsystem.CardGroup
import dev.locus.core.designsystem.ExpandableRow
import dev.locus.core.designsystem.GroupDivider
import dev.locus.core.designsystem.Insets
import dev.locus.core.designsystem.Locus
import dev.locus.core.designsystem.LocusIcons
import dev.locus.core.designsystem.LocusType
import dev.locus.core.designsystem.SectionLabel
import dev.locus.core.designsystem.SectionNote
import dev.locus.core.designsystem.Spacing
import dev.locus.core.designsystem.SupportingText
import dev.locus.core.designsystem.Tone

/**
 * Library → Helpers: the helper (sub-agent) types the agent can start. Built-in ones are listed for
 * reference; ones the agent drafts with agent_create wait here for review, like skills.
 */
@Composable
internal fun AgentsTab(library: Library, viewModel: LibraryViewModel, modifier: Modifier) {
    val padding = PaddingValues(start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = Spacing.xxl)
    LazyColumn(modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        item {
            SectionNote(
                stringResource(R.string.helpers_intro),
            )
        }
        if (library.agents.isNotEmpty()) {
            item {
                CardGroup {
                    library.agents.forEachIndexed { i, agent ->
                        if (i > 0) GroupDivider(inset = Insets.iconRow)
                        DefinitionRow(agent, viewModel, approvedActive = library.agents.any { !it.draft && it.name == agent.name })
                    }
                }
            }
        } else {
            item {
                SectionNote(
                    stringResource(R.string.helpers_empty),
                )
            }
        }
        item { SectionLabel(stringResource(R.string.built_in), Modifier.padding(start = 0.dp)) }
        item {
            CardGroup {
                AgentProfile.BUILT_IN.forEachIndexed { i, profile ->
                    if (i > 0) GroupDivider(inset = Insets.iconRow)
                    ExpandableRow(profile.label, profile.description, helperIcon(profile.type), iconDescription = stringResource(R.string.built_in_helper)) {
                        ToolsLine(profile.tools)
                        SupportingText(
                            buildString {
                                append(if (profile.model == AgentProfile.MODEL_HELPER) stringResource(R.string.helper_model) else stringResource(R.string.chat_model))
                                profile.effort?.let { append(" · ${it.label} effort") }
                                append(" · up to ${profile.maxSteps} steps")
                            },
                            style = LocusType.mono,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DefinitionRow(agent: AgentHome.AgentDefinition, viewModel: LibraryViewModel, approvedActive: Boolean) {
    var confirmRemove by remember { mutableStateOf(false) }
    var confirmChanged by remember { mutableStateOf(false) }
    if (confirmChanged) {
        ConfirmApproveChanged(
            agent.name,
            "helper",
            onDismiss = { confirmChanged = false },
            onConfirm = { confirmChanged = false; viewModel.approveAgent(agent.name) },
        )
    }
    if (confirmRemove) {
        ConfirmRemove(
            title = "Remove “${agent.name}”?",
            body = "Locus can't start this helper any more. Its definition is deleted from this phone.",
            action = stringResource(R.string.remove),
            onDismiss = { confirmRemove = false },
            onConfirm = { confirmRemove = false; viewModel.removeAgent(agent.name) },
        )
    }
    ExpandableRow(
        agent.name,
        agent.description,
        LocusIcons.Sparkle,
        badge = when {
            agent.changedOutside -> CHANGED_OUTSIDE
            agent.draft -> stringResource(R.string.needs_review)
            else -> null
        },
        badgeTone = if (agent.changedOutside) Tone.Critical else Tone.Caution,
        startExpanded = agent.draft,
        iconDescription = stringResource(R.string.your_helper),
    ) {
        if (agent.changedOutside) ChangedOutsideBanner("helper", approvedActive)
        ToolsLine(agent.tools)
        SupportingText(
            buildString {
                append(
                    when (agent.model.lowercase()) {
                        "inherit", "same" -> stringResource(R.string.chat_model)
                        "helper" -> stringResource(R.string.helper_model)
                        else -> agent.model
                    },
                )
                agent.effort?.let { append(" · $it effort") }
                agent.maxSteps?.let { append(" · up to $it steps") }
                if (agent.background) append(" · runs in the background")
            },
            style = LocusType.mono,
        )
        Markdown(
            content = agent.body,
            modifier = Modifier.fillMaxWidth().codeWell(),
        )
        // Actions wrap to a second line at a large font instead of running off the card.
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            if (agent.draft) {
                OutlinedButton(onClick = { viewModel.rejectAgent(agent.name) }) { Text(if (agent.changedOutside) stringResource(R.string.discard_change) else stringResource(R.string.reject)) }
                Button(onClick = { if (agent.changedOutside) confirmChanged = true else viewModel.approveAgent(agent.name) }) { Text(stringResource(R.string.approve)) }
            } else {
                TextButton(onClick = { confirmRemove = true }) { Text(stringResource(R.string.remove), color = Locus.colors.critical) }
            }
        }
    }
}

/** A built-in helper's mark, by what it's for. */
internal fun helperIcon(type: String): androidx.compose.ui.graphics.vector.ImageVector = when (type) {
    "researcher" -> LocusIcons.Search
    "coder" -> LocusIcons.Code
    "explorer" -> LocusIcons.Folder
    "ops" -> LocusIcons.Terminal
    "browser" -> LocusIcons.Globe
    else -> LocusIcons.Sparkle
}

@Composable
private fun ToolsLine(tools: List<String>) {
    SupportingText(
        "Tools: ${tools.joinToString()}",
    )
}

