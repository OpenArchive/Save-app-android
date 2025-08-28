package net.opendasharchive.openarchive.features.media.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun CameraGridOverlay(
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokeWidth = with(density) { 1.dp.toPx() }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Vertical lines (rule of thirds)
        val verticalLine1X = width / 3f
        val verticalLine2X = (width * 2f) / 3f
        
        // Horizontal lines (rule of thirds)
        val horizontalLine1Y = height / 3f
        val horizontalLine2Y = (height * 2f) / 3f
        
        val gridColor = Color.White.copy(alpha = 0.5f)
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        
        // Draw vertical lines
        drawLine(
            color = gridColor,
            start = Offset(verticalLine1X, 0f),
            end = Offset(verticalLine1X, height),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
            pathEffect = pathEffect
        )
        
        drawLine(
            color = gridColor,
            start = Offset(verticalLine2X, 0f),
            end = Offset(verticalLine2X, height),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
            pathEffect = pathEffect
        )
        
        // Draw horizontal lines
        drawLine(
            color = gridColor,
            start = Offset(0f, horizontalLine1Y),
            end = Offset(width, horizontalLine1Y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
            pathEffect = pathEffect
        )
        
        drawLine(
            color = gridColor,
            start = Offset(0f, horizontalLine2Y),
            end = Offset(width, horizontalLine2Y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
            pathEffect = pathEffect
        )
    }
}