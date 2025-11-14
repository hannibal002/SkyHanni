package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.item.ShardEvent
import at.hannibal2.skyhanni.events.item.ShardSource
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeShardsData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemNameCompact
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.find
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object FusionDisplay {

    private val config get() = SkyHanniMod.feature.hunting

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.fusionDisplay

    private var renderable: List<Renderable>? = null

    private var lastInternalName: NeuInternalName? = null
    private var pureReptiles = 0

    /**
     * REGEX-TEST: §b§lPURE REPTILE
     */
    private val pureReptilePattern by RepoPattern.group("attributeshards").pattern(
        "pure-reptile-chat",
        "^§b§lPURE REPTILE",
    )

    @HandleEvent
    fun onShardGain(event: ShardEvent) {
        if (event.source != ShardSource.FUSE) return
        if (event.amount < 0) return
        if (lastInternalName != event.shardInternalName) {
            lastInternalName = event.shardInternalName
            pureReptiles = 0
        }
        makeRenderable()
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (event.inventoryName == "Fusion Box") makeRenderable()
    }

    private fun makeRenderable() {
        val internalName = lastInternalName ?: return
        val shardName = AttributeShardsData.shardInternalNameToShardName(internalName)
        val currentShardAmount = AttributeShardsData.getAmountInHuntingBox(shardName)
        val currentShardName = internalName.repoItemNameCompact
        val list = mutableListOf(Renderable.text("$currentShardName§7: §7$currentShardAmount"))
        if (pureReptiles > 0) {
            list += Renderable.text("§b§lPURE REPTILE§7: §e$pureReptiles")
        }
        renderable = list
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onChat(event: SkyHanniChatEvent) {
        if (pureReptilePattern.find(event.message)) pureReptiles++
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!AttributeShardsData.isInFusionMachine()) return
        renderable?.let {
            config.fusionDisplayPosition.renderRenderables(it, posLabel = "Fusion Display")
        }
    }
}
