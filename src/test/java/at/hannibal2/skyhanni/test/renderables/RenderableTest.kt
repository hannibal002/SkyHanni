package at.hannibal2.skyhanni.test.renderables

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.renderables.Renderable
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RenderableTest {

    @Test
    fun `hover bounds are half open`() {
        Renderable.withMousePosition(10, 20) {
            assertTrue(testRenderable.isHovered(10, 20))
            assertFalse(testRenderable.isHovered(0, 10))
        }
    }

    @Test
    fun `box hover bounds are half open`() {
        Renderable.withMousePosition(10, 20) {
            assertTrue(testRenderable.isBoxHovered(10, 10, 20, 10))
            assertFalse(testRenderable.isBoxHovered(0, 10, 10, 10))
        }
    }

    @Test
    fun `adjacent renderables do not share an edge hover`() {
        Renderable.withMousePosition(5, 10) {
            assertFalse(testRenderable.isHovered(0, 0))
            assertTrue(testRenderable.isHovered(0, 10))
        }
    }

    private companion object {
        val testRenderable = object : Renderable {
            override val width = 10
            override val height = 10
            override val horizontalAlign = HorizontalAlignment.LEFT
            override val verticalAlign = VerticalAlignment.TOP

            override fun render(mouseOffsetX: Int, mouseOffsetY: Int) = Unit
        }
    }
}
