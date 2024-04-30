package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.mining.CompactUpdateEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matches
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object BlocksSinceMineshaftDisplay {
    private val config get() = SkyHanniMod.feature.mining.blocksSinceMineshaft

    private var hardstoneBlocks = 0L
    private var hardstoneBlocksNoMole = 0L

    @SubscribeEvent
    fun onCompactUpdate(event: CompactUpdateEvent) {
        if (!isEnabled()) return
        ChatUtils.debug("Mined ${event.amount} ${event.block.blockName}")
        if (event.block.oreType == OreType.HARD_STONE) {
            hardstoneBlocks += event.amount
            hardstoneBlocksNoMole++
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        Position(10, 10).renderStrings(
            listOf(
                "Hardstone mined: $hardstoneBlocks",
                "Without Mole: $hardstoneBlocksNoMole"
            ), posLabel = "shaft test"
        )
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (MiningNotifications.mineshaftSpawn.matches(event.message)) {
            ChatUtils.chat("Hardstone mined: $hardstoneBlocks")
            ChatUtils.chat("Without Mole: $hardstoneBlocksNoMole")
            resetShaftBlocks()
        }
    }

    fun resetShaftBlocks() {
        hardstoneBlocks = 0
        hardstoneBlocksNoMole = 0
    }

    @SubscribeEvent

    fun isEnabled() = MiningAPI.inGlacialTunnels() && config
}
