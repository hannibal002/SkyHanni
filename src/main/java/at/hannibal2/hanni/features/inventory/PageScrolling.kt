package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.ToolTipData
import at.hannibal2.hanni.events.InventoryOpenEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.compat.MouseCompat
import at.hannibal2.hanni.utils.renderables.ScrollValue
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@HanniModule
object PageScrolling {

    private val config get() = HanniMod.feature.inventory.pageScrolling

    private val patternGroup = RepoPattern.group("inventory.pagescrolling")

    private val illegalInventory by patternGroup.list(
        "illegal",
        "Large Chest",
        "Chest",
    )

    private val forwardPattern by patternGroup.list(
        "forward",
        "§aNext Page",
        "§aScroll Up",
        "§aLevels 26 - 50",
        "§aNext Page →",
        "§aScroll Right",
    )

    private val backwardPattern by patternGroup.list(
        "backward",
        "§aPrevious Page",
        "§aScroll Down",
        "§aLevels 1 - 25",
        "§a← Previous Page",
        "§aScroll Left",
    )

    private val scroll = ScrollValue()

    // these checks are to prevent cheat-like behaviour, where the player could scroll through the inventory without any delay
    // currentlyScrollable is the primary check, to see if the player is currently able to scroll,
    // with cooldown beeing a fallback to still allow for scrolling if currentlyScrollable is stuck
    private var currentlyScrollable = false
    private var cooldown = SimpleTimeMark.farPast()

    @HandleEvent
    fun onTick(event: HanniTickEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.inStorage() && InventoryUtils.isNeuStorageEnabled) return
        if (!currentlyScrollable && cooldown.isInFuture()) return
        if (!scroll.isMouseEventValid()) return

        val inventoryName = InventoryUtils.openInventoryName()
        if (inventoryName.isEmpty()) return
        if (illegalInventory.matches(inventoryName)) return

        if (ToolTipData.lastSlot != null) {
            if (!(config.invertBypass xor config.bypassKey.isKeyHeld())) return
        }

        val dWheel = MouseCompat.getScrollDelta()
        if (dWheel == 0) return
        val patterns = if ((dWheel > 0) xor config.invertScroll) forwardPattern else backwardPattern
        val slot = InventoryUtils.getItemsInOpenChest().firstOrNull {
            patterns.matches(it.stack?.displayName)
        } ?: return
        InventoryUtils.clickSlot(slot.slotNumber)

        currentlyScrollable = false
        cooldown = 1.0.seconds.fromNow()
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        currentlyScrollable = true
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enable && InventoryUtils.inInventory()
}
