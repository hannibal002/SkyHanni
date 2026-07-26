package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.events.entity.TextDisplayRemovedEvent
import at.hannibal2.skyhanni.events.entity.TextDisplayUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ComponentMatcher
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.matchStyledMatcher
import at.hannibal2.skyhanni.utils.ConditionalUtils.onDisable
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component

@SkyHanniModule
object TreeProgressDisplay {

    private val config get() = SkyHanniMod.feature.foraging.trees.progress
    private var display: Renderable? = null
    private var displayEntityId: Int? = null

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
        if (!config.enabled.get()) return
        display?.let {
            config.position.renderRenderable(it, posLabel = "Tree Progress")
        }
    }

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    fun onIslandJoin() {
        display = null
    }

    @HandleEvent
    fun onConfigLoad() {
        config.enabled.onDisable {
            display = null
        }
    }

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    fun onTextDisplayRemoved(event: TextDisplayRemovedEvent) {
        if (!config.enabled.get()) return
        if (event.entity.id == displayEntityId) {
            display = null
            displayEntityId = null
        }
    }

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    fun onTextDisplayUpdate(event: TextDisplayUpdateEvent) {
        if (!config.enabled.get()) return
        if (config.onlyHoldingAxe && InventoryUtils.getItemInHand()?.getItemCategoryOrNull() != ItemCategory.AXE) {
            display = null
            return
        }
        val newName = event.newName ?: return
        currentTreeProgressPattern.matchStyledMatcher(newName) {
            val component = if (config.compact) formatCompact() else newName
            displayEntityId = event.entity.id
            display = Renderable.text(component)
        }
    }

    private fun ComponentMatcher.formatCompact(): Component {
        val treeType = componentOrThrow("treeType")
        val percent = groupOrThrow("percent")
        val percentStyle = percent.sampleStyleAtStart()

        return componentBuilder {
            append(treeType)
            append(" ")
            append(percent.intoComponent())
            append("%") {
                style = percentStyle
            }
        }
    }
}
