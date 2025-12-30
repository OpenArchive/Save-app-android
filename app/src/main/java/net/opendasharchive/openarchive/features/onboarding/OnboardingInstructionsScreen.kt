package net.opendasharchive.openarchive.features.onboarding

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import com.tbuonomo.viewpagerdotsindicator.compose.DotsIndicator
import com.tbuonomo.viewpagerdotsindicator.compose.model.DotGraphic
import com.tbuonomo.viewpagerdotsindicator.compose.type.WormIndicatorType
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.LocalColors
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.onboarding.components.HtmlText
import net.opendasharchive.openarchive.features.onboarding.components.OnboardingSlide

@Composable
fun OnboardingInstructionsScreen(
    onDone: () -> Unit,
) {
    val slides = listOf(
        OnboardingSlide(
            titleRes = R.string.intro_header_secure,
            textRes = R.string.intro_text_secure,
            imageRes = R.drawable.onboarding_secure_png
        ),
        OnboardingSlide(
            titleRes = R.string.intro_header_archive,
            textRes = R.string.intro_text_archive,
            linkRes = R.string.intro_link_archive,
            imageRes = R.drawable.onboarding_archive_png
        ),
        OnboardingSlide(
            titleRes = R.string.intro_header_verify,
            textRes = R.string.intro_text_verify,
            linkRes = R.string.intro_link_verify,
            imageRes = R.drawable.onboarding_verify_png
        ),
        OnboardingSlide(
            titleRes = R.string.intro_header_encrypt,
            textRes = R.string.intro_text_encrypt,
            linkRes = R.string.intro_link_encrypt,
            imageRes = R.drawable.onboarding_encrypt_png
        )
    )

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val isLastPage = currentPage == slides.size - 1

    // Handle cover image animation
    val imageAlpha by animateFloatAsState(
        targetValue = if (pagerState.isScrollInProgress) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "imageAlpha"
    )

    // Handle back press
    LaunchedEffect(pagerState.currentPage) {
        // This composition will be called every time page changes
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Right side panel with tertiary background - extends to top edge behind status bars
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.tertiary)
                .width(120.dp) // Approximate width based on buttons
                .zIndex(0f) // Behind other elements
                .padding(bottom = 8.dp) // Only bottom padding, let it extend to top
        ) {
            // Top system bar space
            Spacer(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
            )

            // Skip button (top) - matches XML: insetTop="32dp" + marginTop="8dp" = 40dp
            if (!isLastPage) {
                TextButton(
                    onClick = onDone,
                    modifier = Modifier
                        .padding(top = 20.dp, start = 8.dp, end = 8.dp) // Match XML positioning
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stringResource(R.string.skip),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold // Match XML montserrat_semi_bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // FAB (bottom) - with navigation bar padding, proper color, and scale animation
            // Matches XML behavior: scales UP when pressed and stays large until released
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 1.15f else 1f, // Scale UP when pressed (like XML)
                animationSpec = tween(durationMillis = 100),
                label = "fabScale"
            )

            FloatingActionButton(
                onClick = {
                    if (isLastPage) {
                        onDone()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(24.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                containerColor = LocalColors.current.primaryBright, // Use theme color
                contentColor = MaterialTheme.colorScheme.onBackground,
                shape = RoundedCornerShape(8.dp),
                interactionSource = interactionSource
            ) {
                Icon(
                    painter = painterResource(if (isLastPage) R.drawable.ic_done else R.drawable.ic_arrow_forward_ios),
                    contentDescription = if (isLastPage) "Done" else stringResource(R.string.next),
                    tint = Color.Black
                )
            }
        }

        // Main layout structure following the XML layout (weightSum=125)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f) // Above the right panel
        ) {
            // Top spacer (weight=8 of 125) - with status bar padding
            Spacer(
                modifier = Modifier
                    .weight(8f)
                    .windowInsetsPadding(WindowInsets.statusBars)
            )

            // Cover Image (weight=65 of 125) - extends to touch left edge and under the right panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(65f)
                    .padding(end = 40.dp) // Only end padding for buttons
                    .offset(x = (-8).dp) // Use offset instead of negative padding to extend left
            ) {
                Image(
                    painter = painterResource(slides[currentPage].imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = imageAlpha },
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart
                )
            }

            // Middle spacer (weight=37 of 125)
            Spacer(modifier = Modifier.weight(37f))

            // Bottom area with dots indicator (weight=15 of 125)
            // Calculate width: 4 dots * 10dp + 3 spacings * 7dp + buffer for worm animation
            val indicatorWidth = (slides.size * 10.dp) + ((slides.size - 1) * 7.dp) + 30.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(15f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Dots indicator - constrained width to prevent expansion
                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .wrapContentHeight()
                ) {
                    DotsIndicator(
                        dotCount = slides.size,
                        modifier = Modifier.fillMaxWidth(), // Fill the constrained Box width
                        dotSpacing = 10.dp, // Match XML dotsSpacing="7dp"
                        pagerState = pagerState,
                        type = WormIndicatorType(
                            dotsGraphic = DotGraphic(
                                size = 10.dp, // Match XML dotsSize="10dp"
                                borderWidth = 5.dp, // Match XML dotsStrokeWidth="5dp"
                                borderColor = Color(0xFF666666), // Match XML c23_medium_grey
                                color = Color.Transparent, // Empty center for inactive dots
                            ),
                            wormDotGraphic = DotGraphic(
                                size = 10.dp, // Match XML dotsSize="10dp"
                                color = MaterialTheme.colorScheme.onBackground, // Match XML dotsColor
                            )
                        )
                    )
                }
            }
        }

        // ViewPager equivalent - HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(-1f) // Behind the tertiary panel so content gets clipped by it
        ) { page ->
            OnboardingSlideContent(
                slide = slides[page],
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun OnboardingSlideContent(
    slide: OnboardingSlide,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(
                start = 24.dp,
                end = 140.dp
            ) // Increased to ensure text stays behind 120dp tertiary bar
    ) {
        // Top spacer (weight=8 of 125)
        Spacer(modifier = Modifier.weight(8f))

        // Image spacer (weight=65 of 125)
        Spacer(modifier = Modifier.weight(65f))

        // Content area (weight=42 of 125)
        Column(
            modifier = Modifier
                .weight(42f)
                .padding(top = 8.dp)
        ) {
            // Title - matches XML: textFontWeight="800" (ExtraBold), textSize="28sp"
            Text(
                text = stringResource(slide.titleRes).uppercase(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold // Match XML textFontWeight="800"
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Summary with HTML support
            HtmlText(
                textRes = slide.textRes,
                linkRes = slide.linkRes,
                color = MaterialTheme.colorScheme.onBackground,
                linkColor = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp
            )
        }

        // Bottom spacer (weight=10 of 125)
        Spacer(modifier = Modifier.weight(10f))
    }
}


@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingInstructionsScreenPreview() {
    SaveAppTheme {
        OnboardingInstructionsScreen(
            onDone = {},
        )
    }
}


@Preview(name = "Single Slide Light", showBackground = true)
@Preview(name = "Single Slide Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingSlideContentPreview() {
    val sampleSlide = OnboardingSlide(
        titleRes = R.string.intro_header_secure,
        textRes = R.string.intro_text_secure,
        imageRes = R.drawable.onboarding_secure_png
    )
    SaveAppTheme {
        OnboardingSlideContent(
            slide = sampleSlide,
            modifier = Modifier.fillMaxSize()
        )
    }
}