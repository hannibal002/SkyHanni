package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.item.ShardEvent
import at.hannibal2.skyhanni.events.item.ShardSource
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeShardsData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemNameCompact
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.find
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object FusionDisplay {

    private val config get() = SkyHanniMod.feature.hunting

    private var renderable: List<Renderable>? = null

    private var lastInternalName: NeuInternalName? = null
    private var pureReptiles = 0

    private val patternGroup = RepoPattern.group("attributeshards")

    /**
     * REGEX-TEST: PURE REPTILE
     */
    private val pureReptilePattern by patternGroup.pattern(
        "pure-reptile-chat.colorless",
        "^PURE REPTILE",
    )

    @HandleEvent
    private fun onShardGain(event: ShardEvent) {
        if (event.source != ShardSource.FUSE) return
        if (event.amount < 0) return
        if (lastInternalName != event.shardInternalName) {
            lastInternalName = event.shardInternalName
            pureReptiles = 0
        }
        makeRenderable()
    }

    @HandleEvent
    private fun onInventoryFullyOpened() {
        if (AttributeShardsData.fusionBoxInventory.isInside()) makeRenderable()
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

    @HandleEvent(onlyOnSkyblock = true)
    private fun onSystemMessage(event: SystemMessageEvent.Allow) {
        if (pureReptilePattern.find(event.cleanMessage)) pureReptiles++
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChestGuiRender() {
        if (!config.fusionDisplay) return
        if (!AttributeShardsData.isInFusionMachine()) return
        renderable?.let {
            config.fusionDisplayPosition.renderRenderables(it, posLabel = "Fusion Display")
        }
    }
}
