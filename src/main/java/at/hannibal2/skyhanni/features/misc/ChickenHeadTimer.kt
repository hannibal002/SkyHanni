package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.time.Duration.Companion.seconds

class ChickenHeadTimer {
    private var tick = 0
    private var hasChickenHead = false
    private var lastTime = TimeUtils.now()
    private val config get() = SkyHanniMod.feature.misc

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return
        if (tick++ % 5 != 0) return

        val itemStack = InventoryUtils.getArmor()[3]
        val name = itemStack?.name ?: ""
        hasChickenHead = name.contains("Chicken Head")
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        lastTime = TimeUtils.now()
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (!hasChickenHead) return
        if (event.message == "§aYou laid an egg!") {
            lastTime = TimeUtils.now()
            if (config.chickenHeadTimerHideChat) {
                event.blockedReason = "chicken_head_timer"
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!hasChickenHead) return

        val duration = TimeUtils.now().minus(lastTime)
        val remainingDuration = 20.seconds - duration

        val displayText = if (remainingDuration.isNegative()) {
            "Chicken Head Timer: §aNow"
        } else {
            val formatDuration = TimeUtils.formatDuration(remainingDuration)
            "Chicken Head Timer: §b$formatDuration"
        }

        config.chickenHeadTimerPosition.renderString(displayText, posLabel = "Chicken Head Timer")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.chickenHeadTimerDisplay
}