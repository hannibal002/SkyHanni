package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ModernPatterns
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import net.minecraft.entity.decoration.ArmorStandEntity

@SkyHanniModule
object TreeProgressDisplay {

    private val config get() = SkyHanniMod.feature.foraging.trees.progress
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
                    display = StringRenderable("${group("treeType")} §b§l${group("percent")}%")
                } else {
                    display = StringRenderable(name)
                }
                return

            }
        }
        display = null
    }
}
