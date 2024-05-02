package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.mining.CustomBlockMineEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object BlocksSinceMineshaftDisplay {
    private val config get() = SkyHanniMod.feature.mining

    var minedBlocks = mutableMapOf<OreType, Int>()

    const val MAX_COUNTER = 2000

    @SubscribeEvent
    fun onCustomBlockMine(event: CustomBlockMineEvent) {
        if (!isEnabled()) return
        val oreType = event.originalBlock.oreType ?: return
        if (oreType == OreType.HARD_STONE) return
        minedBlocks.addOrPut(oreType, 1)
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (MiningNotifications.mineshaftSpawn.matches(event.message)) {
            val resultList = mutableListOf<String>()
            resultList.add("Mineshaft Pity Counter: ${calculateCounter()}/$MAX_COUNTER")
            resultList.add("Blocks mined:")
            minedBlocks.forEach {
                resultList.add("    ${it.key.oreName}: ${it.value}")
            }
            val string = resultList.joinToString("\n")
            OSUtils.copyToClipboard(string)
            ChatUtils.chat("Copied shaft spawn info to clipboard!")
            resetShaftBlocks()
        }
    }

    fun calculateCounter(): Int {
        if (minedBlocks.isEmpty()) return 0
        var counter = 0
        minedBlocks.forEach { counter += it.key.shaftMultiplier * it.value }
        return counter
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val renderable = Renderable.string(
            "Mineshaft Pity Counter: ${calculateCounter()}/$MAX_COUNTER"
        )
        config.mineshaftOddsDisplayPosition.renderRenderables(listOf(renderable), posLabel = "Mineshaft Pity Counter")

    }

    fun resetShaftBlocks() {
        minedBlocks = mutableMapOf()
    }

    fun isEnabled() = MiningAPI.inGlacialTunnels() && config.mineshaftOddsDisplay
}
