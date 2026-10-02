package com.fam.aware.ui.components

import androidx.annotation.StringRes
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fam.aware.R
import com.fam.aware.data.model.PermissionReport
import com.fam.aware.data.model.PermissionState
import com.fam.aware.ui.theme.Dimens

@StringRes
fun permissionStateLabel(state: PermissionState): Int = when (state) {
    PermissionState.GRANTED -> R.string.permission_state_granted
    PermissionState.NOT_REQUESTED -> R.string.permission_state_not_requested
    PermissionState.DENIED -> R.string.permission_state_denied
    PermissionState.DENIED_PERMANENTLY -> R.string.permission_state_denied_permanently
    PermissionState.BLOCKED_BY_POLICY -> R.string.permission_state_blocked_by_policy
    PermissionState.NOT_APPLICABLE -> R.string.permission_state_not_supported
}

@Composable
fun permissionStateColor(report: PermissionReport): Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        report.isBlockedBySupervision -> scheme.tertiary
        report.isUsable -> scheme.primary
        report.state == PermissionState.NOT_REQUESTED -> scheme.onSurfaceVariant
        report.state == PermissionState.NOT_APPLICABLE -> scheme.secondary
        else -> scheme.error
    }
}

@Composable
fun permissionStateIcon(report: PermissionReport): ImageVector = when {
    report.isBlockedBySupervision -> Icons.Filled.Lock
    report.isUsable -> Icons.Filled.CheckCircle
    report.state == PermissionState.NOT_REQUESTED -> Icons.Filled.Notifications
    else -> Icons.Filled.Warning
}

/**
 * Карточка одного разрешения: что это, зачем нужно, текущее состояние
 * и какие действия доступны пользователю прямо сейчас.
 */
@Composable
fun PermissionCard(
    report: PermissionReport,
    actions: PermissionActions,
    modifier: Modifier = Modifier,
) {
    val permission = report.permission
    val accent = permissionStateColor(report)

    SectionCard(
        modifier = modifier,
        title = stringResource(R.string.perm_notifications_title),
        subtitle = stringResource(permissionStateLabel(report.state)),
        icon = permissionStateIcon(report),
        accent = accent,
    ) {
        Text(
            text = stringResource(permission.technicalNameRes),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )

        StatusPill(
            text = stringResource(permissionStateLabel(report.state)),
            containerColor = accent.copy(alpha = 0.16f),
            contentColor = accent,
            icon = permissionStateIcon(report),
        )

        Spacer(Modifier.height(Dimens.ItemGap))

        Text(
            text = stringResource(permission.reasonRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val explanationRes: Int? = when {
            report.isBlockedBySupervision -> R.string.permission_blocked_explanation
            report.state == PermissionState.DENIED_PERMANENTLY ->
                R.string.permission_denied_permanently_explanation

            report.state == PermissionState.DENIED -> R.string.permission_denied_explanation
            report.state == PermissionState.GRANTED && !report.featureEnabledInSystem ->
                R.string.permission_channel_disabled_explanation

            else -> null
        }
        if (explanationRes != null) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(explanationRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }

        Spacer(Modifier.height(Dimens.ItemGap))

        Text(
            text = stringResource(permission.ifDeniedRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Dimens.SectionGap))

        PermissionActionRow(report = report, actions = actions)
    }
}

/**
 * Кнопки под состоянием разрешения.
 *
 * Ключевое правило: запрос разрешения возможен только из [PermissionState.NOT_REQUESTED]
 * и [PermissionState.DENIED] и только по явному нажатию. При блокировке политикой
 * или при выборе «больше не спрашивать» приложение предлагает системные настройки
 * и инструкцию для родителя, но не системный диалог.
 */
@Composable
private fun PermissionActionRow(
    report: PermissionReport,
    actions: PermissionActions,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (report.state) {
            PermissionState.NOT_REQUESTED -> Button(
                onClick = actions.onRequest,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.action_request_permission)) }

            PermissionState.DENIED -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = actions.onRequest,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_ask_again)) }
                OutlinedButton(
                    onClick = actions.onShowParentGuide,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_parent_guide)) }
            }

            PermissionState.DENIED_PERMANENTLY -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = actions.onOpenNotificationSettings,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_open_notification_settings)) }
                OutlinedButton(
                    onClick = actions.onOpenAppSettings,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_open_app_settings)) }
            }

            PermissionState.BLOCKED_BY_POLICY -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = actions.onShowParentGuide,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_parent_guide)) }
                OutlinedButton(
                    onClick = actions.onOpenAppSettings,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_open_app_settings)) }
            }

            PermissionState.GRANTED,
            PermissionState.NOT_APPLICABLE,
            -> Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                OutlinedButton(
                    onClick = actions.onOpenNotificationSettings,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_open_notification_settings)) }
            }
        }
    }
}
