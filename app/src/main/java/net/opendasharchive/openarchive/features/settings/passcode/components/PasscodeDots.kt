package net.opendasharchive.openarchive.features.settings.passcode.components

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import kotlin.math.roundToInt

@Composable
fun PasscodeDots(
    passcodeLength: Int,
    currentPasscodeLength: Int,
    shouldShake: Boolean = false
) {

    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(shouldShake) {
        if (shouldShake) {
            val offsets = listOf(25f, 15f, 10f)
            for (offset in offsets) {
                shakeOffset.animateTo(
                    targetValue = offset, // Move right
                    animationSpec = tween(durationMillis = 100)
                )
                shakeOffset.animateTo(
                    targetValue = -offset, // Move left
                    animationSpec = tween(durationMillis = 100)
                )
            }
            shakeOffset.animateTo(
                targetValue = 0f, // Reset to original position
                animationSpec = tween(durationMillis = 50)
            )
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.offset {
            IntOffset(shakeOffset.value.roundToInt(), 0)
        }
    ) {
        repeat(passcodeLength) { index ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        color = if (index < currentPasscodeLength) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PasswordDotsPreview() {

    DefaultScaffoldPreview {
        PasscodeDots(
            passcodeLength = 6,
            currentPasscodeLength = 3
        )
    }
}