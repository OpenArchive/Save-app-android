package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.ThemeColors
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.asString

@Composable
fun ButtonBar(
    modifier: Modifier = Modifier,
    backButtonText: UiText = UiText.Resource(R.string.back),
    nextButtonText: UiText = UiText.Resource(R.string.next),
    isBackEnabled: Boolean = false,
    isNextEnabled: Boolean = false,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            modifier = Modifier
                .padding(8.dp)
                .heightIn(ThemeDimensions.touchable)
                .weight(1f),
            colors = ButtonDefaults.textButtonColors(
                contentColor = colorResource(R.color.colorOnBackground)
            ),
            enabled = isBackEnabled,
            shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
            onClick = onBack
        ) {
            Text(backButtonText.asString())
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            modifier = Modifier
                .padding(8.dp)
                .heightIn(ThemeDimensions.touchable)
                .weight(1f),
            enabled = isNextEnabled,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(ThemeDimensions.roundedCorner),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                disabledContainerColor = colorResource(R.color.grey_50),
                disabledContentColor = colorResource(R.color.extra_light_grey)//MaterialTheme.colorScheme.onBackground
            ),
            onClick = onNext,
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = ThemeColors.material.primary)
            } else {
                Text(
                    nextButtonText.asString(),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}