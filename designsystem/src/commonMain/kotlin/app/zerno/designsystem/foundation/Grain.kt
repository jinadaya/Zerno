package app.zerno.designsystem.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import kotlin.random.Random

/**
 * The film grain from `sj-grain`: a tiled noise texture at 22% opacity, multiplied over the colour below it and
 * kept inside [shape]. Put it after the background and before the content, so the grain sits between them:
 * `Modifier.background(color, shape).sjGrain(shape)`. It never clips the content, so a seal can overhang the edge.
 *
 * Every grain shares one small bitmap, made once on first use.
 */
fun Modifier.sjGrain(shape: Shape = RectangleShape): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(outline, GrainBrush, alpha = GrainOpacity, blendMode = BlendMode.Multiply)
}

private const val GrainOpacity = 0.22f
private const val TileSize = 160

/** Black specks with random alpha up to 0.55, as the SVG noise filter in bundle.css. Same seed every launch. */
private val GrainBrush: ShaderBrush by lazy {
    val bitmap = ImageBitmap(TileSize, TileSize)
    val canvas = Canvas(bitmap)
    val random = Random(GrainSeed)
    val buckets = List(AlphaSteps) { mutableListOf<Offset>() }
    for (y in 0 until TileSize) {
        for (x in 0 until TileSize) {
            buckets[random.nextInt(AlphaSteps)] += Offset(x + 0.5f, y + 0.5f)
        }
    }
    buckets.forEachIndexed { step, points ->
        val paint = Paint().apply {
            color = Color.Black.copy(alpha = MaxSpeckAlpha * step / (AlphaSteps - 1))
            strokeWidth = 1f
            strokeCap = StrokeCap.Square
        }
        canvas.drawPoints(PointMode.Points, points, paint)
    }
    ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
}

private const val GrainSeed = 7
private const val AlphaSteps = 8
private const val MaxSpeckAlpha = 0.55f
