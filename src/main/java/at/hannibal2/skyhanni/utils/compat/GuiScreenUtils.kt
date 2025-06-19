package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Minecraft
//#if MC < 1.16
import net.minecraft.client.gui.ScaledResolution
//#endif

object GuiScreenUtils {

    private val mc get() = Minecraft.getMinecraft()

    val scaledWindowHeight: Int
        get() =
//#if MC < 1.16
            ScaledResolution(mc).scaledHeight
//#else
//$$            mc.window.guiScaledHeight.toInt()
//#endif

    val scaledWindowWidth: Int
        get() =
//#if MC < 1.16
            ScaledResolution(mc).scaledWidth
//#else
//$$            mc.window.guiScaledWidth.toInt()
//#endif

    val displayWidth: Int
        get() =
//#if MC < 1.16
            mc.displayWidth
//#else
//$$            mc.window.width.toInt()
//#endif

    val displayHeight: Int
        get() =
//#if MC < 1.16
            mc.displayHeight
//#else
//$$            mc.window.height.toInt()
//#endif

    val scaleFactor: Int
        get() =
//#if MC < 1.16
            ScaledResolution(mc).scaleFactor
//#else
//$$            mc.window.scaleFactor.toInt()
//#endif

    private val globalMouseX get() = MouseCompat.getX()
    private val globalMouseY get() = MouseCompat.getY()

    val mouseX get() = globalMouseX * scaledWindowWidth / displayWidth

    val mouseY: Int
        get() {
            val height = this.scaledWindowHeight
            // TODO: in later versions the height - factor is removed, i think
            val y = globalMouseY * height / displayHeight
//#if MC < 1.16
            return height - y - 1
//#else
//$$            return y
//#endif
        }

    val mousePos: Pair<Int, Int> get() = mouseX to mouseY
}
