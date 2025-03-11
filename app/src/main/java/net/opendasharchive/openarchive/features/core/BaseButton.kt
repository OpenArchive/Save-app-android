package net.opendasharchive.openarchive.features.core

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.core.presentation.theme.DefaultBoxPreview

@Composable
fun BaseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onPrimary,
    cornerRadius: Dp = 12.dp,
) {

    Button(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(cornerRadius),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    ) {
        ButtonText(text, color = textColor)
    }
}

@Composable
fun BaseNeutralButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onPrimary,
) {

    TextButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        ButtonText(text, color = textColor)
    }
}

@Composable
fun BaseDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.error,
    textColor: Color = MaterialTheme.colorScheme.error,
    cornerRadius: Dp = 12.dp,
) {

    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
    ) {
        ButtonText(
            text,
            color = textColor
        )
    }
}


@Composable
fun ButtonText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    color: Color = MaterialTheme.colorScheme.onPrimary
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color
    ))
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CustomButtonPreview() {
    DefaultBoxPreview {

        BaseButton(
            text = "Submit",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CustomNeutralButtonPreview() {
    DefaultBoxPreview {

        BaseNeutralButton(
            text = "Cancel",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CustomDestructiveButtonPreview() {
    DefaultBoxPreview {

        BaseDestructiveButton(
            text = "Delete",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}