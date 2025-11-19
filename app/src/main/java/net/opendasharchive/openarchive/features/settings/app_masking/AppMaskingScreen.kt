package net.opendasharchive.openarchive.features.settings.app_masking

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.colorResource
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.util.Prefs

private val maskGradients = mapOf(
    AppMaskId.DEFAULT to listOf(Color(0xFF00B4A6), Color(0xFF00B4A6)),
    AppMaskId.CALCULATOR to listOf(Color(0xFFF44336), Color(0xFF9C27B0)),
    AppMaskId.DICTIONARY to listOf(Color(0xFFFF9800), Color(0xFFFFC107)),
    AppMaskId.CALENDAR to listOf(Color(0xFF009688), Color(0xFF673AB7))
)

@Composable
fun AppMaskingScreen() {
    val context = LocalContext.current
    val maskOptions = remember(context) { AppMaskingUtils.getMaskOptions(context) }
    var currentMask by remember { mutableStateOf(AppMaskingUtils.getCurrentMask(context)) }
    var pendingMask by remember { mutableStateOf<AppMask?>(null) }
    var isClosing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val activity = context.findActivity()

    SaveAppTheme {
        AppMaskingScreenContent(
            masks = maskOptions,
            currentMask = currentMask,
            isClosing = isClosing,
            errorMessage = errorMessage,
            onErrorDismissed = { errorMessage = null },
            onMaskSelected = { mask ->
                if (!isClosing) {
                    pendingMask = mask
                }
            }
        )

        MaskConfirmSheet(
            mask = pendingMask,
            onDismiss = {
                if (!isClosing) {
                    pendingMask = null
                }
            },
            onConfirm = { mask ->
                pendingMask = null

                // Apply the mask change immediately
                val result = AppMaskingUtils.setLauncherActivityAlias(context.applicationContext, mask)

                result.fold(
                    onSuccess = {
                        Prefs.returnToSettingsAfterRestart = true
                        currentMask = mask
                        isClosing = true

                        // Brief delay for smooth UX - allows user to see the change succeeded
                        // This is intentional UX polish, not a technical requirement
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            activity?.finish()
                        }, 300)
                    },
                    onFailure = {
                        errorMessage = context.getString(R.string.app_mask_error)
                    }
                )
            }
        )
    }
}

@Composable
private fun AppMaskingScreenContent(
    masks: List<AppMask>,
    currentMask: AppMask,
    isClosing: Boolean,
    errorMessage: String?,
    onErrorDismissed: () -> Unit,
    onMaskSelected: (AppMask) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    androidx.compose.runtime.LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onErrorDismissed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.app_mask_heading),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.app_mask_subheading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                InfoBanner()
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(masks, key = { it.id }) { mask ->
                MaskCard(
                    mask = mask,
                    isActive = mask.alias == currentMask.alias,
                    onSelect = { onMaskSelected(mask) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Brief success overlay before closing - unique to Save
        if (isClosing) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.app_mask_success),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun InfoBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.app_mask_tip_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.app_mask_tip_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MaskCard(
    mask: AppMask,
    isActive: Boolean,
    onSelect: () -> Unit
) {
    val gradient = gradientFor(mask)
    val badgeColor = gradient.first()
    ElevatedCard(
        onClick = {
            if (!isActive) {
                onSelect()
            }
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaskIcon(mask = mask, size = 68.dp, iconSize = 36.dp)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(id = mask.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = mask.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isActive) {
                ActiveBadge(primaryTint = badgeColor)
            } else {
                TextButton(
                    onClick = onSelect,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text(text = stringResource(id = R.string.app_mask_use_this_look))
                }
            }
        }
    }
}

@Composable
private fun ActiveBadge(primaryTint: Color) {
    Surface(
        color = primaryTint.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, primaryTint.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = stringResource(id = R.string.app_mask_active_badge),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun gradientFor(mask: AppMask): List<Color> {
    return maskGradients[mask.id] ?: listOf(Color(0xFF00B4A6), Color(0xFF00B4A6))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaskConfirmSheet(
    mask: AppMask?,
    onDismiss: () -> Unit,
    onConfirm: (AppMask) -> Unit
) {
    val sheetMask = mask ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {

        MaskConfirmSheetContent(
            sheetMask = sheetMask,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaskConfirmSheetContent(
    sheetMask: AppMask,
    onDismiss: () -> Unit,
    onConfirm: (AppMask) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        MaskIcon(mask = sheetMask, size = 96.dp, iconSize = 48.dp, cornerRadius = 28.dp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.app_mask_confirm_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(
                id = R.string.app_mask_confirm_description,
                stringResource(id = sheetMask.titleRes)
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorResource(R.color.colorOnBackground)
                )
            ) {
                Text(text = stringResource(id = R.string.action_cancel))
            }

            Button(
                onClick = { onConfirm(sheetMask) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = colorResource(R.color.black)
                )
            ) {
                Text(text = stringResource(id = R.string.app_mask_confirm_action))
            }
        }
    }

}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview
@Composable
private fun AppMaskingScreenPreview() {
    val mockMasks = listOf(
        AppMask(
            AppMaskId.DEFAULT,
            "a",
            R.string.app_mask_default_label,
            R.string.app_mask_default_description,
            R.drawable.ic_mask_save_icon
        ),
        AppMask(
            AppMaskId.CALCULATOR,
            "b",
            R.string.app_mask_calculator_label,
            R.string.app_mask_calculator_description,
            R.drawable.ic_mask_save_calculator
        ),
        AppMask(
            AppMaskId.CALENDAR,
            "c",
            R.string.app_mask_calendar_label,
            R.string.app_mask_calendar_description,
            R.drawable.ic_mask_save_calendar
        )
    )
    DefaultScaffoldPreview {
        AppMaskingScreenContent(
            masks = mockMasks,
            currentMask = mockMasks.first(),
            isClosing = false,
            errorMessage = null,
            onErrorDismissed = {},
            onMaskSelected = {}
        )
    }
}

@Preview
@Composable
private fun MaskConfirmSheetPreview() {
    DefaultScaffoldPreview {

        MaskConfirmSheetContent(
            sheetMask = AppMask(
                AppMaskId.DICTIONARY,
                "b",
                R.string.app_mask_dictionary_label,
                R.string.app_mask_dictionary_description,
                R.drawable.ic_mask_save_dictionary
            ),
            onDismiss = {},
            onConfirm = {}

        )
    }
}

@Composable
private fun MaskIcon(
    mask: AppMask,
    size: Dp = 68.dp,
    iconSize: Dp = 36.dp,
    cornerRadius: Dp = 20.dp
) {
    val gradient = gradientFor(mask)
    Box(
        modifier = Modifier
            .size(size)
            .background(
                brush = Brush.linearGradient(gradient),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = mask.iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(iconSize)
        )
    }
}
