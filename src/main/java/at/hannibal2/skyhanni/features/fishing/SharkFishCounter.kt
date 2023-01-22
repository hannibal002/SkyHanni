package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.SeaCreatureFishEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class SharkFishCounter {
    private var counter = 0
    private var display = "$counter sharks caught"
    private var tick = 0
    private var hasWaterRodInHand = false

    @SubscribeEvent
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        if (!SkyHanniMod.feature.fishing.sharkFishCounter) return

        val displayName = event.seaCreature.displayName
        if (displayName.contains("Shark")) {
            counter++
            display = "$counter sharks caught"
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.fishing.sharkFishCounter) return

        tick++

        if (tick % 10 == 0) {
            hasWaterRodInHand = isWaterFishingRod()
        }
    }

    private fun isWaterFishingRod(): Boolean {
        val heldItem = Minecraft.getMinecraft().thePlayer.heldItem ?: return false
        val isRod = heldItem.name?.contains("Rod") ?: return false
        if (!isRod) return false

        val isLavaRod = heldItem.getLore().any { it.contains("Lava Rod") }
        if (isLavaRod) return false

        return true
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.fishing.sharkFishCounter) return
        if (!hasWaterRodInHand) return

        SkyHanniMod.feature.fishing.sharkFishCounterPos.renderString(display)
    }
}