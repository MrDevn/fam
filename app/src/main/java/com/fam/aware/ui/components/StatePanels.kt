package com.fam.aware.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fam.aware.R
import com.fam.aware.data.model.PermissionReport
import com.fam.aware.data.model.PermissionState
import com.fam.aware.data.model.UiState
import com.fam.aware.ui.theme.Dimens
import com.fam.aware.ui.theme.Motion

/** Набор действий, которые UI может предложить при отсутствии разрешения. */
data class PermissionActions(
    /** Явно запросить разрешение. Вызывается только по нажатию пользователя. */
    val onRequest: () -> Unit,
    val onOpenAppSettings: () -> Unit,
    val onOpenNotificationSettings: () -> Unit,
    val onShowParentGuide: () -> Unit,
)

/**
 * Универсальный рендер четырёх состояний экрана:
 * Loading, Success, Error и Permission denied.
 */
@Composable
fun <T> UiStateContent(
    state: UiState<T>,
    loadingText: String,
    onRetry: () -> Unit,
    permissionActions: PermissionActions,
    modifier: Modifier = Modifier,
    idle: (@Composable () -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = state,
        modifier = modifier,
        transitionSpec = {
            (
                fadeIn(tween(Motion.DurationMedium)) +
                    scaleIn(tween(Motion.DurationMedium), initialScale = 0.98f)
                ) togetherWith (
                    fadeOut(tween(Motion.DurationShort)) +
                        scaleOut(tween(Motion.DurationShort), targetScale = 0.98f)
                    )
        },
        label = "ui-state",
    ) { current ->
        when (current) {
            is UiState.Idle -> {
                if (idle != null) idle()
            }

            is UiState.Loading -> LoadingPanel(loadingText)

            is UiState.Error -> ErrorPanel(
                titleRes = current.messageRes,
                detail = current.detail,
                onRetry = onRetry,
            )

            is UiState.PermissionDenied -> PermissionDeniedPanel(
                report = current.report,
                actions = permissionActions,
            )

            is UiState.Success -> content(current.data)
        }
    }
}

/** Состояние Loading. */
@Composable
fun LoadingPanel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "loading")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loading-pulse",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .alpha(pulse),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Состояние Error. */
@Composable
fun ErrorPanel(
    @StringRes titleRes: Int,
    detail: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatePanel(
        modifier = modifier,
        icon = Icons.Filled.Warning,
        accent = MaterialTheme.colorScheme.error,
        title = stringResource(titleRes),
        body = stringResource(R.string.state_error_hint),
    ) {
        if (!detail.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(Dimens.ItemGap))
        }
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.state_retry))
        }
    }
}

/**
 * Состояние Permission denied.
 *
 * Кнопки подбираются под конкретную причину отказа. В состояниях
 * [PermissionState.BLOCKED_BY_POLICY] и [PermissionState.DENIED_PERMANENTLY]
 * кнопки «запросить ещё раз» нет вообще: приложение не давит на пользователя
 * и не пытается обойти решение системы или родителя.
 */
@Composable
fun PermissionDeniedPanel(
    report: PermissionReport,
    actions: PermissionActions,
    modifier: Modifier = Modifier,
) {
    val explanationRes = when {
        report.isBlockedBySupervision -> R.string.permission_blocked_explanation
        report.state == PermissionState.DENIED_PERMANENTLY ->
            R.string.permission_denied_permanently_explanation

        report.state == PermissionState.GRANTED || report.state == PermissionState.NOT_APPLICABLE ->
            R.string.permission_channel_disabled_explanation

        else -> R.string.permission_denied_explanation
    }

    StatePanel(
        modifier = modifier,
        icon = if (report.isBlockedBySupervision) Icons.Filled.Lock else Icons.Filled.Notifications,
        accent = if (report.isBlockedBySupervision) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.error
        },
        title = stringResource(R.string.state_permission_denied_title),
        body = stringResource(explanationRes),
    ) {
        val buttons = buildPermissionButtons(report, actions)

        buttons.forEachIndexed { index, button ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            when (button) {
                is PermissionButton.Primary -> Button(
                    onClick = button.onClick,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(button.labelRes)) }

                is PermissionButton.Secondary -> OutlinedButton(
                    onClick = button.onClick,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(button.labelRes)) }
            }
        }
    }
}

/**
 * Подбор доступных действий под состояние разрешения.
 *
 * Правило из требований: если разрешение заблокировано политикой или отклонено
 * навсегда, повторный системный запрос не предлагается вообще.
 */
private fun buildPermissionButtons(
    report: PermissionReport,
    actions: PermissionActions,
): List<PermissionButton> = buildList {
    when (report.state) {
        PermissionState.NOT_REQUESTED ->
            add(PermissionButton.Primary(R.string.action_request_permission, actions.onRequest))

        PermissionState.DENIED ->
            add(PermissionButton.Primary(R.string.action_ask_again, actions.onRequest))

        PermissionState.DENIED_PERMANENTLY ->
            add(
                PermissionButton.Primary(
                    R.string.action_open_app_settings,
                    actions.onOpenAppSettings,
                ),
            )

        PermissionState.BLOCKED_BY_POLICY -> {
            add(PermissionButton.Primary(R.string.action_parent_guide, actions.onShowParentGuide))
            add(
                PermissionButton.Secondary(
                    R.string.action_open_app_settings,
                    actions.onOpenAppSettings,
                ),
            )
        }

        PermissionState.GRANTED,
        PermissionState.NOT_APPLICABLE,
        -> add(
            PermissionButton.Primary(
                R.string.action_open_notification_settings,
                actions.onOpenNotificationSettings,
            ),
        )
    }

    if (report.isBlockedBySupervision && report.state != PermissionState.BLOCKED_BY_POLICY) {
        add(PermissionButton.Secondary(R.string.action_parent_guide, actions.onShowParentGuide))
    }
}

private sealed interface PermissionButton {
    @get:StringRes
    val labelRes: Int
    val onClick: () -> Unit

    data class Primary(
        @StringRes override val labelRes: Int,
        override val onClick: () -> Unit,
    ) : PermissionButton

    data class Secondary(
        @StringRes override val labelRes: Int,
        override val onClick: () -> Unit,
    ) : PermissionButton
}

/** Общий каркас панели состояния: иконка, заголовок, текст, действия. */
@Composable
private fun StatePanel(
    icon: ImageVector,
    accent: Color,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = accent.copy(alpha = 0.14f),
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.SectionGap))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = actions,
        )
    }
}

/** Панель успешного завершения проверки. */
@Composable
fun SuccessBanner(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    @StringRes actionLabelRes: Int? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
    ) {
        Row(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.IconSize),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (actionLabelRes != null && onAction != null) {
                FilledTonalButton(onClick = onAction) {
                    Text(stringResource(actionLabelRes))
                }
            }
        }
    }
}
