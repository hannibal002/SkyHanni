package at.hannibal2.skyhanni.features.inventory.keyboardcontrol

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.inventory.KeyboardControlConfig
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.isStainedGlassPane
import net.minecraft.item.ItemStack
import org.lwjgl.input.Keyboard
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.milliseconds

// All menu definitions from all keyboard control classes (auction, bazaar, etc.)
// This way we can centralize processing to avoid (some of) redundant work
object Registry {
    private val allMenuDefinitions = arrayListOf<UiMenu>()

    fun registerMenu(menu: UiMenu) {
        allMenuDefinitions.add(menu)
    }

    fun registerMenus(menus: Array<UiMenu>) {
        allMenuDefinitions.addAll(menus)
    }

    fun getAllDefinitions(): ArrayList<UiMenu> {
        return allMenuDefinitions
    }
}

// Snapshot of opened inventory (menu) state
data class MenuSnapshot(
    val patternToSlot: Map<String, Int>,
    // items are all non-empty non-glass-pane stacks before first ui button (corresponding to how most Hypixel UIs work atm)
    val itemSlots: IntArray,
    // Sometimes you have 2 menus that cannot be distinguished by name pattern
    // There is "menuIndicatorPatterns" to provide a way to check which one it is by matched slots inside
    // So far, having boolean flag to tell 2 menus apart is enough (for bazaar specific item options and some submenu)
    // In the future, more generic way to do so might be required
    val isVariantMenu: Boolean,
)

// for key -> list of possible (slot + condition + mouse & mode) for this key
data class KeyActionEntry(
    val slot: Int,
    val modifiers: IntArray,
    val mouseButton: Int,
    val mode: Int,
)

// for slot -> list of possible (key + condition + mouse & mode) that map to this slot
data class SlotActionEntry(
    val key: Int,
    val modifiers: IntArray,
    val mouseButton: Int,
    val mode: Int,
)

sealed class KeyBinding {
    abstract val key: Int
    abstract val modifiers: IntArray
    abstract val mouseButton: Int
    abstract val mode: Int

    // lf Sized tagged union
    data class PatternBinding(
        override val key: Int,
        val pattern: Pattern,
        override val modifiers: IntArray = intArrayOf(),
        override val mouseButton: Int = 0,
        override val mode: Int = 0,
    ) : KeyBinding()

    data class SlotBinding(
        override val key: Int,
        val slot: Int,
        override val modifiers: IntArray = intArrayOf(),
        override val mouseButton: Int = 0,
        override val mode: Int = 0,
    ) : KeyBinding()

    companion object {
        private fun getNumberKeys(config: KeyboardControlConfig) = intArrayOf(
            config.shared.number1, config.shared.number2, config.shared.number3,
            config.shared.number4, config.shared.number5, config.shared.number6,
            config.shared.number7, config.shared.number8, config.shared.number9,
        )

        fun bindKeysToSlots(
            keys: IntArray,
            slots: IntArray,
            modifiers: IntArray = intArrayOf(),
            mouseButton: Int = 0,
            mode: Int = 0,
        ): List<KeyBinding> {
            val n = minOf(keys.size, slots.size)
            val out = ArrayList<KeyBinding>(n)
            for (i in 0 until n) {
                out.add(KeyBinding.SlotBinding(keys[i], slots[i], modifiers, mouseButton, mode))
            }
            return out
        }

        fun bindNumberKeysToSlots(
            config: KeyboardControlConfig,
            slots: IntArray,
            modifiers: IntArray = intArrayOf(),
            mouseButton: Int = 0,
            mode: Int = 0,
        ): List<KeyBinding> =
            bindKeysToSlots(getNumberKeys(config), slots, modifiers, mouseButton, mode)

        fun bindNumberKeysToItems(
            config: KeyboardControlConfig,
            snapshot: MenuSnapshot,
            modifiers: IntArray = intArrayOf(),
            mouseButton: Int = 0,
            mode: Int = 0,
        ): List<KeyBinding> =
            bindNumberKeysToSlots(config, snapshot.itemSlots, modifiers, mouseButton, mode)

        // Some nicety
        fun createPatternBindings(builder: BindingBuilder.() -> Unit): List<KeyBinding> {
            val builderInstance = BindingBuilder()
            builderInstance.builder()
            return builderInstance.bindings
        }

        // Even more nicety
        class BindingBuilder {
            internal val bindings = mutableListOf<PatternBinding>()
            infix fun Int.to(pattern: Pattern) = PatternBinding(this, pattern).also { bindings.add(it) }
            infix fun PatternBinding.with(modifierKeys: IntArray) = also {
                bindings.removeLast()
                bindings.add(copy(modifiers = modifierKeys))
            }

            infix fun PatternBinding.mouse(mb: Int) = also {
                bindings.removeLast()
                bindings.add(copy(mouseButton = mb))
            }

            infix fun PatternBinding.mode(m: Int) = also {
                bindings.removeLast()
                bindings.add(copy(mode = m))
            }
        }
    }
}

data class UiMenu(
    // how to recognize this menu title (regex pattern)
    val titlePattern: Pattern,
    // patterns for all UI buttons inside this menu (used to separate name UI buttons from "item" options)
    // if anything missing, some UI buttons might be parsed as "items"
    val buttonPatterns: Array<Pattern> = arrayOf(),
    // patterns that mark alternative menus we want to detect (affects snapshot.isItemOptionsMenu)
    val variantIndicators: Set<Pattern> = emptySet(),
    // produce KeyBinding's for this menu given the MenuSnapshot
    val getBindings: (snapshot: MenuSnapshot) -> List<KeyBinding>,
)


// Context that stores both raw and resolved/precomputed maps
data class KeybindContext(
    val snapshot: MenuSnapshot,
    val rawBinds: List<KeyBinding>,
    // key -> array of {slot + actions}
    val keyToActions: Map<Int, Array<KeyActionEntry>>,
    // track keybinds per slot
    val slotToKeybinds: Map<Int, Array<SlotActionEntry>>,
)


class MenuKeybindHandler {
    private val menuDefinitions: ArrayList<UiMenu> get() = Registry.getAllDefinitions()
    private val config get() = SkyHanniMod.feature.inventory.keyboardControl
    var context: KeybindContext? = null

    // track last click time for each key for cooldown
    private val lastClickTime = mutableMapOf<Int, SimpleTimeMark>()

    // Scan an inventory and produce a MenuSnapshot
    fun scanMenu(
        slotsMap: Map<Int, ItemStack>,
        // patterns for all the buttons in menu we are currently working with
        buttonPatternsForThisMenu: Array<Pattern>,
        // patterns to determine if menu is second variant we want to distinguish
        menuIndicatorPatterns: Set<Pattern>,
    ): MenuSnapshot {
        // order to detect "before first button"
        // TODO already sorted?
        val entries = slotsMap.entries.sortedBy { it.key }

        val buttonByPattern = linkedMapOf<String, Int>()
        val itemSlots = arrayListOf<Int>()
        var isVariantMenu = false
        var hasHitFirstButton = false

        for ((slot, stack) in entries) {
            val display = stack.displayName?.removeColor() ?: continue
            // TODO
            val internal = stack.getInternalNameOrNull()?.asString()?.removeColor() ?: display

            // try to match any button pattern
            for (pattern in buttonPatternsForThisMenu) {
                if (pattern.matches(display) || pattern.matches(internal)) {
                    val key = pattern.pattern()
                    buttonByPattern.putIfAbsent(key, slot)
                    if (pattern in menuIndicatorPatterns) isVariantMenu = true
                    hasHitFirstButton = true
                    break
                }
            }

            // collect items only until we encounter the first button
            if (!hasHitFirstButton && !stack.isStainedGlassPane()) {
                itemSlots.add(slot)
            }
        }

        return MenuSnapshot(buttonByPattern, itemSlots.toIntArray(), isVariantMenu)
    }

    // Called when an inventory is opened. Scans the inventory, obtains
    // KeyBinding's from the matched UiMenu, resolves patterns to slots and precomputes
    // lookup maps used by the keypress handler.
    fun calculateBindings(inventoryItems: Map<Int, ItemStack>, title: String) {
        val currentMenu = menuDefinitions.firstOrNull { it.titlePattern.matcher(title).matches() } ?: return

        val snapshot = try {
            scanMenu(inventoryItems, currentMenu.buttonPatterns, currentMenu.variantIndicators)
        } catch (ex: Exception) {
            ChatUtils.debug("Menu scan failed: ${ex.message}")
            return
        }

        val rawBinds = try {
            currentMenu.getBindings(snapshot)
        } catch (ex: Exception) {
            ChatUtils.debug("getBindings threw: ${ex.message}")
            return
        }

        val keyToActionsMutable = mutableMapOf<Int, ArrayList<KeyActionEntry>>()
        val slotToActionsMutable = mutableMapOf<Int, ArrayList<SlotActionEntry>>()

        for (bind in rawBinds) {
            val slot = when (bind) {
                is KeyBinding.SlotBinding -> bind.slot
                is KeyBinding.PatternBinding -> snapshot.patternToSlot[bind.pattern.pattern()]
            } ?: continue

            val key = bind.key
            val modifiers = bind.modifiers
            val mouseButton = bind.mouseButton
            val mode = bind.mode

            keyToActionsMutable.getOrPut(key) { arrayListOf() }
                .add(KeyActionEntry(slot, modifiers, mouseButton, mode))

            slotToActionsMutable.getOrPut(slot) { arrayListOf() }
                .add(SlotActionEntry(key, modifiers, mouseButton, mode))
        }

        // more specific (more modifiers) first ordering
        keyToActionsMutable.values.forEach { actions ->
            actions.sortWith(compareByDescending<KeyActionEntry> { it.modifiers.size }.thenBy { it.slot })
        }
        slotToActionsMutable.values.forEach { list ->
            list.sortWith(compareByDescending<SlotActionEntry> { it.modifiers.size }.thenBy { it.key })
        }

        val keyToActions = keyToActionsMutable.mapValues { it.value.toTypedArray() }
        val slotToActions = slotToActionsMutable.mapValues { it.value.toTypedArray() }

        context = KeybindContext(snapshot, rawBinds, keyToActions, slotToActions)
        return
    }

    // Check the precomputed context for actions for this key,
    // find actions whose modifiers are currently held, and execute them
    internal fun handleKeyPress(event: GuiKeyPressEvent, pressedKey: Int) {
        val ctx = context ?: return
        val actionsForKey = ctx.keyToActions[pressedKey] ?: return
        val currentTime = SimpleTimeMark.now()

        for (action in actionsForKey) {
            if (action.modifiers.all { it.isKeyHeld() }) {
                val lastTime = lastClickTime.getOrDefault(pressedKey, SimpleTimeMark.farPast())
                if (currentTime - lastTime >= config.clickCooldown.toLong().milliseconds) {
                    InventoryUtils.clickSlot(action.slot, null, action.mouseButton, action.mode)
                    lastClickTime[pressedKey] = currentTime
                    event.cancel()
                    return
                }
            }
        }
    }


    internal fun clearContext() {
        context = null
        lastClickTime.clear()
    }
}

@SkyHanniModule
object InventoryKeybindSystem {
    val handler = MenuKeybindHandler()
    private val config get() = SkyHanniMod.feature.inventory.keyboardControl

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return
        handler.clearContext()
        // InventoryUtils.getItemsInOpenChest is out-of-sync so we use event.inventoryItems
        // builds (somewhat expensive) context (key to action map) once and then reuses on clicks
        // in worst case, we will use it once (no time saved / lost)
        handler.calculateBindings(event.inventoryItems, event.inventoryName.removeColor())
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiKeyPress(event: GuiKeyPressEvent) {
        if (!isEnabled()) return
        //#if MC < 1.21
        val pressed = Keyboard.getEventKey()
        try {
            handler.handleKeyPress(event, pressed)
        } catch (e: Exception) {
            ChatUtils.debug("handleKeyPress threw: ${e.message}")
        }
        //#else
        //$$ val ctx = handler.context ?: return
        //$$ for (key in ctx.keyToActions.keys) {
        //$$     try {
        //$$         if (key.isKeyHeld()) {
        //$$             handler.handleKeyPress(event, key)
        //$$             return
        //$$         }
        //$$     } catch (e: Exception) {
        //$$         ChatUtils.debug("handleKeyPress threw: ${e.message}")
        //$$     }
        //$$ }
        //#endif
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.keybindsEnabled
}
