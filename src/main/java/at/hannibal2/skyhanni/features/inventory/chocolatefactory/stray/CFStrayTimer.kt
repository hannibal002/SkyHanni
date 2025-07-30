package at.hannibal2.skyhanni.features.inventory.chocolatefactory.stray

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.HoppityEggLocationsJson
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.hoppity.EggFoundEvent
import at.hannibal2.skyhanni.events.inventory.AttemptedInventoryCloseEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import java.util.Locale
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
        if (timer == Duration.ZERO) return
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

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() || !config.blockClosing) return
        if (KeyboardManager.isShiftKeyDown()) return
        if (event.slotId !in destructiveSlots) return
        event.sendPreventCloseTitle()
    }

    @HandleEvent
    fun onAttemptedInventoryClose(event: AttemptedInventoryCloseEvent) {
        if (!config.blockClosing || !isEnabled()) return
        event.sendPreventCloseTitle()
    }

    private fun getTimerRenderable() = Renderable.vertical(
        Renderable.text("§eStray Timer"),
        Renderable.text("§b${String.format(Locale.US, "%.2f", timer.inPartialSeconds)}s"),
    )

    private fun SkyHanniEvent.Cancellable.sendPreventCloseTitle() {
        TitleManager.sendTitle(
            "§cStray Timer Prevented Close",
            subtitleText = "§7Hold §eShift §7to bypass",
            duration = 5.seconds,
            location = TitleManager.TitleLocation.INVENTORY,
        )
        SoundUtils.playErrorSound()
        cancel()
    }

    private fun isEnabled() = config.enabled && InventoryUtils.openInventoryName() == "Chocolate Factory" && timer > Duration.ZERO
}
