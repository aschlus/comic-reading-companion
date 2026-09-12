package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.shape.GenericShape

val ComicSlantedShape =
    GenericShape { size, _ ->
        val slant =
            size.width * 0.035f

        moveTo(
            slant,
            0f
        )

        lineTo(
            size.width,
            0f
        )

        lineTo(
            size.width - slant,
            size.height
        )

        lineTo(
            0f,
            size.height
        )

        close()
    }