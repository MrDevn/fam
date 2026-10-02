package com.fam.aware.ui.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fam.aware.R
import com.fam.aware.data.model.AppPermission
import com.fam.aware.data.model.DeviceSnapshot
import com.fam.aware.data.model.UiState
import com.fam.aware.ui.components.ParentGuideSheet
import com.fam.aware.ui.components.PermissionActions
import com.fam.aware.ui.components.PermissionCard
import com.fam.aware.ui.components.PermissionDeniedPanel
import com.fam.aware.ui.components.SectionCard
import com.fam.aware.ui.components.StatusPill
import com.fam.aware.ui.components.SuccessBanner
import com.fam.aware.ui.components.SupervisionCard
import com.fam.aware.ui.components.TransparencyCard
import com.fam.aware.ui.components.UiStateContent
import com.fam.aware.ui.theme.Dimens
import com.fam.aware.ui.theme.LocalDynamicColorSupported
import com.fam.aware.util.SystemIntents
import com.fam.aware.util.TimeFormatter
import com.fam.aware.util.findActivity
import com.fam.aware.viewmodel.AppearanceViewModel
import com.fam.aware.viewmodel.HomeViewModel

/**
 * Главный экран: диагностика устройства, состояние разрешений,
 * проверка функции уведомлений и раздел прозрачности.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModelFactory: ViewModelProvider.Factory,
    appearanceViewModel: AppearanceViewModel,
    onOpenSetup: () -> Unit,
) {
    val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    val state by homeViewModel.uiState.collectAsStateWithLifecycle()
    val dynamicColor by appearanceViewModel.dynamicColor.collectAsStateWithLifecycle(false)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var parentGuideVisible by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) homeViewModel.onResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        val rationaleVisible = context.findActivity()
            ?.shouldShowRequestPermissionRationale(AppPermission.NOTIFICATIONS.manifestName)
            ?: false
        homeViewModel.onPermissionResult(rationaleVisible)
    }

    val permissionActions = remember(context, permissionLauncher, homeViewModel) {
        PermissionActions(
            onRequest = {
                // Единственный путь к системному диалогу — явное нажатие пользователя.
                homeViewModel.onPermissionRequestStarted()
                permissionLauncher.launch(AppPermission.NOTIFICATIONS.manifestName)
            },
            onOpenAppSettings = { SystemIntents.openAppDetailsSettings(context) },
            onOpenNotificationSettings = {
                if (!SystemIntents.openNotificationSettings(context)) {
                    SystemIntents.openAppDetailsSettings(context)
                }
            },
            onShowParentGuide = { parentGuideVisible = true },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = homeViewModel::runCheck) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenSetup) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = stringResource(R.string.onboarding_intro_title),
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
            state = state.check,
            loadingText = stringResource(R.string.state_loading),
            onRetry = homeViewModel::runCheck,
            permissionActions = permissionActions,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { snapshot ->
            HomeContent(
                snapshot = snapshot,
                delivery = state.delivery,
                dynamicColor = dynamicColor,
                permissionActions = permissionActions,
                onRunCheck = homeViewModel::runCheck,
                onSendTestNotification = homeViewModel::sendTestNotification,
                onDynamicColorChange = appearanceViewModel::setDynamicColorEnabled,
            )
        }
    }

    if (parentGuideVisible) {
        ParentGuideSheet(
            onDismiss = { parentGuideVisible = false },
            onOpenHelp = {
                if (!SystemIntents.openParentHelp(context)) {
                    Toast.makeText(
                        context,
                        R.string.parent_guide_help_unavailable,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            onOpenAppSettings = { SystemIntents.openAppDetailsSettings(context) },
        )
    }
}

@Composable
private fun HomeContent(
    snapshot: DeviceSnapshot,
    delivery: UiState<Unit>,
    dynamicColor: Boolean,
    permissionActions: PermissionActions,
    onRunCheck: () -> Unit,
    onSendTestNotification: () -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 360.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.ScreenPadding,
            end = Dimens.ScreenPadding,
            top = Dimens.ItemGap,
            bottom = Dimens.SectionGap,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SuccessBanner(
                title = stringResource(R.string.home_check_result_title),
                body = stringResource(
                    R.string.home_last_checked,
                    TimeFormatter.format(snapshot.checkedAtMillis),
                ),
                actionLabelRes = R.string.home_rerun,
                onAction = onRunCheck,
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
                Text(
                    text = stringResource(R.string.home_section_status),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SupervisionCard(report = snapshot.supervision)
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
                Text(
                    text = stringResource(R.string.home_section_permissions),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PermissionCard(report = snapshot.permission, actions = permissionActions)
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
                Text(
                    text = stringResource(R.string.home_section_actions),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NotificationTestCard(
                    delivery = delivery,
                    canSend = snapshot.permission.isUsable,
                    actions = permissionActions,
                    onSend = onSendTestNotification,
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            AppearanceCard(
                dynamicColor = dynamicColor,
                onDynamicColorChange = onDynamicColorChange,
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            TransparencyCard()
        }
    }
}

/**
 * Карточка проверки функции уведомлений.
 *
 * Показывает все состояния: Idle, Loading, Success, Error и Permission denied.
 */
@Composable
private fun NotificationTestCard(
    delivery: UiState<Unit>,
    canSend: Boolean,
    actions: PermissionActions,
    onSend: () -> Unit,
) {
    SectionCard(
        title = stringResource(R.string.home_action_notifications_title),
        subtitle = stringResource(R.string.home_action_notifications_body),
        icon = Icons.Filled.Notifications,
        accent = MaterialTheme.colorScheme.tertiary,
    ) {
        Button(
            onClick = onSend,
            enabled = canSend && !delivery.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.home_action_send))
        }

        Spacer(Modifier.height(Dimens.ItemGap))

        when (delivery) {
            is UiState.Idle -> DeliveryHint(stringResource(R.string.delivery_state_idle))

            is UiState.Loading -> Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Text(
                    text = stringResource(R.string.delivery_state_sending),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is UiState.Success -> StatusPill(
                text = stringResource(R.string.delivery_state_sent),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Filled.CheckCircle,
            )

            is UiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(
                    text = stringResource(R.string.delivery_state_failed, delivery.detail.orEmpty()),
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                    contentColor = MaterialTheme.colorScheme.error,
                )
                DeliveryHint(stringResource(R.string.state_error_hint))
            }

            is UiState.PermissionDenied -> Column {
                StatusPill(
                    text = stringResource(R.string.delivery_state_denied),
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                    contentColor = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(Dimens.ItemGap))
                PermissionDeniedPanel(report = delivery.report, actions = actions)
            }
        }
    }
}

@Composable
private fun DeliveryHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Карточка единственной пользовательской настройки оформления. */
@Composable
private fun AppearanceCard(
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    val supported = LocalDynamicColorSupported.current

    SectionCard(
        title = stringResource(R.string.settings_dynamic_color_title),
        subtitle = if (supported) {
            stringResource(R.string.settings_dynamic_color_body)
        } else {
            stringResource(R.string.settings_dynamic_color_unavailable)
        },
        icon = Icons.Filled.Star,
        accent = MaterialTheme.colorScheme.secondary,
        trailing = {
            Switch(
                checked = dynamicColor && supported,
                onCheckedChange = onDynamicColorChange,
                enabled = supported,
            )
        },
    ) {}
}
