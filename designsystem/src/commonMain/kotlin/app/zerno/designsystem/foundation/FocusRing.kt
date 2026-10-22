package app.zerno.designsystem.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.theme.LocalSjColors

/**
 * The keyboard focus ring from bundle.css: a 2 dp `ultra` outline 3 dp outside [shape], shown while the element
 * after it in the chain is focused. Put it before the focus target: `Modifier.sjFocusRing(shape).clickable { }`.
 */
fun Modifier.sjFocusRing(shape: Shape = RectangleShape): Modifier = this then FocusRingElement(shape)

private data class FocusRingElement(val shape: Shape) : ModifierNodeElement<FocusRingNode>() {
    override fun create(): FocusRingNode = FocusRingNode(shape)

    override fun update(node: FocusRingNode) {
        node.shape = shape
        node.invalidateDraw()
    }
}

private class FocusRingNode(var shape: Shape) :
    Modifier.Node(),
    FocusEventModifierNode,
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {

    private var focused = false

    override fun onFocusEvent(focusState: FocusState) {
        if (focusState.isFocused != focused) {
            focused = focusState.isFocused
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (!focused) return
        val stroke = RingWidth.toPx()
        val inset = RingOffset.toPx() + stroke / 2
        val outline = shape.createOutline(Size(size.width + inset * 2, size.height + inset * 2), layoutDirection, this)
        translate(-inset, -inset) {
            drawOutline(outline, currentValueOf(LocalSjColors).ultra, style = Stroke(stroke))
        }
    }
}

private val RingWidth = 2.dp
private val RingOffset = 3.dp
