package net.opendasharchive.openarchive.features.core

import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat

@Immutable
sealed interface UiColor {

    @Immutable
    data class Dynamic(val color: Color) : UiColor

    @Immutable
    data class Resource(@param:ColorRes val resId: Int) : UiColor

    /**
     * Resolve into a Compose Color within the UI layer.
     */
    @Composable
    fun asColor(): Color = when (this) {
        is Dynamic -> color
        is Resource -> colorResource(resId)
    }

    /**
     * Resolve into a Compose Color using a Context (e.g., for Legacy Views or Services).
     */
    fun asColor(context: android.content.Context): Color = when (this) {
        is Dynamic -> color
        is Resource -> Color(ContextCompat.getColor(context, resId))
    }
}

// Helper Extensions
fun Color.asUiColor() = UiColor.Dynamic(this)
fun @receiver:ColorRes Int.asUiColor() = UiColor.Resource(this)