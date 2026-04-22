package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Minecraft

object GuiScreenUtils {

    private val mc get() = Minecraft.getInstance()
    private var screenMetricsOverride: ScreenMetricsOverride? = null

    private data class ScreenMetricsOverride(
        val scaledWindowWidth: Int,
        val scaledWindowHeight: Int,
        val scaleFactor: Int,
    )

    val scaledWindowHeight: Int
        get() = screenMetricsOverride?.scaledWindowHeight ?: mc.window.guiScaledHeight

    val scaledWindowWidth: Int
        get() = screenMetricsOverride?.scaledWindowWidth ?: mc.window.guiScaledWidth

    val displayWidth: Int
        get() = mc.window.width

    val displayHeight: Int
        get() = mc.window.height

    val scaleFactor: Int
        get() = screenMetricsOverride?.scaleFactor ?: mc.window.guiScale.toInt()

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

    fun <T> withScreenMetricsOverride(
        scaledWindowWidth: Int,
        scaledWindowHeight: Int,
        scaleFactor: Int,
        action: () -> T,
    ): T {
        val previousOverride = screenMetricsOverride
        screenMetricsOverride = ScreenMetricsOverride(scaledWindowWidth, scaledWindowHeight, scaleFactor)
        return try {
            action()
        } finally {
            screenMetricsOverride = previousOverride
        }
    }
}
