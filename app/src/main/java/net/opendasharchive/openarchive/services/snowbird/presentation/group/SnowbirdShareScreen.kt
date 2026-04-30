package net.opendasharchive.openarchive.services.snowbird.presentation.group

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.components.LoadingOverlay
import net.opendasharchive.openarchive.core.presentation.theme.PreviewLightDark
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.extensions.asQRCode


@Composable
fun SnowbirdShareScreen(
    viewModel: SnowbirdShareViewModel,
    onShareQr: (String, String) -> Unit // qrContent, groupName — handled at SaveNavGraph level
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SnowbirdShareEvent.ShareQrImageExternal -> {
                    onShareQr(state.qrContent, state.groupName)
                }
            }
        }
    }

    SnowbirdShareScreenContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun SnowbirdShareScreenContent(
    state: SnowbirdShareState,
    onAction: (SnowbirdShareAction) -> Unit
) {
    val context = LocalContext.current
    val qrBitmap = remember(state.qrContent) {
        if (state.qrContent.isNotBlank()) {
            state.qrContent.asQRCode(
                onColor = android.graphics.Color.BLACK,
                offColor = android.graphics.Color.WHITE
            )
        } else null // Return null if content isn't ready yet
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Centered Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp), // Leave space for bottom bar
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Surface(
                modifier = Modifier
                    .fillMaxWidth() // Makes it as big as possible horizontally
                    .aspectRatio(1f) // Keeps it square
                    .clip(RoundedCornerShape(32.dp)),
                color = colorResource(R.color.white),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Group QR Code",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else if (state.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }


            Spacer(modifier = Modifier.height(12.dp))

            // Group Name
            Text(
                text = state.groupName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Join code with copy action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.qrContent,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    enabled = state.qrContent.isNotBlank() && !state.isLoading,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("DWeb Join Code", state.qrContent))
                        Toast.makeText(context, "Join code copied", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_document),
                        contentDescription = "Copy code"
                    )
                }
            }
        }

        // Bottom Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Button(
                modifier = Modifier
                    .height(ThemeDimensions.touchable)
                    .weight(1f),
                enabled = !state.isLoading,
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = colorResource(R.color.black)
                ),
                onClick = { onAction(SnowbirdShareAction.Cancel) }
            ) {
                Text(
                    stringResource(R.string.done),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        if (state.isLoading) {
            LoadingOverlay()
        }
    }
}

@PreviewLightDark
@Composable
private fun SnowbirdShareScreenPreview() {
    SaveAppTheme {
        SnowbirdShareScreenContent(
            state = SnowbirdShareState(
                groupName = "Test Collective",
                qrContent = "save+dweb::?dht=caf0a5ab51d936a0f6cbdb471feee9601714641523168359dce72fcdbb3394e3&enc=5af9ad74efce749b9b2bed692ecb16f71016729a6b11ea021d20f5f9186a1a7d&pk=0f083d9391c5bddf199320014abc9ab3f5b51c9e41adf97c0261e5be6b1ba41d&sk=e1525c792b78cd3aca849b3e2a1b6d3f7e08dd9339f3e872513b88400196c398",
            ),
            onAction = {}
        )
    }
}
