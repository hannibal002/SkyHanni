package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent
import at.hannibal2.skyhanni.events.render.gui.ScreenDrawnEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SignUtils
import at.hannibal2.skyhanni.utils.SignUtils.isSupercraftAmountSetSign
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.gui.screens.inventory.SignEditScreen
import java.awt.Color

@SkyHanniModule
object SuperCraftPresets {

    private val config get() = SkyHanniMod.feature.inventory.superCrafting.presets

    private var display: Renderable? = null

    private const val BUTTONS_PER_ROW = 4

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiOpen(event: GuiScreenOpenEvent) {
        display = null
        if (!config.enabled) return
        val gui = event.gui as? SignEditScreen ?: return
        if (!gui.isSupercraftAmountSetSign()) return

        val presets = parsePresets()
        if (presets.isEmpty()) return

        val title = Renderable.text(
            "§7Supercraft Presets",
            horizontalAlign = HorizontalAlignment.CENTER,
        )
        val buttonRows = presets.chunked(BUTTONS_PER_ROW).map { row ->
            Renderable.horizontal(
                row.map { amount -> createButton(amount) },
                spacing = 3,
                horizontalAlign = HorizontalAlignment.CENTER,
            )
        }

        display = Renderable.drawInsideRoundedRect(
            Renderable.vertical(
                listOf(title) + buttonRows,
                spacing = 3,
                horizontalAlign = HorizontalAlignment.CENTER,
            ),
            color = Color(32, 32, 32, 128),
            padding = 5,
            radius = 6,
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onScreenDrawn(event: ScreenDrawnEvent) {
        if (!config.enabled) return
        val gui = event.gui as? SignEditScreen ?: return
        if (!gui.isSupercraftAmountSetSign()) return
        config.signPosition.renderRenderable(
            display,
            posLabel = "Supercraft Presets",
        )
    }

    private fun createButton(amount: Int): Renderable {
        val label = Renderable.text(
            " §e${amount.addSeparators()} ",
            horizontalAlign = HorizontalAlignment.CENTER,
        )
        val button = Renderable.drawInsideRoundedRect(
            label,
            color = Color(255, 255, 255, 0),
            padding = 4,
            radius = 4,
        )
        val hoverButton = Renderable.drawInsideRoundedRect(
            label,
            color = Color(255, 255, 255, 255),
            padding = 4,
            radius = 4,
        )
        return Renderable.clickable(
            Renderable.hoverable(hoverButton, button),
            onLeftClick = { setPresetAmount(amount) },
        )
    }

    private fun setPresetAmount(amount: Int) {
        SignUtils.setTextIntoSign("$amount", 0)
    }

    private fun parsePresets(): List<Int> = config.presets
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it > 0 }
        .distinct()
}




