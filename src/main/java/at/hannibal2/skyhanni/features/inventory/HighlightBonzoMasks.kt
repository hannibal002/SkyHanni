package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.interpolate
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * @author Linnea Gräf
 */
@OptIn(ExperimentalTime::class)
class HighlightBonzoMasks {

    val bonzoMaskTimers = mutableMapOf<String, TimeMark>()

    // Technically this timer is overestimating since the cooldown is affected by mage level, however I do not care.
    val bonzoMaskCooldown = 360.seconds
    val bonzoMaskMessage = "Your (.*Bonzo's Mask) saved your life!".toRegex()

    val greenHue = Color.RGBtoHSB(0, 255, 0, null)[0].toDouble()
    val redHue = Color.RGBtoHSB(255, 0, 0, null)[0].toDouble()

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!SkyHanniMod.feature.inventory.highlightDepletedBonzosMasks) return
        for (slot in event.gui.inventorySlots.inventorySlots) {
            val item = slot.stack ?: continue
            val internalName = item.getInternalName()
            if (!internalName.endsWith("BONZO_MASK")) continue
            val timer = bonzoMaskTimers[internalName] ?: continue
            if (timer.elapsedNow() < bonzoMaskCooldown) {
                val progress =
                    timer.elapsedNow().toDouble(DurationUnit.SECONDS) / bonzoMaskCooldown.toDouble(DurationUnit.SECONDS)
                val hue = interpolate(greenHue, redHue, progress.toFloat())
                slot.highlight(Color(Color.HSBtoRGB(hue.toFloat(), 1F, 1F)))
            }
        }
    }

    @SubscribeEvent
    fun onChatReceived(event: ClientChatReceivedEvent) {
        val match = bonzoMaskMessage.matchEntire(event.message.unformattedText) ?: return
        val bonzoId = if ("⚚" in match.groupValues[1]) "STARRED_BONZO_MASK" else "BONZO_MASK"
        bonzoMaskTimers[bonzoId] = TimeSource.Monotonic.markNow()
    }

    @SubscribeEvent
    fun onJoinWorld(ignored: WorldEvent.Load) {
        bonzoMaskTimers.clear()
    }
}