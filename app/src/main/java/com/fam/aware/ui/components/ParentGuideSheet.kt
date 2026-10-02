package com.fam.aware.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fam.aware.R
import com.fam.aware.ui.theme.Dimens

/**
 * Нижняя шторка с официальными способами снять ограничение.
 *
 * Приложение не меняет настройки само — оно показывает родителю, где именно
 * в Family Link или в системных настройках нужно разрешить функцию.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentGuideSheet(
    onDismiss: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = Dimens.ScreenPadding)
                .padding(bottom = Dimens.SectionGap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(Dimens.IconSize),
                )
                Spacer(Modifier.size(Dimens.ItemGap))
                Text(
                    text = stringResource(R.string.parent_guide_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(Dimens.ItemGap))
            Text(
                text = stringResource(R.string.parent_guide_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Dimens.SectionGap))

            GuideBlock(title = stringResource(R.string.parent_guide_family_link_title)) {
                GuideStep(R.string.parent_guide_family_link_step_1)
                GuideStep(R.string.parent_guide_family_link_step_2)
                GuideStep(R.string.parent_guide_family_link_step_3)
                GuideStep(R.string.parent_guide_family_link_step_4)
                GuideStep(R.string.parent_guide_family_link_step_5)
                GuideStep(R.string.parent_guide_family_link_step_6)
            }

            Spacer(Modifier.height(Dimens.ItemGap))

            GuideBlock(title = stringResource(R.string.parent_guide_device_title)) {
                GuideStep(R.string.parent_guide_device_step_1)
                GuideStep(R.string.parent_guide_device_step_2)
                GuideStep(R.string.parent_guide_device_step_3)
            }

            Spacer(Modifier.height(Dimens.ItemGap))

            GuideBlock(title = stringResource(R.string.parent_guide_mdm_title)) {
                Text(
                    text = stringResource(R.string.parent_guide_mdm_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(Dimens.SectionGap))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onOpenHelp, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.parent_guide_help_link))
                }
                OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.action_open_app_settings))
                }
            }
        }
    }
}

@Composable
private fun GuideBlock(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}

@Composable
private fun GuideStep(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
