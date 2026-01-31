package net.opendasharchive.openarchive.core.presentation.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.MontserratFontFamily
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.EvidenceStatus

/**
 * Shared media status overlay component for showing upload states.
 * Used in both PreviewMedia (grid) and UploadManager (list) screens.
 *
 * @param media The media item to show status for
 * @param modifier Modifier for the overlay container
 * @param showProgressText Whether to show percentage text for uploading state
 * @param backgroundColor Background color for the overlay
 * @param progressIndicatorSize Size of the circular progress indicator
 * @param showQueuedState Whether to show overlay for queued state
 * @param showUploadingState Whether to show overlay for uploading state
 */
@Composable
fun MediaStatusOverlay(
    evidence: Evidence,
    modifier: Modifier = Modifier,
    showProgressText: Boolean = true,
    backgroundColor: Color = colorResource(R.color.transparent_loading_overlay),
    progressIndicatorSize: Int = 42,
    showQueuedState: Boolean = true,
    showUploadingState: Boolean = true
) {
    when (evidence.status) {
        EvidenceStatus.ERROR -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_error),
                    contentDescription = stringResource(R.string.error),
                    tint = colorResource(R.color.colorDanger),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        EvidenceStatus.QUEUED -> {
            if (showQueuedState) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(progressIndicatorSize.dp),
                        strokeWidth = 4.dp
                    )
                }
            }
        }

        EvidenceStatus.UPLOADING -> {
            if (showUploadingState) {
                val progressValue = evidence.uploadPercentage
                    ?: if (evidence.contentLength > 0) (evidence.progress.toFloat() / evidence.contentLength * 100).toInt() else 0
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (progressValue > 2) {
                        CircularProgressIndicator(
                            progress = { progressValue / 100f },
                            color = MaterialTheme.colorScheme.tertiary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(progressIndicatorSize.dp),
                            strokeWidth = 4.dp
                        )
                    } else {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(progressIndicatorSize.dp),
                            strokeWidth = 4.dp
                        )
                    }

                    if (showProgressText) {
                        Text(
                            text = "$progressValue%",
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = MontserratFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                }
            }
        }

        else -> Unit
    }
}
