package at.hannibal2.skyhanni.features.inventory.loadout

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.render.gui.GuiMouseInputEvent
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object CustomLoadoutKeybinds {

    private data class LoadoutBinding(val slotIndex: Int, val key: Int)

    private val config get() = LoadoutApi.config.keybinds
    private val keybinds get() = config.slotKeybinds.asList()
    private val contestKeybinds get() = config.contestSlotKeybinds.asList()
    private var lastClick = SimpleTimeMark.farPast()

    @HandleEvent
    fun onGuiKeyPress(event: GuiKeyPressEvent) {
        if (handlePress()) event.cancel()
    }

    @HandleEvent
    fun onGuiMouseInput(event: GuiMouseInputEvent) {
        if (handlePress()) event.cancel()
    }

    private fun handlePress(): Boolean {
        if (!isEnabled()) return false
        val slots = LoadoutApi.slots.filter { it.isInCurrentPage() }
        if (config.cycleKey.isKeyHeld()) {
            return cycleLoadout(slots)
        }

        val bindings = activeKeybinds()

        for ((index, key) in bindings) {
            if (!key.isKeyHeld()) continue
            if (lastClick.passedSince() < 200.milliseconds) break
            val slot = slots.getOrNull(index) ?: continue

            val clicked = LoadoutApi.clickSlot(slot)
            if (clicked) {
                lastClick = SimpleTimeMark.now()
                return true
            }
        }

        return false
    }

    private fun cycleLoadout(slots: List<LoadoutSlot>): Boolean {
        if (lastClick.passedSince() < 200.milliseconds) return false
        val contestOrder = config.contestCycleOrder
        val configuredOrder = if (FarmingContestApi.isContestActive && contestOrder.isNotEmpty()) {
            contestOrder
        } else {
            config.cycleOrder
        }
        val orderedSlots = configuredOrder.mapNotNull { slots.getOrNull(it.ordinal) }
        val currentIndex = orderedSlots.indexOfFirst { it.id == LoadoutApi.currentSlot }
        val cycle = if (currentIndex == -1) {
            orderedSlots
        } else {
            orderedSlots.drop(currentIndex + 1) + orderedSlots.take(currentIndex + 1)
        }

        for (slot in cycle) {
            if (!LoadoutApi.clickSlot(slot)) continue
            lastClick = SimpleTimeMark.now()
            return true
        }
        return false
    }

    fun allowMouseClick() = isEnabled() && (
        config.cycleKey < 0 && config.cycleKey.isKeyHeld() ||
            activeKeybinds().any { it.key < 0 && it.key.isKeyHeld() }
        )

    fun allowKeyboardClick() = isEnabled() && (
        config.cycleKey > 0 && config.cycleKey.isKeyHeld() ||
            activeKeybinds().any { it.key > 0 && it.key.isKeyHeld() }
        )

    private fun activeKeybinds(): List<LoadoutBinding> = buildList {
        if (FarmingContestApi.isContestActive) {
            contestKeybinds.forEachIndexed { index, key -> add(LoadoutBinding(index, key)) }
        }
        keybinds.forEachIndexed { index, key -> add(LoadoutBinding(index, key)) }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && LoadoutApi.inLoadouts() && config.slotKeybindsToggle
}
