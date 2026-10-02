package com.fam.aware.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fam.aware.R
import com.fam.aware.data.model.SignalKind
import com.fam.aware.data.model.SupervisionReport
import com.fam.aware.data.model.SupervisionSignal
import com.fam.aware.ui.theme.Dimens

/**
 * Карточка результатов проверки родительского контроля.
 *
 * Показывает только то, что удалось прочитать официальными API.
 * Приложение ничего не отключает и не предлагает способы обхода —
 * рядом всегда есть кнопка с инструкцией для родителя (см. [ParentGuideSheet]).
 */
@Composable
fun SupervisionCard(
    report: SupervisionReport,
    modifier: Modifier = Modifier,
) {
    val supervised = report.isSupervised
    val accent = if (supervised) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }

    SectionCard(
        modifier = modifier,
        title = stringResource(
            if (supervised) R.string.supervision_detected_title else R.string.supervision_clean_title,
        ),
        icon = if (supervised) Icons.Filled.Lock else Icons.Filled.CheckCircle,
        accent = accent,
        trailing = {
            StatusPill(
                text = stringResource(
                    if (supervised) {
                        R.string.permission_state_blocked_by_policy
                    } else {
                        R.string.permission_state_granted
                    },
                ),
                containerColor = accent.copy(alpha = 0.16f),
                contentColor = accent,
            )
        },
    ) {
        Text(
            text = stringResource(
                if (supervised) R.string.supervision_detected_body else R.string.supervision_clean_body,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AnimatedVisibility(visible = report.restrictions.isNotEmpty()) {
            Column(modifier = Modifier.animateContentSize()) {
                Spacer(Modifier.height(Dimens.ItemGap))
                report.restrictions.forEach { signal ->
                    SignalRow(signal = signal, accent = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        AnimatedVisibility(visible = report.info.isNotEmpty()) {
            Column(modifier = Modifier.animateContentSize()) {
                Spacer(Modifier.height(Dimens.ItemGap))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.home_section_status),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        report.info.forEach { signal ->
                            SignalRow(signal = signal, accent = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalRow(
    signal: SupervisionSignal,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = if (signal.kind == SignalKind.RESTRICTION) {
                Icons.Filled.Warning
            } else {
                Icons.Filled.Info
            },
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(signal.titleRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val detailRes = signal.detailRes
            val detailArg = signal.detailArg
            if (detailRes != null) {
                val detailText = if (detailArg != null) {
                    stringResource(detailRes, detailArg)
                } else {
                    stringResource(detailRes)
                }
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!signal.detail.isNullOrBlank()) {
                Text(
                    text = signal.detail,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
