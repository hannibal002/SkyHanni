package at.hannibal2.hanni.features.foraging

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemCategory
import at.hannibal2.hanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.hanni.utils.ModernPatterns
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.RenderUtils.renderRenderable
import at.hannibal2.hanni.utils.compat.formattedTextCompat
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.primitives.text
import net.minecraft.entity.decoration.ArmorStandEntity

@HanniModule
object TreeProgressDisplay {

    private val config get() = HanniMod.feature.foraging.trees.progress
    private var display: Renderable? = null

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (display == null) return
        config.position.renderRenderable(display, posLabel = "Tree Progress")
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onTick() {
        if (!config.enabled) return
        if (config.onlyHoldingAxe && InventoryUtils.getItemInHand()?.getItemCategoryOrNull() != ItemCategory.AXE) {
            display = null
            return
        }
        for (entity in EntityUtils.getAllEntities()) {
            if (entity !is ArmorStandEntity) continue
            val name = entity.displayName.formattedTextCompat()
            ModernPatterns.currentTreeProgressPattern.matchMatcher(name) {
                if (config.compact) {
                    display = Renderable.text("${group("treeType")} §b§l${group("percent")}%")
                } else {
                    display = Renderable.text(name)
                }
                return

            }
        }
        display = null
    }
}
