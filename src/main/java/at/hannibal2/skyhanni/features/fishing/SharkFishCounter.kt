package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.SeaCreatureFishEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SharkFishCounter {
    private var counter = 0
    private var display = ""
    private var hasWaterRodInHand = false

    @SubscribeEvent
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        if (!SkyHanniMod.feature.fishing.sharkFishCounter) return

        if (event.seaCreature.name.contains("Shark")) {
            counter += if (event.doubleHook) 2 else 1
            display = "§7Sharks caught: §e${counter.addSeparators()}"
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.fishing.sharkFishCounter) return

        if (event.isMod(10)) {
            hasWaterRodInHand = isWaterFishingRod()
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (event.message == "§b§lFISHING FESTIVAL §r§eThe festival has concluded! Time to dry off and repair your rods!") {
            val funnyComment = when {
                counter == 0 -> return
                counter < 50 -> "Well done!"
                counter < 100 -> "Nice!"
                counter < 150 -> "Really nice!"
                counter < 200 -> "Super cool!"
                counter < 250 -> "Mega cool!"
                counter < 350 -> "Like a pro!"
                else -> "How???"
            }
            LorenzUtils.chat("§e[SkyHanni] You caught ${counter.addSeparators()} sharks during this fishing contest. $funnyComment")
            counter = 0
        }
    }

    private fun isWaterFishingRod(): Boolean {
        val heldItem = InventoryUtils.getItemInHand() ?: return false
        val isRod = heldItem.name?.contains("Rod") ?: return false
        if (!isRod) return false

        val isLavaRod = heldItem.getLore().any { it.contains("Lava Rod") }
        return !isLavaRod
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.fishing.sharkFishCounter) return
        if (!hasWaterRodInHand) return

        SkyHanniMod.feature.fishing.sharkFishCounterPos.renderString(display, posLabel = "Shark Fish Counter")
    }
}
