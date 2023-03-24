package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.client.Minecraft
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class ChickenHeadTimer {

    private var tick = 0
    private var hasChickenHead = false
    private var lastTime = 0L

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return
        if (tick++ % 5 != 0) return

        val itemStack = Minecraft.getMinecraft().thePlayer.inventory.armorInventory[3]
        val name = itemStack?.name ?: ""
        hasChickenHead = name.contains("Chicken Head")
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        lastTime = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (!hasChickenHead) return
        if (event.message == "§aYou laid an egg!") {
            lastTime = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!hasChickenHead) return

        val sinceLastTime = System.currentTimeMillis() - lastTime
        val cooldown = 20_000
        val remainingTime = cooldown - sinceLastTime

        val displayText = if (remainingTime < 0) {
            "Chicken Head Timer: §aNow"
        } else {
            val formatDuration = TimeUtils.formatDuration(remainingTime)
            "Chicken Head Timer: §b$formatDuration"
        }

        SkyHanniMod.feature.misc.chickenHeadTimerPosition.renderString(displayText, posLabel = "Chicken Head Timer")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.misc.chickenHeadTimerDisplay
}