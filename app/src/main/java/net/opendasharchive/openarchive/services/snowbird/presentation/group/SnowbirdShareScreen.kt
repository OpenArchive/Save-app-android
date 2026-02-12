package net.opendasharchive.openarchive.services.snowbird.presentation.group

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.components.LoadingOverlay
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.extensions.asQRCode
import org.koin.androidx.compose.koinViewModel

@Composable
fun SnowbirdShareScreen(
    groupKey: String,
    onCancel: () -> Unit,
    onShareQr: (String, String) -> Unit, // qrContent, groupName
    viewModel: SnowbirdShareViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(groupKey) {
        viewModel.onAction(SnowbirdShareAction.LoadGroup(groupKey))
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SnowbirdShareEvent.NavigateBack -> onCancel()
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
    val qrBitmap = remember(state.qrContent) {
        if (state.qrContent.isNotBlank()) {
            state.qrContent.asQRCode(size = 1024)
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Centered Content Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 100.dp), // Leave space for bottom bar
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // QR Code Container
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp)),
                color = colorResource(R.color.white),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Group QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (state.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Group Name
            Text(
                text = state.groupName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // QR Content/URI
            Text(
                text = state.qrContent,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
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

@Preview(showBackground = true)
@Composable
private fun SnowbirdShareScreenPreview() {
    SaveAppTheme {
        SnowbirdShareScreenContent(
            state = SnowbirdShareState(
                groupName = "Test Collective",
                qrContent = "some-uri-content"
            ),
            onAction = {}
        )
    }
}
