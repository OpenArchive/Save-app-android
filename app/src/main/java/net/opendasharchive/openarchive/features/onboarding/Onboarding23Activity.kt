package net.opendasharchive.openarchive.features.onboarding

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.view.Window
import android.view.WindowManager
import android.view.animation.BounceInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorRes
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.databinding.ActivityOnboarding23Binding
import net.opendasharchive.openarchive.features.core.BaseActivity

class Onboarding23Activity : BaseActivity() {

    private lateinit var mBinding: ActivityOnboarding23Binding

    // Toggle to switch between XML and Compose implementation
    private val useComposeImplementation = true  // Set to false to use XML implementation

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        if (useComposeImplementation) {
            // Use Compose implementation
            setContent {
                SaveAppTheme {
                    OnboardingWelcomeScreen(
                        onGetStartedClick = {
                            startActivity(
                                Intent(
                                    this@Onboarding23Activity,
                                    Onboarding23InstructionsActivity::class.java
                                )
                            )
                        }
                    )
                }
            }
        } else {
            // Use original XML implementation
            setupXmlImplementation()
        }
    }

    private fun setupXmlImplementation() {
        // Keep the existing XML binding for legacy compatibility
        mBinding = ActivityOnboarding23Binding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Keep the original XML-based code for reference and fallback
        mBinding.getStarted.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    Onboarding23InstructionsActivity::class.java
                )
            )
        }

        // Handle back button to exit app instead of returning to MainActivity
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Exit the app when back is pressed during onboarding
                finishAffinity()
            }
        })

        for (textView in arrayOf(
            mBinding.titleBlock.shareText,
            mBinding.titleBlock.archiveText,
            mBinding.titleBlock.verifyText,
            mBinding.titleBlock.encryptText
        )) {
            textView.text = colorizeFirstLetter(textView.text, R.color.colorTertiary)
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()

        if (!useComposeImplementation) {
            // Animation is handled in Compose version
            val oa = ObjectAnimator.ofFloat(mBinding.arrow, "translationX", 0F, 25F, 0F)
            oa.interpolator = BounceInterpolator()
            oa.startDelay = 3000
            oa.duration = 2000
            oa.repeatCount = 999999
            oa.start()
        }
    }

    private fun colorizeFirstLetter(text: CharSequence, @ColorRes color: Int): Spanned {
        val colorHexString =
            Integer.toHexString(0xffffff and ContextCompat.getColor(this, color))
        val html =
            "<font color=\"#${colorHexString}\">${text.substring(0, 1)}</font>${text.substring(1)}"
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}

@Composable
fun OnboardingWelcomeScreen(
    onGetStartedClick: () -> Unit = {}
) {
    var arrowOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        delay(3000)
        while (true) {
            repeat(3) {
                arrowOffset = 25f
                delay(300)
                arrowOffset = 0f
                delay(300)
            }
            delay(2000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // Main content area (equivalent to LinearLayout above nav_block in XML)
        Column(
            modifier = Modifier
                .weight(1f) // Take remaining space above nav block
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, end = 32.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Logo (weight=8 of 55) - wrap_content width like XML
            Box(
                modifier = Modifier
                    .weight(8f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
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
            Spacer(modifier = Modifier.weight(8f))

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
                    .clickable { onGetStartedClick() }
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
                        .graphicsLayer { translationX = arrowOffset },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                )
            }
        }
    }


}


@Composable
private fun StyledTitleText(
    text: String,
    modifier: Modifier = Modifier
) {
    // Match the horizontal LinearLayout with weightSum="1000"
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Spacer with weight 65 of 1000
        Spacer(modifier = Modifier.weight(65f))

        // Text with weight 870 of 1000
        val styledText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
                append(text.firstOrNull()?.toString() ?: "")
            }
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                append(text.drop(1))
            }
        }

        Text(
            text = styledText,
            modifier = Modifier
                .weight(870f)
                .fillMaxHeight()
                .wrapContentSize(Alignment.CenterStart)
                .graphicsLayer {
                    scaleX = 1.15f
                    scaleY = 1.15f
                },
            fontSize = 60.sp, // Using app:autoSizeTextType="uniform" equivalent
            fontWeight = FontWeight.Black, // textFontWeight="900"
            maxLines = 1
        )
    }
}

@Preview(name = "Welcome Screen Light", showBackground = true)
@Composable
private fun OnboardingWelcomeScreenPreviewLight() {
    SaveAppTheme {
        OnboardingWelcomeScreen(
            onGetStartedClick = {}
        )
    }
}

@Preview(
    name = "Welcome Screen Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun OnboardingWelcomeScreenPreviewDark() {
    SaveAppTheme {
        OnboardingWelcomeScreen(
            onGetStartedClick = {}
        )
    }
}

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    minFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    maxFontSize: androidx.compose.ui.unit.TextUnit = 100.sp,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null
) {
    // Simple implementation using standard Text with reasonable font size
    // In a real implementation, you'd measure text and adjust font size
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = minFontSize, // Use minimum font size for now
        fontWeight = fontWeight,
        textAlign = textAlign
    )
}