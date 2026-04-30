package net.opendasharchive.openarchive.features.onboarding

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.onboarding.components.AutoSizeText
import net.opendasharchive.openarchive.features.onboarding.components.StyledTitleText

@Composable
fun OnboardingWelcomeScreen(
    onGetStartedClick: () -> Unit = {}
) {
    val arrowOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(3000) // Initial delay like XML (startDelay = 3000)
        while (true) {
            // Animate from 0 → 25 → 0 with bounce effect, matching XML animation
            // XML: duration = 2000ms with BounceInterpolator
            arrowOffset.animateTo(
                targetValue = 25f,
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = EaseOutBounce // Similar to BounceInterpolator
                )
            )
            arrowOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = EaseOutBounce
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // Main content area (equivalent to LinearLayout above nav_block in XML)
        // XML has weightSum="63" with title_block weight="55", leaving 8 units at bottom
        Column(
            modifier = Modifier
                .weight(1f) // Take remaining space above nav block
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, end = 32.dp),
            verticalArrangement = Arrangement.Center // Match XML android:gravity="center_vertical"
        ) {

            Spacer(modifier = Modifier.weight(4f))

            // Logo (weight=8 of 55) - wrap_content width like XML
            Box(
                modifier = Modifier
                    .weight(8f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomStart // Push logo down to create space above
            ) {
                Image(
                    painter = painterResource(R.drawable.save_oa),
                    contentDescription = null,
                    modifier = Modifier.wrapContentSize(),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
                    contentScale = ContentScale.Fit
                )
            }

            // Spacer (weight=8 of 55)
            Spacer(modifier = Modifier.weight(4f))

            // Four title texts (weight=8 each of 55)
            StyledTitleText(
                text = stringResource(R.string.intro_header_secure),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(8f)
            )

            StyledTitleText(
                text = stringResource(R.string.intro_header_archive),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(8f)
            )

            StyledTitleText(
                text = stringResource(R.string.intro_header_verify),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(8f)
            )

            StyledTitleText(
                text = stringResource(R.string.intro_header_encrypt),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(8f)
            )

            // Spacer (weight=2 of 55)
            Spacer(modifier = Modifier.weight(2f))

            // Description text (weight=5 of 55) - with auto text sizing like XML
            AutoSizeText(
                text = stringResource(R.string.secure_mobile_media_preservation),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(5f),
                color = MaterialTheme.colorScheme.onBackground,
                minFontSize = 16.sp,
                maxFontSize = 500.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Bottom spacer (weight=8 of 63 total in XML)
            // XML has weightSum="63" with content using weight="55", leaving 8 units for bottom spacing
            Spacer(modifier = Modifier.weight(8f))
        }

        // Nav block at bottom (equivalent to android:layout_alignParentBottom="true")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalAlignment = Alignment.Top
        ) {
            // Hand image with negative margins like XML
            Image(
                painter = painterResource(R.drawable.onboarding23_app_hand),
                contentDescription = null,
                modifier = Modifier
                    .width(170.dp)
                    .height(200.dp)
                    .offset(x = (-8).dp, y = (8).dp) // Negative margins like XML
            )

            // Get Started section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onGetStartedClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = stringResource(R.string.get_started),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 16.dp)
                )

                Image(
                    painter = painterResource(R.drawable.onboarding23_arrow_right),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { translationX = arrowOffset.value },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                )
            }
        }
    }


}


@Preview(name = "Welcome Screen Light", showBackground = true)
@Preview(name = "Welcome Screen Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingWelcomeScreenPreviewLight() {
    SaveAppTheme {
        OnboardingWelcomeScreen(
            onGetStartedClick = {}
        )
    }
}