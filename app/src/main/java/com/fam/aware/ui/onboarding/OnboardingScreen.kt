package com.fam.aware.ui.onboarding

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fam.aware.R
import com.fam.aware.data.model.AppPermission
import com.fam.aware.ui.components.BulletItem
import com.fam.aware.ui.components.ExpandableDetails
import com.fam.aware.ui.components.ParentGuideSheet
import com.fam.aware.ui.components.PermissionActions
import com.fam.aware.ui.components.PermissionCard
import com.fam.aware.ui.components.SectionCard
import com.fam.aware.ui.components.SupervisionCard
import com.fam.aware.ui.components.UiStateContent
import com.fam.aware.ui.theme.Dimens
import com.fam.aware.ui.theme.Motion
import com.fam.aware.util.SystemIntents
import com.fam.aware.util.findActivity
import com.fam.aware.viewmodel.OnboardingUiState
import com.fam.aware.viewmodel.OnboardingViewModel

/**
 * Экран первичной настройки.
 *
 * Три шага: что делает приложение, какие разрешения и зачем нужны,
 * и что происходит, если устройством управляет родитель.
 */
@Composable
fun OnboardingScreen(
    viewModelFactory: ViewModelProvider.Factory,
    onFinished: () -> Unit,
) {
    val onboardingViewModel: OnboardingViewModel = viewModel(factory = viewModelFactory)
    val state by onboardingViewModel.uiState.collectAsStateWithLifecycle()
    val completed by onboardingViewModel.onboardingCompleted.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var parentGuideVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(completed) {
        if (completed) onFinished()
    }

    // После возврата из системных настроек перечитываем состояние разрешений.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onboardingViewModel.onResumed()
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
        onboardingViewModel.onPermissionResult(rationaleVisible)
    }

    val permissionActions = remember(context, permissionLauncher, onboardingViewModel) {
        PermissionActions(
            onRequest = {
                // Разрешение запрашивается только по явному нажатию пользователя.
                onboardingViewModel.onPermissionRequestStarted()
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

    val pagerState = rememberPagerState(pageCount = { state.pageCount })

    LaunchedEffect(state.page) {
        if (pagerState.currentPage != state.page) {
            pagerState.animateScrollToPage(state.page)
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != state.page) {
            onboardingViewModel.setPage(pagerState.settledPage)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OnboardingHeader()

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.Top,
        ) { page ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = Dimens.ScreenPadding),
            ) {
                when (page) {
                    0 -> IntroPage()
                    1 -> PermissionsPage(state = state, actions = permissionActions)
                    else -> SupervisionPage(
                        state = state,
                        actions = permissionActions,
                        onRetry = onboardingViewModel::refresh,
                    )
                }
                Spacer(Modifier.height(Dimens.SectionGap))
            }
        }

        PageIndicator(
            pageCount = state.pageCount,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        )

        OnboardingControls(
            isFirstPage = state.isFirstPage,
            isLastPage = state.isLastPage,
            onBack = onboardingViewModel::previousPage,
            onNext = {
                if (state.isLastPage) {
                    onboardingViewModel.complete()
                } else {
                    onboardingViewModel.nextPage()
                }
            },
        )
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
private fun OnboardingHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.size(Dimens.ItemGap))
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
    }
}

@Composable
private fun IntroPage() {
    Column(modifier = Modifier.fillMaxWidth()) {
        PageTitle(stringResource(R.string.onboarding_intro_title))
        Text(
            text = stringResource(R.string.onboarding_intro_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.ItemGap))
        Text(
            text = stringResource(R.string.onboarding_intro_body_2),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.SectionGap))

        SectionCard(
            title = stringResource(R.string.home_section_transparency),
            icon = Icons.Filled.Lock,
            accent = MaterialTheme.colorScheme.secondary,
        ) {
            listOf(
                R.string.transparency_item_1,
                R.string.transparency_item_2,
                R.string.transparency_item_3,
                R.string.transparency_item_5,
                R.string.transparency_item_6,
                R.string.transparency_item_7,
            ).forEach { itemRes ->
                BulletItem(
                    text = stringResource(itemRes),
                    marker = Icons.Filled.CheckCircle,
                    markerTint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun PermissionsPage(
    state: OnboardingUiState,
    actions: PermissionActions,
) {
    var legacyNoteExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        PageTitle(stringResource(R.string.onboarding_permissions_title))
        Text(
            text = stringResource(R.string.onboarding_permissions_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.SectionGap))

        val report = state.permission
        if (report != null) {
            PermissionCard(report = report, actions = actions)
        }

        Spacer(Modifier.height(Dimens.ItemGap))

        Surface(
            shape = RoundedCornerShape(16.dp),
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
                    text = stringResource(R.string.onboarding_permissions_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        TextButton(
            onClick = { legacyNoteExpanded = !legacyNoteExpanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(
                    if (legacyNoteExpanded) {
                        R.string.onboarding_legacy_toggle_hide
                    } else {
                        R.string.onboarding_legacy_toggle_show
                    },
                ),
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
        }
        ExpandableDetails(expanded = legacyNoteExpanded) {
            Text(
                text = stringResource(R.string.perm_notifications_legacy_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SupervisionPage(
    state: OnboardingUiState,
    actions: PermissionActions,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PageTitle(stringResource(R.string.onboarding_supervision_title))
        Text(
            text = stringResource(R.string.onboarding_supervision_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_supervision_body_2),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.SectionGap))

        UiStateContent(
            state = state.supervision,
            loadingText = stringResource(R.string.state_loading),
            onRetry = onRetry,
            permissionActions = actions,
        ) { report ->
            SupervisionCard(report = report)
        }

        Spacer(Modifier.height(Dimens.ItemGap))

        TextButton(
            onClick = actions.onShowParentGuide,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.action_parent_guide))
        }
    }
}

@Composable
private fun PageTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = Dimens.ItemGap),
    )
}

/** Анимированный индикатор страниц мастера. */
@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.ItemGap),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) 24.dp else 8.dp,
                animationSpec = tween(Motion.DurationMedium),
                label = "dot-width",
            )
            val color by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                animationSpec = tween(Motion.DurationMedium),
                label = "dot-color",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(width)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun OnboardingControls(
    isFirstPage: Boolean,
    isLastPage: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = !isFirstPage,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.onboarding_back))
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onNext) {
            Text(
                text = stringResource(
                    if (isLastPage) R.string.onboarding_finish else R.string.onboarding_next,
                ),
            )
        }
    }
}
