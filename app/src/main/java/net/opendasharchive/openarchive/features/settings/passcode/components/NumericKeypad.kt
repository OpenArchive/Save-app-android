package net.opendasharchive.openarchive.features.settings.passcode.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.PreviewLightDark

private val keys = listOf(
    "1", "2", "3",
    "4", "5", "6",
    "7", "8", "9",
    "delete", "0", "submit"
)

@Composable
fun NumericKeypad(
    isEnabled: Boolean = true,
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onHaptic: () -> Unit = {},
) {

    Box(
        modifier = Modifier,
        contentAlignment = Alignment.Center
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(keys, key = { it }) { label ->
                Box(
                    modifier = Modifier
                        .size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (label.isNotEmpty()) {
                        NumberButton(
                            label = label,
                            enabled = isEnabled,
                            onClick = {
                                when (label) {
                                    "delete" -> onDeleteClick()
                                    "submit" -> onSubmitClick()
                                    else -> onNumberClick(label)
                                }
                            },
                            onHaptic = onHaptic
                        )
                    } else {
                        Spacer(modifier = Modifier.size(72.dp))
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun NumericKeypadPreview() {

    DefaultScaffoldPreview {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Custom numeric keypad
            NumericKeypad(
                isEnabled = true,
                onNumberClick = { number ->

                },
                onDeleteClick = {},
                onSubmitClick = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

        }

    }

}

@Composable
private fun NumberButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onHaptic: () -> Unit = {},
) {

    val pressedColor = when (label) {
        "delete" -> colorResource(R.color.red_bg).copy(alpha = 0.5f)
        "submit" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
    }
    val restingColor = when (label) {
        "delete" -> colorResource(R.color.red_bg).copy(alpha = 0.3f)
        "submit" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // pressAlpha drives the fill: 1f = fully pressed color, 0f = resting color
    val pressAlpha = remember { Animatable(0f) }

    // Use a scope so we can cancel the fade-out if a new press arrives mid-animation
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val backgroundColor = androidx.compose.ui.graphics.lerp(restingColor, pressedColor, pressAlpha.value)
    val borderColor = if (label == "delete" || label == "submit") {
        Color.Transparent
    } else {
        tertiaryColor.copy(alpha = 1f - pressAlpha.value)
    }

    Box(
        modifier = Modifier
            .background(color = backgroundColor, shape = CircleShape)
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = { _ ->
                        if (!enabled) return@detectTapGestures
                        onHaptic()
                        scope.launch { pressAlpha.snapTo(1f) }
                        tryAwaitRelease()
                        scope.launch {
                            delay(60)
                            pressAlpha.animateTo(0f, animationSpec = tween(200))
                        }
                    },
                    onTap = { if (enabled) onClick() }
                )
            }
            .border(width = 2.dp, color = borderColor, shape = CircleShape)
            .size(72.dp),
        contentAlignment = Alignment.Center
    ) {

        when (label) {
            "delete" -> Icon(
                painter = painterResource(R.drawable.ic_backspace),
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onBackground
            )

            "submit" -> Icon(
                painter = painterResource(R.drawable.ic_arrow_submit),
                contentDescription = "Submit",
                tint = MaterialTheme.colorScheme.onBackground
            )

            else -> Text(
                text = label,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    }
}