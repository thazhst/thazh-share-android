package com.thazh.share.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kyant.capsule.ContinuousCapsule
import com.thazh.share.R
import com.thazh.share.ui.components.GlasmioText
import com.thazh.share.ui.components.IosSwipeBackContainer
import com.thazh.share.ui.components.ThazhAppIconBadge
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import com.thazh.share.util.OnboardingPermissionStep
import com.thazh.share.util.PermissionUtils
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioIconButton

@Composable
fun PermissionRationaleScreen(
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val steps = remember { PermissionUtils.allSteps() }

    // Page -1 = Welcome Screen ("Trang Chào mừng")
    // Pages 0..steps.lastIndex = Individual Permission Preparation Pages ("Màn hình Chuẩn bị")
    var currentPageIndex by rememberSaveable { mutableIntStateOf(-1) }
    var deniedOnCurrentStep by rememberSaveable { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isStepSwipePop by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, currentPageIndex) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
                if (PermissionUtils.hasAllRequiredPermissions(context) && currentPageIndex >= 0) {
                    onPermissionsGranted()
                } else if (currentPageIndex in steps.indices) {
                    val step = steps[currentPageIndex]
                    if (PermissionUtils.isStepGranted(context, step)) {
                        deniedOnCurrentStep = false
                        isStepSwipePop = false
                        if (currentPageIndex < steps.lastIndex) {
                            currentPageIndex++
                        } else if (PermissionUtils.hasAllRequiredPermissions(context)) {
                            onPermissionsGranted()
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = currentPageIndex >= 0) {
        isStepSwipePop = false
        deniedOnCurrentStep = false
        currentPageIndex = (currentPageIndex - 1).coerceAtLeast(-1)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val stepIndex = currentPageIndex
        if (stepIndex !in steps.indices) return@rememberLauncherForActivityResult
        val currentStep = steps[stepIndex]
        val stepGranted = result.values.all { it } || PermissionUtils.isStepGranted(context, currentStep)
        if (stepGranted) {
            isStepSwipePop = false
            deniedOnCurrentStep = false
            if (stepIndex < steps.lastIndex) {
                currentPageIndex = stepIndex + 1
            } else {
                onPermissionsGranted()
            }
        } else {
            deniedOnCurrentStep = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = currentPageIndex,
            transitionSpec = {
                val springSpec = spring<androidx.compose.ui.unit.IntOffset>(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
                val transform = if (targetState > initialState) {
                    (slideInHorizontally(springSpec) { width -> width / 3 } + fadeIn(tween(220)) + scaleIn(initialScale = 0.96f)) togetherWith
                        (slideOutHorizontally(springSpec) { width -> -width / 3 } + fadeOut(tween(180)))
                } else if (isStepSwipePop) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    (slideInHorizontally(springSpec) { width -> -width / 3 } + fadeIn(tween(220)) + scaleIn(initialScale = 0.96f)) togetherWith
                        (slideOutHorizontally(springSpec) { width -> width / 3 } + fadeOut(tween(180)))
                }
                transform using SizeTransform(clip = false)
            },
            label = "onboarding_page_transition",
            modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->
            if (pageIndex < 0) {
                WelcomeOnboardingPage(
                    onStartClick = {
                        isStepSwipePop = false
                        deniedOnCurrentStep = false
                        if (PermissionUtils.hasAllRequiredPermissions(context)) {
                            onPermissionsGranted()
                        } else {
                            currentPageIndex = PermissionUtils.firstUngrantedStepIndex(context)
                        }
                    },
                )
            } else {
                val step = steps[pageIndex.coerceIn(steps.indices)]
                val isStepAlreadyGranted = remember(pageIndex, refreshTrigger) {
                    PermissionUtils.isStepGranted(context, step)
                }
                val goBackByButton = {
                    isStepSwipePop = false
                    deniedOnCurrentStep = false
                    currentPageIndex = (pageIndex - 1).coerceAtLeast(-1)
                }
                val goBackBySwipe = {
                    isStepSwipePop = true
                    deniedOnCurrentStep = false
                    currentPageIndex = (pageIndex - 1).coerceAtLeast(-1)
                }
                IosSwipeBackContainer(
                    enabled = true,
                    contentKey = pageIndex,
                    onBack = goBackBySwipe,
                    previousContent = {
                        if (pageIndex - 1 < 0) {
                            WelcomeOnboardingPage(onStartClick = {})
                        } else {
                            val prevStep = steps[(pageIndex - 1).coerceIn(steps.indices)]
                            PreparationPermissionPage(
                                step = prevStep,
                                stepNumber = pageIndex,
                                totalSteps = steps.size,
                                isGranted = PermissionUtils.isStepGranted(context, prevStep),
                                showDeniedHint = false,
                                onBackClick = {},
                                onGrantClick = {},
                                onOpenSettingsClick = {},
                            )
                        }
                    },
                ) {
                    PreparationPermissionPage(
                        step = step,
                        stepNumber = pageIndex + 1,
                        totalSteps = steps.size,
                        isGranted = isStepAlreadyGranted,
                        showDeniedHint = deniedOnCurrentStep,
                        onBackClick = goBackByButton,
                        onGrantClick = {
                            val permsToRequest = PermissionUtils.permissionsForStep(step)
                            if (permsToRequest.isEmpty() || PermissionUtils.isStepGranted(context, step)) {
                                isStepSwipePop = false
                                deniedOnCurrentStep = false
                                if (pageIndex < steps.lastIndex) {
                                    currentPageIndex = pageIndex + 1
                                } else {
                                    onPermissionsGranted()
                                }
                            } else {
                                permissionLauncher.launch(permsToRequest)
                            }
                        },
                        onOpenSettingsClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeOnboardingPage(
    onStartClick: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val entryScaleState = animateFloatAsState(
        targetValue = if (entered) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "welcome_entry_scale",
    )
    val entryAlphaState = animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "welcome_entry_alpha",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "welcome_breathing")
    val pulseScaleState = infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "welcome_pulse_scale",
    )

    // Scrollable full-screen container with generous inner padding so LiquidGlass bounce expansion never clips
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val s = entryScaleState.value
                    scaleX = s
                    scaleY = s
                    alpha = entryAlphaState.value
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Animated Breathing Apple HIG App Icon Emblem
            Box(
                modifier = Modifier.size(144.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .graphicsLayer {
                            val s = pulseScaleState.value
                            scaleX = s
                            scaleY = s
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    semantic.accentBlue.copy(alpha = 0.26f),
                                    semantic.accentCyan.copy(alpha = 0.12f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                ThazhAppIconBadge(
                    size = 96.dp,
                    cornerRadius = 22.dp,
                    contentDescription = stringResource(R.string.app_name),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlasmioText(
                    text = stringResource(R.string.welcome_title),
                    style = GlasmioTheme.textStyles.largeTitle.copy(fontWeight = FontWeight.Bold),
                    color = colors.content,
                    textAlign = TextAlign.Center,
                )

                GlasmioText(
                    text = stringResource(R.string.welcome_subtitle),
                    style = GlasmioTheme.textStyles.subheadline,
                    color = colors.secondaryContent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Extra padding around button prevents LiquidGlass interactive scale bounce from clipping
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                GlasmioButton(
                    onClick = onStartClick,
                    emphasis = GlassEmphasis.Prominent,
                    navigationSurfaceColor = semantic.accentBlue,
                    size = GlasmioButtonSize.Large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("welcome_start_button"),
                ) {
                    GlasmioText(
                        text = stringResource(R.string.welcome_start_button),
                        style = GlasmioTheme.textStyles.headline,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SfIcon(
                        systemName = ThazhSymbols.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        size = 18.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreparationPermissionPage(
    step: OnboardingPermissionStep,
    stepNumber: Int,
    totalSteps: Int,
    isGranted: Boolean,
    showDeniedHint: Boolean,
    onBackClick: () -> Unit,
    onGrantClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic

    val accentColor = when (step) {
        OnboardingPermissionStep.WIFI_DIRECT -> semantic.accentBlue
        OnboardingPermissionStep.BLUETOOTH_BLE -> semantic.accentCyan
        OnboardingPermissionStep.NOTIFICATIONS -> semantic.accentGreen
    }

    val stepIcon = when (step) {
        OnboardingPermissionStep.WIFI_DIRECT -> ThazhSymbols.WifiTethering
        OnboardingPermissionStep.BLUETOOTH_BLE -> ThazhSymbols.Bluetooth
        OnboardingPermissionStep.NOTIFICATIONS -> ThazhSymbols.NotificationsActive
    }

    val stepTitle = when (step) {
        OnboardingPermissionStep.WIFI_DIRECT -> stringResource(R.string.prep_wifi_title)
        OnboardingPermissionStep.BLUETOOTH_BLE -> stringResource(R.string.prep_ble_title)
        OnboardingPermissionStep.NOTIFICATIONS -> stringResource(R.string.prep_notif_title)
    }

    val stepWhyReason = when (step) {
        OnboardingPermissionStep.WIFI_DIRECT -> stringResource(R.string.prep_wifi_why_body)
        OnboardingPermissionStep.BLUETOOTH_BLE -> stringResource(R.string.prep_ble_why_body)
        OnboardingPermissionStep.NOTIFICATIONS -> stringResource(R.string.prep_notif_why_body)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "prep_icon_pulse")
    val iconPulseState = infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "prep_icon_scale",
    )

    // Scrollable full-screen container with inner padding so LiquidGlass bounce scale is never clipped
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Top Header with Back Button and Step Indicator Pill (padded so bounce never clips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GlasmioIconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("prep_back_button"),
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.Back,
                        contentDescription = stringResource(R.string.back),
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(ContinuousCapsule)
                        .background(accentColor.copy(alpha = if (colors.isDark) 0.22f else 0.14f))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    GlasmioText(
                        text = stringResource(R.string.prep_step_badge, stepNumber, totalSteps),
                        style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                        color = accentColor,
                    )
                }

                Spacer(modifier = Modifier.size(44.dp))
            }

            // Animated Step Progress Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (index in 1..totalSteps) {
                    val active = index == stepNumber
                    val completed = index < stepNumber
                    val dotWidth by animateDpAsState(
                        targetValue = if (active) 28.dp else 8.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "step_dot_width_$index",
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(ContinuousCapsule)
                            .background(
                                when {
                                    active -> accentColor
                                    completed -> accentColor.copy(alpha = 0.55f)
                                    else -> colors.separator.copy(alpha = 0.5f)
                                },
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Borderless Soft Ambient Halo Icon Emblem
            Box(
                modifier = Modifier.size(104.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            val s = iconPulseState.value
                            scaleX = s
                            scaleY = s
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = if (colors.isDark) 0.30f else 0.20f),
                                    accentColor.copy(alpha = if (colors.isDark) 0.10f else 0.06f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = if (colors.isDark) 0.22f else 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    SfIcon(
                        systemName = stepIcon,
                        contentDescription = null,
                        tint = accentColor,
                        size = 36.dp,
                    )
                }
            }

            GlasmioText(
                text = stepTitle,
                style = GlasmioTheme.textStyles.title1.copy(fontWeight = FontWeight.Bold),
                color = colors.content,
                textAlign = TextAlign.Center,
            )

            // Frameless concise explanation (no GlasmioCard frame around the text)
            GlasmioText(
                text = stepWhyReason,
                style = GlasmioTheme.textStyles.body,
                color = colors.secondaryContent,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            )

            AnimatedVisibility(
                visible = showDeniedHint && !isGranted,
                enter = fadeIn() + expandVertically(clip = false),
                exit = fadeOut() + shrinkVertically(clip = false),
            ) {
                GlasmioText(
                    text = stringResource(R.string.prep_denied_hint),
                    style = GlasmioTheme.textStyles.footnote,
                    color = semantic.warningOrange,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Generous margin around LiquidGlass action buttons so press bounce never clips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlasmioButton(
                    onClick = onGrantClick,
                    emphasis = GlassEmphasis.Prominent,
                    navigationSurfaceColor = accentColor,
                    size = GlasmioButtonSize.Large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_permission_button"),
                ) {
                    GlasmioText(
                        text = when {
                            isGranted && stepNumber < totalSteps -> stringResource(R.string.prep_continue_next)
                            isGranted -> stringResource(R.string.prep_finish_button)
                            stepNumber < totalSteps -> stringResource(R.string.prep_grant_and_continue)
                            else -> stringResource(R.string.prep_grant_and_finish)
                        },
                        style = GlasmioTheme.textStyles.headline,
                        color = Color.White,
                    )
                }

                AnimatedVisibility(
                    visible = showDeniedHint && !isGranted,
                    enter = fadeIn() + expandVertically(clip = false),
                    exit = fadeOut() + shrinkVertically(clip = false),
                ) {
                    GlasmioButton(
                        onClick = onOpenSettingsClick,
                        emphasis = GlassEmphasis.Regular,
                        size = GlasmioButtonSize.Large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_app_settings_button"),
                    ) {
                        GlasmioText(
                            text = stringResource(R.string.permission_open_settings),
                            style = GlasmioTheme.textStyles.headline,
                            color = colors.accent,
                        )
                    }
                }
            }
        }
    }
}
