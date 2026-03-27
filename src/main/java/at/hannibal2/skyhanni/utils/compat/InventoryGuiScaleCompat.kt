package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Minecraft

object InventoryGuiScaleCompat {

    private val mc get() = Minecraft.getInstance()

    fun <T> withOriginalHudScale(action: () -> T): T {
        val window = mc.window
        val currentScale = window.guiScale
        val originalScale = window.calculateScale(mc.options.guiScale().get(), mc.isEnforceUnicode)

        if (currentScale == originalScale) return action()

        val correctionScale = originalScale.toFloat() / currentScale.toFloat()
        val originalScaledWidth = ceilDiv(window.width, originalScale)
        val originalScaledHeight = ceilDiv(window.height, originalScale)

        return GuiScreenUtils.withScreenMetricsOverride(originalScaledWidth, originalScaledHeight, originalScale) {
            DrawContextUtils.pushPopResult {
                DrawContextUtils.scale(correctionScale, correctionScale)
                action()
            }
        }
    }

    private fun ceilDiv(value: Int, divisor: Int): Int = (value + divisor - 1) / divisor
}
