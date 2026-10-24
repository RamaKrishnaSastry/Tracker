package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoResizedText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    minFontSize: TextUnit = 8.sp,
    textAlign: TextAlign? = null
) {
    var resizedTextStyle by remember(text, style) { mutableStateOf(style) }
    var shouldDraw by remember(text, style) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        textAlign = textAlign,
        modifier = modifier.drawWithContent {
            if (shouldDraw) {
                drawContent()
            }
        },
        style = resizedTextStyle,
        maxLines = maxLines,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if ((result.didOverflowWidth || result.didOverflowHeight) && resizedTextStyle.fontSize > minFontSize) {
                val nextSize = (resizedTextStyle.fontSize.value * 0.88f).sp
                resizedTextStyle = resizedTextStyle.copy(
                    fontSize = if (nextSize < minFontSize) minFontSize else nextSize
                )
            } else {
                shouldDraw = true
            }
        }
    )
}
