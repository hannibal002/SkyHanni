package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityRemovedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ComponentMatcher
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.matchStyledMatcher
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object TreeProgressDisplay {

    private val config get() = SkyHanniMod.feature.foraging.trees.progress

    private var treeProgressDisplay: TreeProgressDisplay? = null
    private data class TreeProgressDisplay(
        val id: Int,
        val renderable: Renderable,
        val distanceToPlayer: Double,
    )

    /**
     * REGEX-TEST: FIG TREE 88%
     * REGEX-TEST: MANGROVE TREE 5%
     */
    private val currentTreeProgressPattern by RepoPattern.pattern(
        "foraging.tree.progress.colorless",
        "(?<treeType>\\w+) TREE (?<percent>\\d+)%",
    )

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    private fun onGuiRenderOverlay() {
        if (!isEnabled()) return
        treeProgressDisplay?.let {
            config.position.renderRenderable(it.renderable, posLabel = "Tree Progress")
        }
    }

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    private fun onIslandJoin() {
        treeProgressDisplay = null
    }

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    private fun onEntityRemoved(event: EntityRemovedEvent<ArmorStand>) {
        if (!isEnabled()) return
        if (event.entity.id == treeProgressDisplay?.id) {
            treeProgressDisplay = null
        }
    }

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    private fun onEntityNameUpdate(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        if (!isEnabled()) return
        if (!MinecraftCompat.localPlayerExists) return

        if (config.onlyHoldingAxe && InventoryUtils.getItemInHand()?.getItemCategoryOrNull() != ItemCategory.AXE) {
            treeProgressDisplay = null
            return
        }

        val newName = event.newName ?: return
        val displayText = currentTreeProgressPattern.matchStyledMatcher(newName) {
            if (config.compact) formatCompact() else newName
        } ?: return

        val entityDistance = event.entity.distanceToPlayer()

        val current = treeProgressDisplay
        if (current != null && current.id != event.entity.id) {
            // Prefer the closest tree if there are multiple trees with progress displayed
            val currentDistance = current.distanceToPlayer
            val newDistance = event.entity.distanceToPlayer()
            if (newDistance >= currentDistance) {
                return
            }
        }

        treeProgressDisplay = TreeProgressDisplay(
            id = event.entity.id,
            renderable = Renderable.text(displayText),
            distanceToPlayer = entityDistance,
        )
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

    private fun isEnabled() = config.enabled
}
