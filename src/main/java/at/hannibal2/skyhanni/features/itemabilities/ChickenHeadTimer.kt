package at.hannibal2.skyhanni.features.itemabilities

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ChickenHeadTimer {
    private val config get() = SkyHanniMod.feature.inventory.itemAbilities.chickenHead

    private var hasChickenHead = false
    private var lastTime = SimpleTimeMark.farPast()
    private val cooldown = 5.seconds

    private val chickenHead = "CHICKEN_HEAD".asInternalName()

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return

        hasChickenHead = InventoryUtils.getHelmet()?.getInternalName() == chickenHead
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        lastTime = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
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

    fun isEnabled() = LorenzUtils.inSkyBlock && config.displayTimer
}
