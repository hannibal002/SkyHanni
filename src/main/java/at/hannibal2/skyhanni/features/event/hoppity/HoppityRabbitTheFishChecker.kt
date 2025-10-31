package at.hannibal2.hanni.features.event.hoppity

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.events.inventory.AttemptedInventoryCloseEvent
import at.hannibal2.hanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.RegexUtils.anyMatches
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.RenderUtils.highlight
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.SoundUtils
import kotlin.time.Duration.Companion.seconds

@HanniModule
object HoppityRabbitTheFishChecker {

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: Chocolate Breakfast Egg
     * REGEX-TEST: Chocolate Lunch Egg
     * REGEX-TEST: Chocolate Dinner Egg
     * REGEX-TEST: Chocolate Brunch Egg
     * REGEX-TEST: Chocolate Déjeuner Egg
     * REGEX-TEST: Chocolate Supper Egg
     */
    val mealEggInventoryPattern by CFApi.patternGroup.pattern(
        "inventory.mealegg.name",
        "(?:§.)*Chocolate (?:Breakfast|Lunch|Dinner|Brunch|Déjeuner|Supper) Egg.*",
    )

    /**
     * REGEX-TEST: §cRabbit the Fish
     */
    private val rabbitTheFishItemPattern by CFApi.patternGroup.pattern(
        "item.rabbitthefish",
        "(?:§.)*Rabbit the Fish",
    )

    /**
     * REGEX-TEST: Click to open Chocolate Factory!
     */
    private val openCfSlotLorePattern by CFApi.patternGroup.pattern(
        "inventory.mealegg.continue",
        "(?:§.)*Click to open Chocolate Factory!",
    )
    // </editor-fold>

    private val config get() = HanniMod.feature.event.hoppityEggs
    private var rabbitTheFishIndex: Int? = null

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return

        val index = rabbitTheFishIndex ?: return
        InventoryUtils.getItemsInOpenChest().firstOrNull { it.slotIndex == index }?.highlight(LorenzColor.RED)
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        rabbitTheFishIndex = null
        if (!isEnabled() || !mealEggInventoryPattern.matches(event.inventoryName)) return

        rabbitTheFishIndex = event.inventoryItems.filter {
            it.value.displayName.isNotEmpty() && it.key != 22
        }.entries.firstOrNull {
            rabbitTheFishItemPattern.matches(it.value.displayName)
        }?.key
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() || rabbitTheFishIndex == null) return

        // Prevent opening chocolate factory when Rabbit the Fish is present
        val stack = event.slot?.stack ?: return
        if (openCfSlotLorePattern.anyMatches(stack.getLore())) {
            event.sendPreventClosureTitle()
        } else if (rabbitTheFishIndex == event.slot.slotIndex) {
            rabbitTheFishIndex = null
        }
    }

    @HandleEvent
    fun onAttemptedInventoryClose(event: AttemptedInventoryCloseEvent) {
        if (!isEnabled() || rabbitTheFishIndex == null) return
        event.sendPreventClosureTitle()
    }

    private fun HanniEvent.Cancellable.sendPreventClosureTitle() {
        TitleManager.sendTitle(
            "§cRabbit the Fish Prevented Close",
            subtitleText = "§7Hold §eShift §7to bypass",
            duration = 5.seconds,
            location = TitleManager.TitleLocation.INVENTORY,
        )
        SoundUtils.playErrorSound()
        cancel()
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && HoppityApi.isHoppityEvent() && config.preventMissingRabbitTheFish
}
