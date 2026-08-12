package at.hannibal2.skyhanni.features.inventory.loadout

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.inventory.customloadout.LoadoutKeybindConfig
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.render.gui.GuiMouseInputEvent
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.SkyBlockUtils

@SkyHanniModule
object CustomLoadoutKeybinds {

    private enum class LoadoutLayout(val displayName: String) {
        CONTEST("contest"),
        DEFAULT("normal"),
    }

    private data class LoadoutLayoutDefinition(
        val layout: LoadoutLayout,
        val isActive: () -> Boolean,
        val keybinds: () -> List<Int>,
        val cycleOrder: () -> List<LoadoutKeybindConfig.CycleLoadout>,
    )

    private data class LoadoutBinding(val slotIndex: Int, val key: Int, val layout: LoadoutLayout)

    private val config get() = LoadoutApi.config.keybinds
    private val keybinds get() = config.slotKeybinds.asList()
    private val contestKeybinds get() = config.contestSlotKeybinds.asList()
    private val layoutDefinitions
        get() = listOf(
            LoadoutLayoutDefinition(
                LoadoutLayout.CONTEST,
                { FarmingContestApi.isContestActive },
                { contestKeybinds },
                { config.contestCycleOrder },
            ),
            LoadoutLayoutDefinition(LoadoutLayout.DEFAULT, { true }, { keybinds }, { config.cycleOrder }),
        )
    private var debugEnabled = false

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        for (slot in 1..12) {
            event.move(
                143,
                "inventory.customLoadout.keybinds.slot$slot",
                "inventory.customLoadout.keybinds.slotKeybinds.slot$slot",
            )
            event.move(
                143,
                "inventory.customLoadout.keybinds.contestSlot$slot",
                "inventory.customLoadout.keybinds.contestSlotKeybinds.slot$slot",
            )
        }
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shloadoutdebug") {
            description = "Toggles contest loadout keybind decision logging."
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback {
                debugEnabled = !debugEnabled
                ChatUtils.chat("§7Contest loadout debug: ${if (debugEnabled) "§aenabled" else "§cdisabled"}")
                FarmingContestApi.getContestStatusDebug().forEach(ChatUtils::chat)
            }
        }
    }

    @HandleEvent
    private fun onGuiKeyPress(event: GuiKeyPressEvent) {
        if (handlePress()) event.cancel()
    }

    @HandleEvent
    private fun onGuiMouseInput(event: GuiMouseInputEvent) {
        if (handlePress()) event.cancel()
    }

    private fun handlePress(): Boolean {
        if (!isEnabled()) return false
        val slots = LoadoutApi.slots.filter { it.isInCurrentPage() }
        if (config.cycleKey.isKeyHeld()) {
            return cycleLoadout(slots)
        }

        val bindings = activeKeybinds()

        for ((index, key, layout) in bindings) {
            if (!key.isKeyHeld()) continue
            val slot = slots.getOrNull(index)
            if (slot == null) {
                debug("§cKey $key matched slot ${index + 1}, but that slot is not on the current page.")
                continue
            }

            val clicked = LoadoutApi.clickSlot(slot)
            debug(
                "§7Key §e$key §7-> contest active: §e${FarmingContestApi.isContestActive}§7, " +
                    "layout: §e${layout.displayName}§7, chosen slot: §e${index + 1}§7, " +
                    "click sent: §e$clicked",
            )
            if (clicked) {
                return true
            }
        }

        return false
    }

    private fun cycleLoadout(slots: List<LoadoutSlot>): Boolean {
        val activeLayouts = layoutDefinitions.filter { it.isActive() }
        val definition = activeLayouts.firstOrNull { it.cycleOrder().isNotEmpty() } ?: activeLayouts.last()
        val layout = definition.layout
        val configuredOrder = definition.cycleOrder()
        val orderedSlots = configuredOrder.mapNotNull { slots.getOrNull(it.ordinal) }
        val currentIndex = orderedSlots.indexOfFirst { it.id == LoadoutApi.currentSlot }
        val cycle = if (currentIndex == -1) {
            orderedSlots
        } else {
            orderedSlots.drop(currentIndex + 1) + orderedSlots.take(currentIndex + 1)
        }

        for (slot in cycle) {
            if (!LoadoutApi.clickSlot(slot)) continue
            debug(
                "§7Cycle key -> contest active: §e${FarmingContestApi.isContestActive}§7, " +
                    "layout: §e${layout.displayName}§7, chosen slot: §e${slot.id + 1}",
            )
            return true
        }
        debug("§cCycle key pressed, but its active order has no available loadouts on this page.")
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

    private fun activeKeybinds(): List<LoadoutBinding> = layoutDefinitions
        .filter { it.isActive() }
        .flatMap { definition ->
            definition.keybinds().mapIndexed { index, key -> LoadoutBinding(index, key, definition.layout) }
        }

    private fun debug(message: String) {
        if (debugEnabled) ChatUtils.debug("Loadout keybinds: $message")
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && LoadoutApi.inLoadouts() && config.slotKeybindsToggle
}
