package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.times
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.tbuonomo.viewpagerdotsindicator.compose.DotsIndicator
import com.tbuonomo.viewpagerdotsindicator.compose.model.DotGraphic
import com.tbuonomo.viewpagerdotsindicator.compose.type.IndicatorType
import com.tbuonomo.viewpagerdotsindicator.compose.type.ShiftIndicatorType
import com.tbuonomo.viewpagerdotsindicator.compose.type.WormIndicatorType
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityOnboarding23InstructionsBinding
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.LocalColors

class Onboarding23InstructionsActivity : BaseActivity() {

    private lateinit var mBinding: ActivityOnboarding23InstructionsBinding
    
    // Toggle to switch between XML and Compose implementation
    private val useComposeImplementation = true  // Set to false to use XML implementation

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
        )
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        if (useComposeImplementation) {
            // Use Compose implementation
            setContent {
                SaveAppTheme {
                    OnboardingInstructionsScreen(
                        onDone = { done() },
                        onBackPressed = { finish() }
                    )
                }
            }
        } else {
            // Use original XML implementation
            setupXmlImplementation()
        }
    }
    
    private fun setupXmlImplementation() {
        mBinding = ActivityOnboarding23InstructionsBinding.inflate(layoutInflater)

        mBinding.fab.applyEdgeToEdgeInsets { insets ->
            bottomMargin = insets.bottom
        }

        setContentView(mBinding.root)

        mBinding.skipButton.setOnClickListener {
            done()
        }

        mBinding.viewPager.adapter =
            Onboarding23FragmentStateAdapter(supportFragmentManager, lifecycle, this)

        mBinding.dotsIndicator.attachTo(mBinding.viewPager)

        mBinding.fab.setOnClickListener {
            if (isLastPage()) {
                done()
            } else {
                mBinding.coverImage.alpha = 0F
                mBinding.viewPager.currentItem++
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFirstPage()) {
                    finish()
                } else {
                    mBinding.viewPager.currentItem--
                }
            }
        })

        mBinding.viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (isLastPage()) {
                    mBinding.skipButton.visibility = View.INVISIBLE
                    val icon = ContextCompat.getDrawable(mBinding.fab.context, R.drawable.ic_done)
                    mBinding.fab.setImageDrawable(icon)
                } else {
                    mBinding.skipButton.visibility = View.VISIBLE
                    val icon = ContextCompat.getDrawable(mBinding.fab.context, R.drawable.ic_arrow_forward_ios,)
                    icon?.isAutoMirrored = true
                    mBinding.fab.setImageDrawable(icon)
                }

            }

            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> mBinding.coverImage.alpha = 0F
                    ViewPager2.SCROLL_STATE_IDLE -> updateCoverImage()
                    ViewPager2.SCROLL_STATE_SETTLING -> { /* ignored */ }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        
        if (useComposeImplementation) {
            hideSystemBars()
        }

        if (!useComposeImplementation) {
            updateCoverImage()
        }
    }

    private fun updateCoverImage() {
        when (mBinding.viewPager.currentItem) {
            0 -> mBinding.coverImage.setImageResource(R.drawable.onboarding_secure_png)
            1 -> mBinding.coverImage.setImageResource(R.drawable.onboarding_archive_png)
            2 -> mBinding.coverImage.setImageResource(R.drawable.onboarding_verify_png)
            3 -> mBinding.coverImage.setImageResource(R.drawable.onboarding_encrypt_png)
        }
        mBinding.coverImage.alpha = 0F
        mBinding.coverImage.animate().setDuration(200L).alpha(1F).start()
    }

    private fun isFirstPage(): Boolean {
        return if (useComposeImplementation) false else mBinding.viewPager.currentItem <= 0
    }

    private fun isLastPage(): Boolean {
        return if (useComposeImplementation) {
            false
        } else {
            val pageCount: Int =
                if (mBinding.viewPager.adapter == null) 0 else mBinding.viewPager.adapter?.itemCount!!
            mBinding.viewPager.currentItem + 1 >= pageCount
        }
    }

    private fun done() {
        // Hide keyboard before finishing activity
        val imm = getSystemService(InputMethodManager::class.java)
        currentFocus?.let { view ->
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()  // Remove focus from any input field
        }

        Prefs.didCompleteOnboarding = true
        // We are moving space setup to MainActivity
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

// Data class for onboarding slides
data class OnboardingSlide(
    val titleRes: Int,
    val textRes: Int,
    val linkRes: Int? = null,
    val imageRes: Int
)

@Composable
fun OnboardingInstructionsScreen(
    onDone: () -> Unit,
    onBackPressed: () -> Unit
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
                        .padding(top = 40.dp, start = 8.dp, end = 8.dp) // Match XML positioning
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
            val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
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
            Spacer(modifier = Modifier
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
            .padding(start = 24.dp, end = 140.dp) // Increased to ensure text stays behind 120dp tertiary bar
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

// Convert Android Spanned (from HtmlCompat.fromHtml) to Compose AnnotatedString
// This ensures perfect parity with XML version which also uses HtmlCompat.fromHtml
private fun spannedToAnnotatedString(
    spanned: android.text.Spanned,
    color: Color,
    linkColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        val text = spanned.toString()
        append(text)

        // Get all spans from the Spanned text
        val spans = spanned.getSpans(0, spanned.length, Any::class.java)

        for (span in spans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)

            when (span) {
                // Handle URL spans (links)
                is android.text.style.URLSpan -> {
                    addLink(
                        LinkAnnotation.Url(
                            url = span.url,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        ),
                        start = start,
                        end = end
                    )
                }
                // Handle bold text
                is android.text.style.StyleSpan -> {
                    when (span.style) {
                        android.graphics.Typeface.BOLD -> {
                            addStyle(
                                SpanStyle(fontWeight = FontWeight.Bold),
                                start = start,
                                end = end
                            )
                        }
                        android.graphics.Typeface.ITALIC -> {
                            addStyle(
                                SpanStyle(fontStyle = FontStyle.Italic),
                                start = start,
                                end = end
                            )
                        }
                        android.graphics.Typeface.BOLD_ITALIC -> {
                            addStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic
                                ),
                                start = start,
                                end = end
                            )
                        }
                    }
                }
                // Handle underline
                is android.text.style.UnderlineSpan -> {
                    addStyle(
                        SpanStyle(textDecoration = TextDecoration.Underline),
                        start = start,
                        end = end
                    )
                }
            }
        }

        // Apply default color to all text
        addStyle(
            SpanStyle(color = color),
            start = 0,
            end = text.length
        )
    }
}

@Composable
fun HtmlText(
    textRes: Int,
    modifier: Modifier = Modifier,
    linkRes: Int? = null,
    color: Color = MaterialTheme.colorScheme.onBackground,
    linkColor: Color = MaterialTheme.colorScheme.onBackground,
    fontSize: TextUnit = 16.sp,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val text = stringResource(textRes)
    val linkText = linkRes?.let { stringResource(it) }

    if (linkText != null && text.contains("%1\$s")) {
        // Format text with link, just like XML version does
        val formattedText = text.format(linkText)

        // Use HtmlCompat.fromHtml() like XML version for perfect parity
        val spanned = androidx.core.text.HtmlCompat.fromHtml(
            formattedText,
            androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        // Convert Spanned to AnnotatedString
        val annotatedString = spannedToAnnotatedString(spanned, color, linkColor)

        // Use BasicText instead of deprecated ClickableText
        BasicText(
            text = annotatedString,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.Normal
            ),
            modifier = modifier
        )
    } else {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal)
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingInstructionsScreenPreviewLight() {
    SaveAppTheme {
        OnboardingInstructionsScreen(
            onDone = {},
            onBackPressed = {}
        )
    }
}


@Preview(name = "Single Slide Light", showBackground = true)
@Preview(name = "Single Slide Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingSlideContentPreviewLight() {
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

