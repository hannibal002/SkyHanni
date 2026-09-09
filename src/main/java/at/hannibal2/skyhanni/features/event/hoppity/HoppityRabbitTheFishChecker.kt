package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.inventory.AttemptedInventoryCloseEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
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

    private val config get() = SkyHanniMod.feature.event.hoppityEggs
    private var rabbitTheFishIndex: Int? = null

    @HandleEvent
    private fun onBackgroundDrawn() {
        if (!isEnabled()) return

        val index = rabbitTheFishIndex ?: return
        InventoryUtils.getItemsInOpenChest().firstOrNull { it.containerSlot == index }?.highlight(LorenzColor.RED)
    }

    @HandleEvent
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        rabbitTheFishIndex = null
        if (!isEnabled() || !mealEggInventoryPattern.matches(event.inventoryName)) return

        rabbitTheFishIndex = event.inventoryItems.filter {
            it.value.hoverName.string.isNotEmpty() && it.key != 22
        }.entries.firstOrNull {
            rabbitTheFishItemPattern.matches(it.value.hoverName.formattedTextCompatLeadingWhiteLessResets())
        }?.key
    }

    @HandleEvent(priorityLevel = HIGHEST)
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() || rabbitTheFishIndex == null) return

        // Prevent opening Chocolate Factory when Rabbit the Fish is present
        val slot = event.slot ?: return
        if (openCfSlotLorePattern.anyMatches(slot.item.getLore())) {
            event.sendPreventClosureTitle()
        } else if (rabbitTheFishIndex == slot.containerSlot) {
            rabbitTheFishIndex = null
        }
    }

    @HandleEvent
    private fun onAttemptedInventoryClose(event: AttemptedInventoryCloseEvent) {
        if (!isEnabled() || rabbitTheFishIndex == null) return
        event.sendPreventClosureTitle()
    }

    @HandleEvent
    private fun onInventoryClose() {
        rabbitTheFishIndex = null
    }

    private fun SkyHanniEvent.Cancellable.sendPreventClosureTitle() {
        TitleManager.sendTitle(
            "§cRabbit the Fish Prevented Close",
            subtitleText = "§7Hold §eShift §7to bypass",
            duration = 5.seconds,
            location = INVENTORY,
        )
        SoundUtils.playErrorSound()
        cancel()
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && HoppityApi.isHoppityEvent() && config.preventMissingRabbitTheFish
}
