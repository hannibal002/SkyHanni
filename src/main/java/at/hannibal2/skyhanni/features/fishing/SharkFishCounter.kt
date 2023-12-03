package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.SeaCreatureFishEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SharkFishCounter {
    private var counter = mutableListOf(0, 0, 0, 0)
    private var display = ""
    private var hasWaterRodInHand = false

    @SubscribeEvent
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        if (!SkyHanniMod.feature.fishing.sharkFishCounter) return

        if (event.seaCreature.name.contains("Shark")) {
            val index =
                if (event.seaCreature.name.contains("Blue")) {
                    1
                } else if (event.seaCreature.name.contains("Tiger")) {
                    2
                } else if (event.seaCreature.name.contains("Great")) {
                    3
                } else {
                    0
                }
            counter[index] += if (event.doubleHook) 2 else 1
            display = "§7Sharks caught: §e${counter.sum().addSeparators()} §7(§a${counter[0]} §9${counter[1]} §5${counter[2]} §6${counter[3]}§7)"
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
            val count = counter.sum()
            val funnyComment = when {
                count == 0 -> return
                count < 50 -> "Well done!"
                count < 100 -> "Nice!"
                count < 150 -> "Really nice!"
                count < 200 -> "Super cool!"
                count < 250 -> "Mega cool!"
                count < 350 -> "Like a pro!"
                else -> "How???"
            }
            LorenzUtils.chat("You caught ${count.addSeparators()} §f(§a${counter[0]} §9${counter[1]} §5${counter[2]} §6${counter[3]}§f) sharks during this fishing contest. $funnyComment")
            counter = mutableListOf(0, 0, 0, 0)
            display = ""
        }
    }

    private fun isWaterFishingRod() = FishingAPI.hasFishingRodInHand() && !FishingAPI.isLavaRod()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.fishing.sharkFishCounter) return
        if (!hasWaterRodInHand) return

        SkyHanniMod.feature.fishing.sharkFishCounterPos.renderString(display, posLabel = "Shark Fish Counter")
    }
}
