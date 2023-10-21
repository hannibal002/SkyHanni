package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.interpolate
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.time.Duration
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
    private val config get() = SkyHanniMod.feature.itemAbilities

    private val maskTimers = mutableMapOf<String, CooldownTimer>()

    // Technically this timer is overestimating since mage level affects the cooldown, however I don't care.
    private val bonzoMaskCooldown = 360.seconds
    private val bonzoMaskMessage = "^Your (.*Bonzo's Mask) saved your life!$".toRegex()

    private val spiritMaskCooldown = 30.seconds
    private val spiritMaskMessage = "^Second Wind Activated! Your Spirit Mask saved your life!$".toRegex()

    private val greenHue = Color.RGBtoHSB(0, 255, 0, null)[0].toDouble()
    private val redHue = Color.RGBtoHSB(255, 0, 0, null)[0].toDouble()

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.depletedBonzosMasks) return
        for (slot in event.gui.inventorySlots.inventorySlots) {
            val item = slot.stack ?: continue
            val maskType = maskType(item) ?: continue
            val timer = maskTimers[maskType] ?: continue
            if (timer.isActive) {
                val hue = interpolate(greenHue, redHue, timer.percentComplete.toFloat())
                slot.highlight(Color(Color.HSBtoRGB(hue.toFloat(), 1F, 1F)))
            }
        }
    }

    private fun maskType(item: ItemStack): String? {
        return when (item.getInternalName().asString()) {
            "STARRED_BONZO_MASK" -> "BONZO_MASK"
            "BONZO_MASK" -> "BONZO_MASK"
            "SPIRIT_MASK" -> "SPIRIT_MASK"
            "STARRED_SPIRIT_MASK" -> "SPIRIT_MASK"
            else -> null
        }
    }

    @SubscribeEvent
    fun onChatReceived(event: LorenzChatEvent) {
        val message = event.message.removeColor()
        if (bonzoMaskMessage.matches(message)) {
            maskTimers["BONZO_MASK"] = CooldownTimer(TimeSource.Monotonic.markNow(), bonzoMaskCooldown)
        } else if (spiritMaskMessage.matches(message)) {
            maskTimers["SPIRIT_MASK"] = CooldownTimer(TimeSource.Monotonic.markNow(), spiritMaskCooldown)
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        maskTimers.clear()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "inventory.highlightDepletedBonzosMasks", "itemAbilities.depletedBonzosMasks")
    }

    companion object {
        data class CooldownTimer(val timeMark: TimeMark, val duration: Duration) {
            val percentComplete: Double
                get() =
                    timeMark.elapsedNow().toDouble(DurationUnit.SECONDS) / duration.toDouble(DurationUnit.SECONDS)

            val isActive: Boolean get() = timeMark.elapsedNow() < duration

        }
    }
}

