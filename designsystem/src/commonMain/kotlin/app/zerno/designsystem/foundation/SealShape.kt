package app.zerno.designsystem.foundation

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * The scalloped seal from `sj-seal` and `sj-badge` (24 soft points on a 100 x 100 grid), stretched to the size it
 * is drawn at. Used for the Today card's minutes seal, achievement seals and streak milestones.
 */
object SealShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = addPathNodes(SealPath).toPath()
        path.transform(Matrix().apply { scale(size.width / SealGrid, size.height / SealGrid) })
        return Outline.Generic(path)
    }
}

private const val SealGrid = 100f

private const val SealPath = "M98.0,50.0 Q98.0,50.0 94.5,54.7 Q90.9,59.3 92.1,65.1 Q93.2,70.8 88.0,73.5 " +
    "Q82.8,76.2 81.4,81.9 Q79.9,87.5 74.1,87.7 Q68.2,87.8 64.5,92.3 Q60.7,96.8 55.3,94.4 Q50.0,92.0 44.7,94.4 " +
    "Q39.3,96.8 35.5,92.3 Q31.8,87.8 25.9,87.7 Q20.1,87.5 18.6,81.9 Q17.2,76.2 12.0,73.5 Q6.8,70.8 7.9,65.1 " +
    "Q9.1,59.3 5.5,54.7 Q2.0,50.0 5.5,45.3 Q9.1,40.7 7.9,34.9 Q6.8,29.2 12.0,26.5 Q17.2,23.8 18.6,18.1 " +
    "Q20.1,12.5 25.9,12.3 Q31.8,12.2 35.5,7.7 Q39.3,3.2 44.7,5.6 Q50.0,8.0 55.3,5.6 Q60.7,3.2 64.5,7.7 " +
    "Q68.2,12.2 74.1,12.3 Q79.9,12.5 81.4,18.1 Q82.8,23.8 88.0,26.5 Q93.2,29.2 92.1,34.9 Q90.9,40.7 94.5,45.3 Z"
