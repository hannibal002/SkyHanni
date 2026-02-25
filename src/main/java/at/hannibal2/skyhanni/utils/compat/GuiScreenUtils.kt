package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Minecraft

object GuiScreenUtils {

    private val mc get() = Minecraft.getInstance()

    val scaledWindowHeight: Int
        get() = mc.window.guiScaledHeight

    val scaledWindowWidth: Int
        get() = mc.window.guiScaledWidth

    val displayWidth: Int
        get() = mc.window.width

    val displayHeight: Int
        get() = mc.window.height

    val scaleFactor: Int
        get() = mc.window.guiScale.toInt()

    private val globalMouseX get() = MouseCompat.getX()
    private val globalMouseY get() = MouseCompat.getY()

    val mouseX: Int
        get() {
            var x = globalMouseX * scaledWindowWidth / displayWidth
            if (mc.window.width > mc.window.screenWidth) x *= 2
            return x
        }

    val mouseY: Int
        get() {
            val height = this.scaledWindowHeight
            var y = globalMouseY * height / displayHeight
            if (mc.window.height > mc.window.screenHeight) y *= 2
            return y
        }

    val mousePos: Pair<Int, Int> get() = mouseX to mouseY
}
