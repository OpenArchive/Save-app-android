package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
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
            
            // Skip button (top) - positioned well above images to avoid any overlap
            if (!isLastPage) {
                TextButton(
                    onClick = onDone,
                    modifier = Modifier
                        .padding(top = 80.dp, start = 8.dp, end = 8.dp) // Much higher to avoid image overlap
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stringResource(R.string.skip),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // FAB (bottom) - with navigation bar padding and proper color
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
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = LocalColors.current.primaryBright, // Use theme color
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                Icon(
                    imageVector = if (isLastPage) Icons.Default.Done else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = if (isLastPage) "Done" else stringResource(R.string.next)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(15f)
                    .padding(horizontal = 24.dp)
            ) {
                // Dots indicator (aligned to start|center_vertical)
                DotsIndicator(
                    totalDots = slides.size,
                    selectedIndex = currentPage,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
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
            // Title
            Text(
                text = stringResource(slide.titleRes).uppercase(),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
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

@Composable
fun DotsIndicator(
    totalDots: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        items(totalDots) { index ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == selectedIndex) 
                            MaterialTheme.colorScheme.onBackground
                        else 
                            Color(0xFF666666) // c23_medium_grey equivalent
                    )
            )
        }
    }
}

@Composable
fun HtmlText(
    textRes: Int,
    linkRes: Int? = null,
    color: Color = MaterialTheme.colorScheme.onBackground,
    linkColor: Color = MaterialTheme.colorScheme.onBackground,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    
    val text = stringResource(textRes)
    val linkText = linkRes?.let { stringResource(it) }
    
    if (linkText != null && text.contains("%1\$s")) {
        val formattedText = text.format(linkText)
        
        // Parse HTML-like tags manually for basic support
        val annotatedString = buildAnnotatedString {
            var currentIndex = 0
            val htmlTagRegex = Regex("<[^>]+>")
            val linkRegex = Regex("<a href=\"([^\"]+)\"><u>([^<]+)</u></a>")
            
            // Find all matches
            val matches = linkRegex.findAll(formattedText)
            
            for (match in matches) {
                val beforeMatch = formattedText.substring(currentIndex, match.range.first)
                val url = match.groupValues[1]
                val linkText = match.groupValues[2]
                
                // Add text before the link
                withStyle(SpanStyle(color = color)) {
                    append(beforeMatch.replace(Regex("<[^>]+>"), ""))
                }
                
                // Add the clickable link
                pushStringAnnotation(tag = "URL", annotation = url)
                withStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(linkText)
                }
                pop()
                
                currentIndex = match.range.last + 1
            }
            
            // Add remaining text
            if (currentIndex < formattedText.length) {
                withStyle(SpanStyle(color = color)) {
                    append(formattedText.substring(currentIndex).replace(Regex("<[^>]+>"), ""))
                }
            }
        }
        
        ClickableText(
            text = annotatedString,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = fontSize,
                color = color
            ),
            modifier = modifier,
            onClick = { offset ->
                annotatedString.getStringAnnotations(
                    tag = "URL", 
                    start = offset, 
                    end = offset
                ).firstOrNull()?.let { annotation ->
                    uriHandler.openUri(annotation.item)
                }
            }
        )
    } else {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            modifier = modifier
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun OnboardingInstructionsScreenPreviewLight() {
    SaveAppTheme {
        OnboardingInstructionsScreen(
            onDone = {},
            onBackPressed = {}
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingInstructionsScreenPreviewDark() {
    SaveAppTheme {
        OnboardingInstructionsScreen(
            onDone = {},
            onBackPressed = {}
        )
    }
}

@Preview(name = "Single Slide Light", showBackground = true)
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

@Preview(name = "Single Slide Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingSlideContentPreviewDark() {
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

@Preview(name = "Dots Indicator", showBackground = true)
@Composable
private fun DotsIndicatorPreview() {
    SaveAppTheme {
        DotsIndicator(
            totalDots = 4,
            selectedIndex = 1,
            modifier = Modifier.padding(16.dp)
        )
    }
}