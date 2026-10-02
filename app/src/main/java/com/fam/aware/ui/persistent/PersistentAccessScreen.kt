package com.fam.aware.ui.persistent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fam.aware.R
import com.fam.aware.data.model.AccessDuration
import com.fam.aware.data.model.PersistentAccessState
import com.fam.aware.data.model.PersistentAccessStatus
import com.fam.aware.data.model.sourceLabel
import com.fam.aware.ui.components.BulletItem
import com.fam.aware.ui.components.ParentGuideSheet
import com.fam.aware.ui.components.PermissionActions
import com.fam.aware.ui.components.SectionCard
import com.fam.aware.ui.components.StatusPill
import com.fam.aware.ui.components.UiStateContent
import com.fam.aware.ui.theme.Dimens
import com.fam.aware.util.ParentApprovalResult
import com.fam.aware.util.SystemIntents
import com.fam.aware.util.TimeFormatter
import com.fam.aware.viewmodel.PersistentAccessUiState
import com.fam.aware.viewmodel.PersistentAccessViewModel

/**
 * Экран «режима постоянного доступа».
 *
 * Режим включается только после решения родителя или администратора устройства,
 * всегда ограничен по сроку (если это не решение администратора) и всегда может
 * быть отключён. Приложение не назначает себя Device Owner, не использует
 * `DevicePolicyManager` и не мешает родителю отозвать разрешение или удалить его.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersistentAccessScreen(
    viewModelFactory: ViewModelProvider.Factory,
    onBack: () -> Unit,
) {
    val persistentAccessViewModel: PersistentAccessViewModel = viewModel(factory = viewModelFactory)
    val state by persistentAccessViewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    var parentGuideVisible by rememberSaveable { mutableStateOf(false) }

    // Родитель может изменить решение в любой момент — перечитываем при возврате в приложение.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) persistentAccessViewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Официальный системный broadcast об изменении ограничений приложения.
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                persistentAccessViewModel.refresh()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val approvalLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        persistentAccessViewModel.onApprovalResult(ParentApprovalResult.from(result.data))
    }

    val noticeRes = state.notice
    LaunchedEffect(noticeRes) {
        if (noticeRes != null) {
            snackbarHostState.showSnackbar(context.getString(noticeRes))
            persistentAccessViewModel.consumeNotice()
        }
    }

    BackHandler(onBack = onBack)

    val notificationSettingsAction: () -> Unit = {
        if (!SystemIntents.openNotificationSettings(context)) {
            SystemIntents.openAppDetailsSettings(context)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.pa_screen_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.pa_screen_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = persistentAccessViewModel::refresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.pa_action_refresh),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        UiStateContent(
            state = state.status,
            loadingText = stringResource(R.string.state_loading),
            onRetry = persistentAccessViewModel::refresh,
            permissionActions = PermissionActions(
                onRequest = { notificationSettingsAction() },
                onOpenAppSettings = { SystemIntents.openAppDetailsSettings(context) },
                onOpenNotificationSettings = notificationSettingsAction,
                onShowParentGuide = { parentGuideVisible = true },
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { status ->
            PersistentAccessContent(
                state = state,
                status = status,
                onRequestParentApproval = {
                    val intent = persistentAccessViewModel.createParentApprovalIntent()
                    if (intent == null) {
                        parentGuideVisible = true
                    } else {
                        persistentAccessViewModel.markApprovalRequested()
                        approvalLauncher.launch(intent)
                    }
                },
                onCancelRequest = persistentAccessViewModel::cancelApprovalRequest,
                onShowLocalConfirmDialog = persistentAccessViewModel::showLocalConfirmDialog,
                onRevoke = persistentAccessViewModel::revoke,
                onRefresh = persistentAccessViewModel::refresh,
                onSelectDuration = persistentAccessViewModel::selectDuration,
                onOpenNotificationSettings = notificationSettingsAction,
                onShowParentGuide = { parentGuideVisible = true },
            )
        }
    }

    if (state.localConfirmDialogVisible) {
        AlertDialog(
            onDismissRequest = persistentAccessViewModel::dismissLocalConfirmDialog,
            icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            title = { Text(stringResource(R.string.pa_local_dialog_title)) },
            text = { Text(stringResource(R.string.pa_local_dialog_body)) },
            confirmButton = {
                TextButton(onClick = persistentAccessViewModel::confirmLocally) {
                    Text(stringResource(R.string.pa_local_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = persistentAccessViewModel::dismissLocalConfirmDialog) {
                    Text(stringResource(R.string.pa_local_dialog_cancel))
                }
            },
        )
    }

    if (parentGuideVisible) {
        ParentGuideSheet(
            onDismiss = { parentGuideVisible = false },
            onOpenHelp = { SystemIntents.openParentHelp(context) },
            onOpenAppSettings = { SystemIntents.openAppDetailsSettings(context) },
        )
    }
}

@Composable
private fun PersistentAccessContent(
    state: PersistentAccessUiState,
    status: PersistentAccessStatus,
    onRequestParentApproval: () -> Unit,
    onCancelRequest: () -> Unit,
    onShowLocalConfirmDialog: () -> Unit,
    onRevoke: () -> Unit,
    onRefresh: () -> Unit,
    onSelectDuration: (AccessDuration) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onShowParentGuide: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.ScreenPadding,
            end = Dimens.ScreenPadding,
            top = Dimens.ItemGap,
            bottom = Dimens.SectionGap,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
    ) {
        item { StatusCard(status = status) }

        item {
            ControlCard(
                state = state,
                status = status,
                onRequestParentApproval = onRequestParentApproval,
                onCancelRequest = onCancelRequest,
                onShowLocalConfirmDialog = onShowLocalConfirmDialog,
                onRevoke = onRevoke,
                onRefresh = onRefresh,
                onSelectDuration = onSelectDuration,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onShowParentGuide = onShowParentGuide,
            )
        }

        item { HowParentCard(onShowParentGuide = onShowParentGuide) }

        item { LimitsCard() }
    }
}

@Composable
private fun StatusCard(status: PersistentAccessStatus) {
    val accent = when (status.state) {
        PersistentAccessState.ACTIVE -> MaterialTheme.colorScheme.primary
        PersistentAccessState.REVOKED_BY_ADMIN,
        PersistentAccessState.UNAVAILABLE_NO_NOTIFICATIONS,
        -> MaterialTheme.colorScheme.error

        PersistentAccessState.PENDING_APPROVAL -> MaterialTheme.colorScheme.tertiary
        PersistentAccessState.EXPIRED -> MaterialTheme.colorScheme.secondary
        PersistentAccessState.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    SectionCard(
        title = stringResource(statusTitle(status.state)),
        icon = statusIcon(status.state),
        accent = accent,
        trailing = {
            StatusPill(
                text = stringResource(statusTitle(status.state)),
                containerColor = accent.copy(alpha = 0.16f),
                contentColor = accent,
            )
        },
    ) {
        Text(
            text = stringResource(statusBody(status.state)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (status.isActive) {
            Spacer(Modifier.height(Dimens.ItemGap))
            DetailRow(
                text = if (status.expiresAtMillis != null) {
                    stringResource(R.string.pa_active_until, TimeFormatter.format(status.expiresAtMillis))
                } else {
                    stringResource(R.string.pa_active_by_admin)
                },
            )
            DetailRow(
                text = stringResource(
                    R.string.pa_active_source,
                    stringResource(sourceLabel(status.source)),
                ),
            )
        }

        Spacer(Modifier.height(Dimens.ItemGap))
        DetailRow(
            text = stringResource(
                when {
                    !status.adminRestrictionSet -> R.string.pa_restriction_not_set
                    status.isForbiddenByAdmin -> R.string.pa_restriction_set_false
                    else -> R.string.pa_restriction_set_true
                },
            ),
        )

        Spacer(Modifier.height(Dimens.ItemGap))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(Dimens.CardPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.IconSize),
                )
                Text(
                    text = stringResource(R.string.pa_honesty_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ControlCard(
    state: PersistentAccessUiState,
    status: PersistentAccessStatus,
    onRequestParentApproval: () -> Unit,
    onCancelRequest: () -> Unit,
    onShowLocalConfirmDialog: () -> Unit,
    onRevoke: () -> Unit,
    onRefresh: () -> Unit,
    onSelectDuration: (AccessDuration) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onShowParentGuide: () -> Unit,
) {
    SectionCard(
        title = stringResource(R.string.pa_section_control),
        icon = Icons.Filled.Settings,
        accent = MaterialTheme.colorScheme.tertiary,
    ) {
        val canChooseDuration = status.canActivateLocally || status.canRequestParentApproval
        if (canChooseDuration) {
            Text(
                text = stringResource(R.string.pa_duration_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.durations.forEach { duration ->
                    FilterChip(
                        selected = duration == state.selectedDuration,
                        onClick = { onSelectDuration(duration) },
                        label = { Text(stringResource(duration.labelRes)) },
                    )
                }
            }
            Spacer(Modifier.height(Dimens.SectionGap))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (status.state) {
                PersistentAccessState.ACTIVE -> {
                    Button(onClick = onRevoke, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.pa_action_revoke))
                    }
                    OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.pa_action_refresh))
                    }
                }

                PersistentAccessState.PENDING_APPROVAL -> {
                    Button(onClick = onRequestParentApproval, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.pa_action_request_parent))
                    }
                    OutlinedButton(onClick = onCancelRequest, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.pa_action_cancel_request))
                    }
                }

                PersistentAccessState.REVOKED_BY_ADMIN -> {
                    Button(onClick = onShowParentGuide, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_parent_guide))
                    }
                    OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.pa_action_refresh))
                    }
                }

                PersistentAccessState.UNAVAILABLE_NO_NOTIFICATIONS -> {
                    Button(onClick = onOpenNotificationSettings, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_open_notification_settings))
                    }
                }

                PersistentAccessState.DISABLED,
                PersistentAccessState.EXPIRED,
                -> {
                    if (status.canRequestParentApproval) {
                        Button(onClick = onRequestParentApproval, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.pa_action_request_parent))
                        }
                    }
                    if (status.canActivateLocally) {
                        OutlinedButton(
                            onClick = onShowLocalConfirmDialog,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.pa_action_confirm_locally))
                        }
                    }
                    if (!status.approvalRequestAvailable) {
                        Text(
                            text = stringResource(R.string.pa_notice_approval_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.pa_action_refresh))
                    }
                }
            }
        }
    }
}

@Composable
private fun HowParentCard(onShowParentGuide: () -> Unit) {
    SectionCard(
        title = stringResource(R.string.pa_how_parent_title),
        icon = Icons.Filled.Lock,
        accent = MaterialTheme.colorScheme.tertiary,
    ) {
        GuideBlock(title = stringResource(R.string.pa_how_parent_admin_title)) {
            GuideBlockText(stringResource(R.string.pa_how_parent_admin_body))
        }
        Spacer(Modifier.height(Dimens.ItemGap))
        GuideBlock(title = stringResource(R.string.pa_how_parent_approval_title)) {
            GuideBlockText(stringResource(R.string.pa_how_parent_approval_body))
        }
        Spacer(Modifier.height(Dimens.ItemGap))
        GuideBlock(title = stringResource(R.string.pa_how_parent_family_link_title)) {
            GuideBlockText(stringResource(R.string.pa_how_parent_family_link_step_1))
            GuideBlockText(stringResource(R.string.pa_how_parent_family_link_step_2))
            GuideBlockText(stringResource(R.string.pa_how_parent_family_link_step_3))
            GuideBlockText(stringResource(R.string.pa_how_parent_family_link_step_4))
        }
        Spacer(Modifier.height(Dimens.ItemGap))
        OutlinedButton(onClick = onShowParentGuide, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_parent_guide))
        }
    }
}

@Composable
private fun LimitsCard() {
    SectionCard(
        title = stringResource(R.string.pa_limits_title),
        icon = Icons.Filled.CheckCircle,
        accent = MaterialTheme.colorScheme.secondary,
    ) {
        listOf(
            R.string.pa_limits_1,
            R.string.pa_limits_2,
            R.string.pa_limits_3,
            R.string.pa_limits_4,
            R.string.pa_limits_5,
            R.string.pa_limits_6,
        ).forEach { itemRes ->
            BulletItem(
                text = stringResource(itemRes),
                marker = Icons.Filled.CheckCircle,
                markerTint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun GuideBlock(title: String, content: @Composable () -> Unit) {
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
private fun GuideBlockText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DetailRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

@StringRes
private fun statusTitle(state: PersistentAccessState): Int = when (state) {
    PersistentAccessState.DISABLED -> R.string.pa_state_disabled
    PersistentAccessState.PENDING_APPROVAL -> R.string.pa_state_pending
    PersistentAccessState.ACTIVE -> R.string.pa_state_active
    PersistentAccessState.EXPIRED -> R.string.pa_state_expired
    PersistentAccessState.REVOKED_BY_ADMIN -> R.string.pa_state_revoked_by_admin
    PersistentAccessState.UNAVAILABLE_NO_NOTIFICATIONS -> R.string.pa_state_unavailable_notifications
}

@StringRes
private fun statusBody(state: PersistentAccessState): Int = when (state) {
    PersistentAccessState.DISABLED -> R.string.pa_state_disabled_body
    PersistentAccessState.PENDING_APPROVAL -> R.string.pa_state_pending_body
    PersistentAccessState.ACTIVE -> R.string.pa_state_active_body
    PersistentAccessState.EXPIRED -> R.string.pa_state_expired_body
    PersistentAccessState.REVOKED_BY_ADMIN -> R.string.pa_state_revoked_by_admin_body
    PersistentAccessState.UNAVAILABLE_NO_NOTIFICATIONS -> R.string.pa_state_unavailable_notifications_body
}

private fun statusIcon(state: PersistentAccessState): ImageVector = when (state) {
    PersistentAccessState.ACTIVE -> Icons.Filled.CheckCircle
    PersistentAccessState.PENDING_APPROVAL -> Icons.Filled.Info
    PersistentAccessState.DISABLED -> Icons.Filled.Lock
    PersistentAccessState.EXPIRED -> Icons.Filled.Refresh
    PersistentAccessState.REVOKED_BY_ADMIN -> Icons.Filled.Lock
    PersistentAccessState.UNAVAILABLE_NO_NOTIFICATIONS -> Icons.Filled.Notifications
}
