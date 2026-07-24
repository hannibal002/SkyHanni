package at.hannibal2.skyhanni.features.inventory.loadout

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.render.gui.GuiMouseInputEvent
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object CustomLoadoutKeybinds {

    private data class LoadoutBinding(val slotIndex: Int, val key: Int, val contest: Boolean)

    private val config get() = LoadoutApi.config.keybinds
    private val keybinds
        get() = listOf(
            config.slot1,
            config.slot2,
            config.slot3,
            config.slot4,
            config.slot5,
            config.slot6,
            config.slot7,
            config.slot8,
            config.slot9,
            config.slot10,
            config.slot11,
            config.slot12,
        )
    private val contestKeybinds
        get() = listOf(
            config.contestSlot1,
            config.contestSlot2,
            config.contestSlot3,
            config.contestSlot4,
            config.contestSlot5,
            config.contestSlot6,
            config.contestSlot7,
            config.contestSlot8,
            config.contestSlot9,
            config.contestSlot10,
            config.contestSlot11,
            config.contestSlot12,
        )
    private var lastClick = SimpleTimeMark.farPast()
    private var debugEnabled = false

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
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

        for ((index, key, contest) in activeKeybinds()) {
            if (!key.isKeyHeld()) continue
            if (lastClick.passedSince() < 200.milliseconds) break
            val slot = slots.getOrNull(index)
            if (slot == null) {
                debug("§cKey $key matched slot ${index + 1}, but that slot is not on the current page.")
                continue
            }

            val clicked = LoadoutApi.clickSlot(slot)
            lastClick = SimpleTimeMark.now()
            debug(
                "§7Key §e$key §7-> contest active: §e${FarmingContestApi.isContestActive}§7, " +
                    "binding: §e${if (contest) "contest" else "normal"}§7, chosen slot: §e${index + 1}§7, " +
                    "click sent: §e$clicked",
            )
            return true
        }

        return false
    }

    fun allowMouseClick() = isEnabled() && activeKeybinds().any { it.key < 0 && it.key.isKeyHeld() }
    fun allowKeyboardClick() = isEnabled() && activeKeybinds().any { it.key > 0 && it.key.isKeyHeld() }

    private fun activeKeybinds(): List<LoadoutBinding> = buildList {
        if (FarmingContestApi.isContestActive) {
            contestKeybinds.forEachIndexed { index, key -> add(LoadoutBinding(index, key, true)) }
        }
        keybinds.forEachIndexed { index, key -> add(LoadoutBinding(index, key, false)) }
    }

    private fun debug(message: String) {
        if (debugEnabled) ChatUtils.chat("§8[Loadout Debug] $message")
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && LoadoutApi.inLoadouts() && config.slotKeybindsToggle
}
