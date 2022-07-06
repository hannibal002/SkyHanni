package at.lorenz.mod.utils

import net.minecraft.client.gui.Gui
import net.minecraft.inventory.Slot

class RenderUtil {

    companion object {

        infix fun Slot.highlight(color: LorenzColor) {
            Gui.drawRect(
                this.xDisplayPosition,
                this.yDisplayPosition,
                this.xDisplayPosition + 16,
                this.yDisplayPosition + 16,
                color.toColor().rgb
            )
        }
    }
}