package at.hannibal2.skyhanni.utils.renderables.interactables

import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationLocalStorage
import at.hannibal2.skyhanni.utils.renderables.decorators.RenderableDecoratorOnlyRender
import net.minecraft.core.Direction.Axis

/**
 * A renderable decorator that lets the developer rotate [root] by clicking and dragging.
 * Horizontal drag adjusts yaw (Y-axis); vertical drag adjusts pitch (X-axis).
 * Drag state is captured only when the cursor is inside the renderable bounds on press.
 *
 * @param root The inner renderable to display and rotate.
 * @param rotationStorage The rotation storage whose [AnimatedRotationLocalStorage.currentRotation]
 *   is updated while dragging. Must use zero rotation speed so automatic rotation does not fight
 *   the drag.
 * @param sensitivity Degrees of rotation applied per pixel of cursor movement.
 */
class RotatableDragRenderable private constructor(
    override val root: Renderable,
    private val rotationStorage: AnimatedRotationLocalStorage,
    private val sensitivity: Double,
) : RenderableDecoratorOnlyRender {

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        val isHovered = mouseOffsetX in 0..<width && mouseOffsetY in 0..<height
        val isMouseHeld = KeyboardManager.LEFT_MOUSE.isKeyHeld()

        when {
            isHovered && isMouseHeld && !rotationStorage.isDragging -> {
                rotationStorage.isDragging = true
                rotationStorage.dragStartX = mouseOffsetX
                rotationStorage.dragStartY = mouseOffsetY
                rotationStorage.dragStartRotX = rotationStorage.currentRotation[Axis.X]
                rotationStorage.dragStartRotY = rotationStorage.currentRotation[Axis.Y]
            }
            !isMouseHeld -> rotationStorage.isDragging = false
        }

        if (rotationStorage.isDragging) {
            val deltaX = (mouseOffsetX - rotationStorage.dragStartX) * sensitivity
            val deltaY = (mouseOffsetY - rotationStorage.dragStartY) * sensitivity
            rotationStorage.currentRotation = rotationStorage.currentRotation
                .applyAxisValue(Axis.Y, rotationStorage.dragStartRotY + deltaX)
                .applyAxisValue(Axis.X, rotationStorage.dragStartRotX - deltaY)
        }

        root.render(mouseOffsetX, mouseOffsetY)
    }

    companion object {
        /**
         * Wraps [root] in a [RotatableDragRenderable] that updates [rotationStorage] as the
         * developer clicks and drags over the renderable.
         *
         * @param root The inner renderable to display.
         * @param rotationStorage The rotation storage to update on drag.
         * @param sensitivity Degrees of rotation per pixel dragged.
         */
        fun Renderable.Companion.rotatableDrag(
            root: Renderable,
            rotationStorage: AnimatedRotationLocalStorage,
            sensitivity: Double = 1.0,
        ) = RotatableDragRenderable(root, rotationStorage, sensitivity)
    }
}
