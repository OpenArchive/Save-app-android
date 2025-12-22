package net.opendasharchive.openarchive.features.core

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource

@Immutable
sealed interface UiImage {
    data class DynamicVector(val imageVector: ImageVector) : UiImage
    data class DrawableResource(@param:DrawableRes val resId: Int) : UiImage


    /**
     * Resolve UiImage into a Composable function that returns an Icon/Image
     * Instead of directly rendering inside, this provides flexibility for additional customizations.
     */
    @Composable
    fun asIcon(
        contentDescription: String? = null,
        tint: Color = Color.Unspecified,
        modifier: Modifier = Modifier
    ): @Composable () -> Unit {
        return {
            when (this) {
                is DynamicVector -> Icon(
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    modifier = modifier,
                    tint = tint
                )

                is DrawableResource -> Icon(
                    painter = painterResource(id = resId),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    tint = tint
                )
            }
        }
    }

}


fun @receiver:DrawableRes Int.asUiImage(): UiImage.DrawableResource {
    return UiImage.DrawableResource(this)
}

fun ImageVector.asUiImage(): UiImage {
    return UiImage.DynamicVector(this)
}