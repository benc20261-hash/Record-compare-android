package com.example.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Single-line horizontally scrollable (marquee) button text.
 * Strictly prevents buttons from wrapping lines or ballooning in height,
 * ensuring all button UI remains compact, uniform, and predictable across all screen sizes.
 */
@Composable
fun ButtonText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 12.sp,
    fontWeight: FontWeight? = FontWeight.SemiBold,
    textAlign: TextAlign? = TextAlign.Center,
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier.basicMarquee(
            iterations = Int.MAX_VALUE,
            initialDelayMillis = 800,
            repeatDelayMillis = 800,
            velocity = 35.dp
        )
    )
}
