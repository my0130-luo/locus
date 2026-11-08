package dev.locus.feature.settings

import dev.locus.feature.settings.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.remember
import dev.locus.core.designsystem.CardGroup
import dev.locus.core.designsystem.GroupDivider
import dev.locus.core.designsystem.IconSize
import dev.locus.core.designsystem.IconTile
import dev.locus.core.designsystem.ListRow
import dev.locus.core.designsystem.LocusIcons
import dev.locus.core.designsystem.LocusType
import dev.locus.core.designsystem.ProviderBadge
import dev.locus.core.designsystem.SectionLabel
import dev.locus.core.designsystem.Spacing
import dev.locus.core.designsystem.StatusPill
import dev.locus.core.designsystem.LocusChip
import dev.locus.core.designsystem.SupportingText
import dev.locus.core.designsystem.rememberBefore
import androidx.compose.runtime.getValue
import dev.locus.core.designsystem.Tone
import dev.locus.core.model.ApiFormat
import dev.locus.core.designsystem.Insets
import dev.locus.core.designsystem.RowChevron

/** The top of Settings: one row per area, each with a one-line summary of its current state. */
@Composable
internal fun SettingsHub(state: SettingsUiState, onBack: () -> Unit, onOpen: (SettingsPage) -> Unit) {
    val defaultProvider = state.providers.firstOrNull { it.isDefault }?.config
    val missingCredential = state.providers.firstOrNull { it.config.needsCredential && !it.hasKey }?.config
    val version = appVersion()
    // Until the settings have loaded, every summary shows the same placeholder rather than a guess.
    fun summary(text: String) = if (state.loading) LOADING_SUMMARY else text

    SettingsPageScaffold(stringResource(R.string.settings_title), onBack) {
        item { SectionLabel(stringResource(R.string.settings_providers), SectionPadding) }
        item {
            CardGroup {
                HubRow(
                    title = stringResource(R.string.settings_models),
                    summary = summary(modelsSummary(state)),
                    onClick = { onOpen(SettingsPage.Models) },
                    leading = {
                        if (defaultProvider != null) {
                            ProviderBadge(defaultProvider.name, defaultProvider.baseUrl, defaultProvider.format == ApiFormat.ON_DEVICE, size = IconSize.tile)
                        } else {
                            IconTile(LocusIcons.Key)
                        }
                    },
                    pill = when {
                        // A draft from the agent waits for the user: first, like Library's own stringResource(R.string.settings_needs_review).
                        state.providerDrafts.isNotEmpty() -> (stringResource(R.string.settings_needs_review) to Tone.Caution)
                        !state.loading && state.providers.isEmpty() -> (stringResource(R.string.settings_set_up) to Tone.Caution)
                        missingCredential != null -> ((if (missingCredential.signsIn) stringResource(R.string.settings_signed_out) else stringResource(R.string.settings_no_key)) to Tone.Caution)
                        else -> null
                    },
                )
                GroupDivider(inset = Insets.iconRow)
                HubRow(stringResource(R.string.settings_helper_extras), summary(extrasSummary(state)), LocusIcons.Chats) { onOpen(SettingsPage.Extras) }
                GroupDivider(inset = Insets.iconRow)
                // Proxy and certificates apply to model providers only.
                HubRow(stringResource(R.string.settings_network), summary(networkHubSummary(state.network)), LocusIcons.Globe) { onOpen(SettingsPage.Network) }
            }
        }

        item { SectionLabel(stringResource(R.string.settings_how_locus_works), SectionPadding) }
        item {
            CardGroup {
                HubRow(stringResource(R.string.settings_agent), summary(agentSummary(state)), LocusIcons.Note) { onOpen(SettingsPage.Agent) }
                GroupDivider(inset = Insets.iconRow)
                HubRow(stringResource(R.string.settings_permissions), summary(permissionsSummary(state.tools) + reviewerSummary(state.reviewerMode)), LocusIcons.Shield) { onOpen(SettingsPage.Permissions) }
                GroupDivider(inset = Insets.iconRow)
                HubRow(stringResource(R.string.settings_battery_heat), stringResource(R.string.settings_battery_summary), LocusIcons.Gauge) { onOpen(SettingsPage.Battery) }
                GroupDivider(inset = Insets.iconRow)
                HubRow(stringResource(R.string.settings_voice), stringResource(R.string.settings_voice_summary), LocusIcons.Mic) { onOpen(SettingsPage.Voice) }
                GroupDivider(inset = Insets.iconRow)
                HubRow(stringResource(R.string.settings_automation), stringResource(R.string.settings_automation_summary), LocusIcons.Play) { onOpen(SettingsPage.Automation) }
                GroupDivider(inset = Insets.iconRow)
                HubRow(stringResource(R.string.settings_phone), stringResource(R.string.settings_phone_summary), LocusIcons.Phone) { onOpen(SettingsPage.Phone) }
            }
        }

        item { SectionLabel(stringResource(R.string.settings_connections), SectionPadding) }
        item {
            CardGroup {
                // Remote mode keeps a connection to Telegram open (battery), so it shows here until it times out.
                val remoteOn by rememberBefore(state.telegramRemote.switchesOffAt)
                HubRow(
                    stringResource(R.string.settings_integrations),
                    summary(integrationsSummary(state)),
                    onClick = { onOpen(SettingsPage.Integrations) },
                    leading = { IconTile(LocusIcons.Plug) },
                    chip = if (remoteOn) ({ LocusChip(stringResource(R.string.settings_remote_on), tone = Tone.Live, dot = true, contentDescription = stringResource(R.string.settings_telegram_remote_on)) }) else null,
                )
                GroupDivider(inset = Insets.iconRow)
                HubRow(stringResource(R.string.settings_ssh_machines), summary(sshSummary(state)), LocusIcons.Server) { onOpen(SettingsPage.Ssh) }
                GroupDivider(inset = Insets.iconRow)
                HubRow(stringResource(R.string.settings_saved_logins), stringResource(R.string.settings_saved_logins_summary), LocusIcons.Key) { onOpen(SettingsPage.Logins) }
            }
        }

        item { SectionLabel(stringResource(R.string.settings_data), SectionPadding) }
        item {
            CardGroup {
                HubRow(stringResource(R.string.settings_backup_restore), stringResource(R.string.settings_backup_summary), LocusIcons.Restore) { onOpen(SettingsPage.Backup) }
            }
        }

        // No label: a lone "About" label over stringResource(R.string.settings_about_privacy) would say the same thing twice.
        item { Spacer(Modifier.height(Spacing.xl)) }
        item {
            CardGroup {
                HubRow(stringResource(R.string.settings_about_privacy), stringResource(R.string.settings_about_summary), LocusIcons.Info, meta = version?.let { "v$it" }) { onOpen(SettingsPage.About) }
            }
        }
    }
}

@Composable
private fun HubRow(title: String, summary: String, icon: ImageVector, meta: String? = null, onClick: () -> Unit) {
    HubRow(title, summary, onClick, leading = { IconTile(icon) }, meta = meta)
}

@Composable
private fun HubRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    pill: Pair<String, Tone>? = null,
    meta: String? = null,
    chip: (@Composable () -> Unit)? = null,
) {
    // At a large font the pill and chip go under the summary, so the title keeps the row's width.
    val large = LocalDensity.current.fontScale > LARGE_FONT
    ListRow(
        title = title,
        subtitle = summary,
        // A summary isn't cut at 360dp, and a title isn't cut at a large font.
        titleMaxLines = 2,
        subtitleMaxLines = if (large) 4 else 2,
        onClick = onClick,
        leading = leading,
        meta = if (large && (pill != null || chip != null)) ({
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (pill != null) StatusPill(pill.first, pill.second)
                chip?.invoke()
            }
        }) else null,
        trailing = {
            if (!large) {
                if (pill != null) StatusPill(pill.first, pill.second)
                chip?.invoke()
            }
            if (meta != null) SupportingText(meta, style = LocusType.mono)
            RowChevron()
        },
    )
}

/** From this font scale a hub row's pill moves under its text. */
private const val LARGE_FONT = 1.3f

@Composable
internal fun appVersion(): String? {
    val context = LocalContext.current
    return remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
}

// ---- Summaries (plain functions so they are easy to test) ----

/** What every hub summary shows until the settings have loaded. */
internal const val LOADING_SUMMARY = "Loading…"

/**
 * "OpenAI · default", "OpenAI · default · 3 providers", or "No provider yet". When a provider has no
 * key (or is signed out) it says which, since the row's stringResource(R.string.settings_no_key) pill sits beside the default's logo.
 */
internal fun modelsSummary(state: SettingsUiState): String {
    val drafts = state.providerDrafts.size
    if (drafts > 0) {
        val waiting = if (drafts == 1) "${state.providerDrafts[0].draft.name} to review" else "$drafts drafts to review"
        val default = state.providers.firstOrNull { it.isDefault }?.config
        return waiting + (default?.let { " · ${it.name} · default" } ?: "")
    }
    if (state.providers.isEmpty()) return if (state.loading) "" else "No provider yet"
    val default = state.providers.firstOrNull { it.isDefault }?.config
    val head = default?.let { "${it.name} · default" } ?: "No default"
    val missing = state.providers.firstOrNull { it.config.needsCredential && !it.hasKey }?.config
    return when {
        // First, so it's what the two lines keep: the pill beside it means this provider.
        missing != null -> "${missing.name} ${if (missing.signsIn) "is signed out" else "has no key"} · $head"
        state.providers.size > 1 -> "$head · ${state.providers.size} providers"
        else -> head
    }
}

internal fun extrasSummary(state: SettingsUiState): String {
    val helper = state.providers.firstOrNull { it.config.id == state.helperProviderId }?.config
    val helperText = helper?.let { "Helper: ${it.name}" } ?: if (state.helperAutomatic) "Helper: automatic" else "Helper: each chat's model"
    val on = listOf(state.autoTitle, state.suggestFollowUps, state.reflectAfterTasks).count { it }
    return "$helperText · $on of 3 extras"
}

internal fun agentSummary(state: SettingsUiState): String =
    if (state.customInstructions.isBlank()) "Identity, instructions and task budget" else "Custom instructions set"

internal fun permissionsSummary(tools: List<ToolRow>): String =
    if (tools.isEmpty()) "Which actions run without asking" else policySummary(tools.size, tools.groupingBy { it.policy }.eachCount())

internal fun reviewerSummary(mode: dev.locus.core.model.ReviewerMode): String =
    if (mode == dev.locus.core.model.ReviewerMode.OFF) "" else " · Reviewer on"

internal fun integrationsSummary(state: SettingsUiState): String {
    val connected = state.integrations.filter { it.second }.map { it.first.name }
    return when {
        connected.isEmpty() -> "Nothing connected yet"
        connected.size <= 2 -> "${connected.joinToString(", ")} connected"
        else -> "${connected.size} connected"
    }
}

internal fun sshSummary(state: SettingsUiState): String = when (val n = state.sshHosts.size) {
    0 -> "No machines yet"
    1 -> "1 machine · ${state.sshHosts.first().alias}"
    else -> "$n machines"
}
