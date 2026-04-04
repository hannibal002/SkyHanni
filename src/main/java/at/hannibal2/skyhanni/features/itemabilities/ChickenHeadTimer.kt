package at.hannibal2.skyhanni.features.itemabilities

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ChickenHeadTimer {

    private val config get() = SkyHanniMod.feature.inventory.itemAbilities.chickenHead
    private val chickenHead = "CHICKEN_HEAD".toInternalName()

    private var hasChickenHead = false
    private var lastTime = SimpleTimeMark.farPast()
    private val cooldown = 5.seconds

    // Todo (I'm pretty sure?) we have an event that triggers when inv slots change
    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        if (!config.displayTimer || !event.isMod(5)) return
        hasChickenHead = InventoryUtils.getHelmet()?.getInternalName() == chickenHead
    }

    @HandleEvent
    fun onWorldChange() {
        lastTime = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.displayTimer || !hasChickenHead) return
        if (event.message == "§aYou laid an egg!") {
            lastTime = SimpleTimeMark.now()
            if (config.hideChat) {
                event.blockedReason = "chicken_head_timer"
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.displayTimer || !hasChickenHead) return

        val remainingTime = cooldown - lastTime.passedSince()
        val display = Renderable.text {
            if (remainingTime.isNegative()) append("Chicken Head Timer: §aNow")
            else {
                val formatDuration = remainingTime.format()
                append("Chicken Head Timer: §b$formatDuration")
            }
        }

        config.position.renderRenderable(display, posLabel = "Chicken Head Timer")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.chickenHeadTimerHideChat", "itemAbilities.chickenHead.hideChat")
        event.move(2, "misc.chickenHeadTimerPosition", "itemAbilities.chickenHead.position")
        event.move(2, "misc.chickenHeadTimerDisplay", "itemAbilities.chickenHead.displayTimer")
    }
}
