package at.hannibal2.hanni.features.itemabilities

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.TimeUtils.format
import kotlin.time.Duration.Companion.seconds

@HanniModule
object ChickenHeadTimer {
    private val config get() = HanniMod.feature.inventory.itemAbilities.chickenHead

    private var hasChickenHead = false
    private var lastTime = SimpleTimeMark.farPast()
    private val cooldown = 5.seconds

    private val chickenHead = "CHICKEN_HEAD".toInternalName()

    @HandleEvent
    fun onTick(event: HanniTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return

        hasChickenHead = InventoryUtils.getHelmet()?.getInternalName() == chickenHead
    }

    @HandleEvent
    fun onWorldChange() {
        lastTime = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return
        if (!hasChickenHead) return
        if (event.message == "§aYou laid an egg!") {
            lastTime = SimpleTimeMark.now()
            if (config.hideChat) {
                event.blockedReason = "chicken_head_timer"
            }
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!hasChickenHead) return

        val remainingTime = cooldown - lastTime.passedSince()
        val displayText = if (remainingTime.isNegative()) {
            "Chicken Head Timer: §aNow"
        } else {
            val formatDuration = remainingTime.format()
            "Chicken Head Timer: §b$formatDuration"
        }

        config.position.renderString(displayText, posLabel = "Chicken Head Timer")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.chickenHeadTimerHideChat", "itemAbilities.chickenHead.hideChat")
        event.move(2, "misc.chickenHeadTimerPosition", "itemAbilities.chickenHead.position")
        event.move(2, "misc.chickenHeadTimerDisplay", "itemAbilities.chickenHead.displayTimer")
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.displayTimer
}
