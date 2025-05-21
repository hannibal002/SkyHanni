package at.hannibal2.skyhanni.features.inventory.chocolatefactory.stray

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.data.jsonobjects.repo.HoppityEggLocationsJson
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.hoppity.EggFoundEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isInventoryClosure
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CFStrayTimer {

    private val config get() = SkyHanniMod.feature.event.hoppityEggs.strayTimer
    private var timer: Duration = Duration.ZERO
    private var lastTimerSubtraction: SimpleTimeMark? = SimpleTimeMark.farPast()
    private var lastPingTime = SimpleTimeMark.farPast()
    private var destructiveSlots: Set<Int> = setOf()

    @HandleEvent
    fun onEggFound(event: EggFoundEvent) {
        timer = when (event.type) {
            // If a stray is found, the timer is no longer relevant
            HoppityEggType.STRAY -> Duration.ZERO
            // Only reset the timer for meal entries and hitman eggs
            in HoppityEggType.resettingEntries, HoppityEggType.HITMAN -> 30.seconds
            else -> return
        }
        lastTimerSubtraction = null
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        timer = Duration.ZERO
        lastTimerSubtraction = null
    }

    @HandleEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (timer == Duration.ZERO) return
        timer = when (event.inventoryName) {
            "Chocolate Factory" -> timer
            else -> 30.seconds
        }
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        // Reset the timer when the inventory is closed prematurely
        timer = 30.seconds
        lastTimerSubtraction = null
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        lastTimerSubtraction = lastTimerSubtraction?.takeIfInitialized()?.let {
            timer -= it.passedSince()
            if (timer < Duration.ZERO) timer = Duration.ZERO
            else if (timer < config.dingForTimer.seconds && lastPingTime.passedSince() > 1.seconds) {
                SoundUtils.playPlingSound()
                lastPingTime = SimpleTimeMark.now()
            }
            SimpleTimeMark.now()
        } ?: SimpleTimeMark.now()
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.strayTimerPosition.renderRenderable(getTimerRenderable(), posLabel = "Stray Timer")
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        destructiveSlots = event.getConstant<HoppityEggLocationsJson>("HoppityEggLocations").destructiveSlots
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() || !config.blockClosing) return
        if (event.slotId in destructiveSlots) {
            event.cancel()
            preventCloseTitle()
        }
    }

    private fun getTimerRenderable(): Renderable = VerticalContainerRenderable(
        listOf(
            "§eStray Timer",
            "§b${String.format(Locale.US, "%.2f", timer.inPartialSeconds)}s"
        ).map { RenderableString(it) }
    )

    private fun preventCloseTitle() {
        TitleManager.sendTitle(
            "§cStray Timer Prevented Close",
            subtitleText = "§7Hold §eShift §7to bypass",
            duration = 5.seconds,
            location = TitleManager.TitleLocation.INVENTORY
        )
        SoundUtils.playErrorSound()
    }

    @JvmStatic
    fun shouldContinueWithKeypress(keycode: Int): Boolean {
        if (!isInventoryClosure(keycode)) return true
        if (!config.blockClosing || !isEnabled()) return true
        preventCloseTitle()
        return false
    }

    private fun isEnabled() = config.enabled && InventoryUtils.openInventoryName() == "Chocolate Factory" && timer > Duration.ZERO
}
