package net.opendasharchive.openarchive.features.settings.passcode.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.settings.passcode.AppHapticFeedbackType
import net.opendasharchive.openarchive.features.settings.passcode.HapticManager
import org.koin.compose.koinInject

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
    onSubmitClick: () -> Unit
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
                                    else ->  onNumberClick(label)
                                }
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(72.dp))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun NumericKeypadPreview() {
    SaveAppTheme {
        Scaffold {
            Box(
                modifier = Modifier.padding(it),
                contentAlignment = Alignment.Center
            ) {
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
    }
}

@Composable
private fun NumberButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    //hapticManager: HapticManager = koinInject()
) {

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Determine background color based on button type and pressed state
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed -> when (label) {
                "delete" -> colorResource(R.color.red_bg).copy(alpha = 0.5f)
                "submit" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            }
            else -> when (label) {
                "delete" -> colorResource(R.color.red_bg).copy(alpha = 0.3f)
                "submit" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                else -> Color.Transparent
            }
        },
        animationSpec = spring(),
        label = ""
    )

    // Determine background color based on button type and pressed state
    val borderColor by animateColorAsState(
        targetValue = when {
            isPressed -> when (label) {
                "delete" -> Color.Transparent
                "submit" -> Color.Transparent
                else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
            }
            else -> when (label) {
                "delete" -> Color.Transparent
                "submit" -> Color.Transparent
                else -> MaterialTheme.colorScheme.tertiary
            }
        },
        animationSpec = spring(),
        label = ""
    )

    Box(
        modifier = Modifier
            .background(color = backgroundColor, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    //hapticManager.performHapticFeedback(AppHapticFeedbackType.KeyPress)
                    onClick()
                }
            )
            .border(width = 2.dp, color = borderColor, shape = CircleShape)
            .size(72.dp),
        contentAlignment = Alignment.Center
    ) {

        when (label) {
            "delete" -> Icon(
                imageVector = Icons.Default.Backspace,
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
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    }
}