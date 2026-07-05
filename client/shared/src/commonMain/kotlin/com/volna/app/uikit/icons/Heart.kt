package com.volna.app.uikit.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Heart: ImageVector
    get() {
        if (_Heart != null) {
            return _Heart!!
        }
        _Heart = ImageVector.Builder(
            name = "Heart",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            path(fill = SolidColor(Color(0xFF797979))) {
                moveTo(8.0f, 14.4f)
                lineTo(7.2f, 13.7f)
                curveTo(3.4f, 10.1f, 1.0f, 7.9f, 1.0f, 5.1f)
                curveTo(1.0f, 3.1f, 2.5f, 1.6f, 4.5f, 1.6f)
                curveTo(5.7f, 1.6f, 6.8f, 2.1f, 7.5f, 3.0f)
                curveTo(8.2f, 2.1f, 9.3f, 1.6f, 10.5f, 1.6f)
                curveTo(12.5f, 1.6f, 14.0f, 3.1f, 14.0f, 5.1f)
                curveTo(14.0f, 7.9f, 11.6f, 10.1f, 7.8f, 13.7f)
                lineTo(7.0f, 14.4f)
                close()
            }
        }.build()
        return _Heart!!
    }

@Suppress("ObjectPropertyName")
private var _Heart: ImageVector? = null
