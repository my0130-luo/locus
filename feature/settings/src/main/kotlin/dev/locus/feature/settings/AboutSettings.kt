package dev.locus.feature.settings

import dev.locus.feature.settings.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.locus.core.designsystem.destinationPath
import dev.locus.core.designsystem.DestinationLink
import dev.locus.core.designsystem.CardGroup
import dev.locus.core.designsystem.GroupDivider
import dev.locus.core.designsystem.IconTile
import dev.locus.core.designsystem.ListRow
import dev.locus.core.designsystem.LocusIcons
import dev.locus.core.designsystem.LocusType
import dev.locus.core.designsystem.SupportingText
import dev.locus.core.designsystem.SectionLabel
import dev.locus.core.designsystem.SectionNote
import dev.locus.core.designsystem.Insets
import dev.locus.core.designsystem.RowChevron

/** Version, updates, a plain account of what leaves the phone, Diagnostics, and a way back into setup. */
@Composable
internal fun AboutSettings(
    onBack: () -> Unit,
    onRunSetup: () -> Unit,
    onOpenDiagnostics: () -> Unit = {},
    onOpenActivity: () -> Unit = {},
    updatesViewModel: UpdatesViewModel = hiltViewModel(),
) {
    val version = appVersion()
    val updates by updatesViewModel.uiState.collectAsStateWithLifecycle()
    val updateActions = remember(updatesViewModel) { updatesViewModel.actions() }
    UpdatesResumeEffect(updatesViewModel)
    SettingsPageScaffold(stringResource(R.string.about_title), onBack) {
        // First, so stringResource(R.string.about_update_available) (and the notification that opens this page) is in view.
        updatesSection(updates, updateActions)

        item { SectionLabel(stringResource(R.string.about_data_title), SectionPadding) }
        item {
            CardGroup {
                PrivacyRow(stringResource(R.string.about_messages), stringResource(R.string.about_messages_body), LocusIcons.Chats)
                GroupDivider(inset = Insets.iconRow)
                PrivacyRow(stringResource(R.string.about_keys), stringResource(R.string.about_keys_body), LocusIcons.Key)
                GroupDivider(inset = Insets.iconRow)
                PrivacyRow(stringResource(R.string.about_connections), stringResource(R.string.about_connections_body), LocusIcons.Globe)
                GroupDivider(inset = Insets.iconRow)
                PrivacyRow(stringResource(R.string.about_local_data), stringResource(R.string.about_local_data_body), LocusIcons.Folder)
                // Only in builds that have the check (docs/design/updates.md).
                if (updates.info?.availability?.canCheck == true) {
                    GroupDivider(inset = Insets.iconRow)
                    PrivacyRow(
                        stringResource(R.string.about_update_check),
                        stringResource(R.string.about_update_check_body),
                        LocusIcons.Clock,
                    )
                }
            }
        }
        item { DestinationLink(destinationPath(stringResource(R.string.about_activity)), onNavigate = onOpenActivity, modifier = NotePadding, hint = "Every action Locus took is in") }

        item { SectionLabel(stringResource(R.string.about_reliability), SectionPadding) }
        item {
            CardGroup {
                ListRow(
                    title = stringResource(R.string.about_diagnostics),
                    subtitle = stringResource(R.string.about_diagnostics_body),
                    subtitleMaxLines = 2,
                    onClick = onOpenDiagnostics,
                    leading = { IconTile(LocusIcons.Tool) },
                    trailing = { RowChevron() },
                )
            }
        }

        item { SectionLabel(stringResource(R.string.about_setup), SectionPadding) }
        item {
            CardGroup {
                ListRow(
                    title = stringResource(R.string.about_run_setup),
                    subtitle = stringResource(R.string.about_run_setup_body),
                    onClick = onRunSetup,
                    leading = { IconTile(LocusIcons.Play) },
                    trailing = { RowChevron() },
                )
            }
        }

        item { SectionLabel(stringResource(R.string.about_about), SectionPadding) }
        item {
            CardGroup {
                ListRow(
                    title = "Locus",
                    subtitle = stringResource(R.string.about_tagline),
                    leading = { IconTile(LocusIcons.Info) },
                    trailing = { if (version != null) SupportingText(version, style = LocusType.mono) },
                )
            }
        }
    }
}

@Composable
private fun PrivacyRow(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    // A privacy promise is read in full: it wraps instead of being cut.
    ListRow(title = title, subtitle = body, titleMaxLines = 2, subtitleMaxLines = Int.MAX_VALUE, leading = { IconTile(icon) })
}
