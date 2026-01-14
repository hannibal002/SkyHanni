package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.pests.MantidDisplayConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.tracker.ArmorDropTracker
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeModifier
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import java.util.LinkedList
import java.util.Queue
import kotlin.math.min
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MantidKillDisplay {

    private const val MAX_BONUS = 20
    private val EXPIRE_TIME = 10.minutes
    private val config get() = PestApi.config.mantidDisplay
    // mantid reforge does not work like refrigerate; each pest kill time is individually stored
    private val pestExpireQueue: Queue<SimpleTimeMark> = LinkedList()
    private val isWearingMantid by RecalculatingValue(1.seconds) {
        GardenApi.inGarden() && checkMantid()
    }
    private var nextExpire: SimpleTimeMark = SimpleTimeMark.farPast()
    private var nextExpireGroup: Int = 0
    private var displayCache = emptyList<Renderable>()

    @HandleEvent
    fun onPestKill(event: PestKillEvent) {
        if (!checkMantid()) return
        pestExpireQueue.add(SimpleTimeMark.now() + EXPIRE_TIME)
        removeExtraEntries()
    }

    // mantid bonus resets on world change
    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        resetKills()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSecondPassed(event: SecondPassedEvent) {
        checkForExpired()
        updateDisplay()
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnIsland = IslandType.GARDEN)
    fun onRenderOverlay() {
        if (!shouldShow()) return
        config.pos.renderRenderables(displayCache, posLabel = "Pest Spawn Timer")
    }

    private fun removeExtraEntries() {
        while (pestExpireQueue.size > MAX_BONUS) {
            pestExpireQueue.poll()
        }
        updateNextExpireGroup()
    }

    // mantid bonus is a profile value that is incremented whenever you get a kill while wearing any armor piece that has the reforge
    private fun checkMantid(): Boolean {
        InventoryUtils.getArmor().forEach { armor ->
            if (armor?.getReforgeModifier() == "mantid") return true
        }
        return false
    }

    private fun checkForExpired() {
        while (pestExpireQueue.peek()?.isInPast() == true) {
            pestExpireQueue.poll()
        }
        updateNextExpireGroup()
    }

    private fun updateNextExpireGroup() {
        var count = 0
        nextExpire = SimpleTimeMark.farPast()
        pestExpireQueue.forEach {
            if (nextExpire.isFarPast()) nextExpire = it
            if (it - nextExpire < config.groupSimilarExpire.seconds) {
                count++
            } else {
                return@forEach
            }
        }
        nextExpireGroup = count
    }

    private fun updateDisplay() {
        displayCache = drawDisplay()
    }

    private fun drawDisplay(): List<Renderable> = buildList {
        val bonus = min(pestExpireQueue.size, MAX_BONUS)
        val bonusColor = if (bonus < MAX_BONUS) "§c" else "§a"
        add(Renderable.text("§2Mantid Bonus: $bonusColor$bonus§7/§a$MAX_BONUS"))

        if (bonus > 0) {
            val pestString = if (nextExpireGroup == 1) "1 Pest Expires" else "$nextExpireGroup Pests Expire"
            add(Renderable.text("§e$pestString: §b${nextExpire.timeUntil().format()}"))
        }
    }

    private fun resetKills() {
        pestExpireQueue.clear()
        updateNextExpireGroup()
        updateDisplay()
    }

    private fun isEnabled() = config.enabled && GardenApi.inGarden()
    private fun shouldShow() = isEnabled() && checkShowConditions()
    @Suppress("ReturnCount")
    private fun checkShowConditions(): Boolean {
        for (condition in config.whenToShow) {
            when (condition) {
                MantidDisplayConfig.WhenShowDisplay.ALWAYS -> return true
                MantidDisplayConfig.WhenShowDisplay.ARMOR -> if (ArmorDropTracker.hasArmor) return true
                MantidDisplayConfig.WhenShowDisplay.MANTID -> if (isWearingMantid) return true
                MantidDisplayConfig.WhenShowDisplay.TOOL -> if (GardenApi.hasFarmingToolInHand()) return true
                MantidDisplayConfig.WhenShowDisplay.VACUUM -> if (PestApi.hasVacuumInHand()) return true
            }
        }
        return false
    }
}
