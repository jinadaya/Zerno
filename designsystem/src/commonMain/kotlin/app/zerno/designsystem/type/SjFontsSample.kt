package app.zerno.designsystem.type

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val SampleEn = "Thy word is a lamp unto my feet — Psalm 119, 38%"
private const val SampleRu = "Слово Твоё — светильник ноге моей. Псалом 118, ёйщъыэюя"

/** Every bundled face and weight with an English and a Russian line, to check that Cyrillic renders. */
@Composable
fun SjFontsSample(modifier: Modifier = Modifier) {
    val fonts = rememberSjFonts()
    val faces = listOf(
        Triple("Unbounded 600", fonts.display, FontWeight.SemiBold to FontStyle.Normal),
        Triple("Unbounded 700", fonts.display, FontWeight.Bold to FontStyle.Normal),
        Triple("Spectral 400", fonts.serif, FontWeight.Normal to FontStyle.Normal),
        Triple("Spectral 400 italic", fonts.serif, FontWeight.Normal to FontStyle.Italic),
        Triple("Spectral 600", fonts.serif, FontWeight.SemiBold to FontStyle.Normal),
        Triple("Onest 400", fonts.sans, FontWeight.Normal to FontStyle.Normal),
        Triple("Onest 500", fonts.sans, FontWeight.Medium to FontStyle.Normal),
        Triple("Onest 600", fonts.sans, FontWeight.SemiBold to FontStyle.Normal),
    )
    Column(
        modifier = modifier.background(Color.White).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        faces.forEach { (label, family, weightAndStyle) ->
            FaceSample(label, family, weightAndStyle.first, weightAndStyle.second)
        }
    }
}

@Composable
private fun FaceSample(label: String, family: FontFamily, weight: FontWeight, style: FontStyle) {
    val text = TextStyle(fontFamily = family, fontWeight = weight, fontStyle = style, fontSize = 18.sp)
    Column {
        BasicText(label, style = TextStyle(fontSize = 11.sp, color = Color.Gray))
        BasicText(SampleEn, style = text)
        BasicText(SampleRu, style = text)
    }
}

@Preview
@Composable
private fun SjFontsSamplePreview() {
    SjFontsSample()
}
