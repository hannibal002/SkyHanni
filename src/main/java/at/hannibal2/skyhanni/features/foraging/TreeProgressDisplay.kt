package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object TreeProgressDisplay {

    private val config get() = SkyHanniMod.feature.foraging.trees.progress
    private var display: Renderable? = null

    /**
     * REGEX-TEST: FIG TREE 88%
     * REGEX-TEST: MANGROVE TREE 5%
     */
    private val currentTreeProgressPattern by RepoPattern.pattern(
        "foraging.tree.progress.colorless",
        "(?<treeType>\\w+) TREE (?<percent>\\d+)%",
    )

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    fun onGuiRenderOverlay() {
        if (!config.enabled) return
        display?.let {
            config.position.renderRenderable(it, posLabel = "Tree Progress")
        }
    }

    // TODO: optimize to not use getEntities
    @OptIn(AllEntitiesGetter::class)
    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    fun onTick() {
        if (!config.enabled) return
        if (config.onlyHoldingAxe && InventoryUtils.getItemInHand()?.getItemCategoryOrNull() != ItemCategory.AXE) {
            display = null
            return
        }
        for (entity in EntityUtils.getEntities<ArmorStand>()) {
            val name = entity.displayName.formattedTextCompat().removeColor()
            currentTreeProgressPattern.matchMatcher(name) {
                display = if (config.compact) {
                    Renderable.text("${group("treeType")} §b§l${group("percent")}%")
                } else {
                    Renderable.text(name)
                }
                return

            }
        }
        display = null
    }
}
