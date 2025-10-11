package at.hannibal2.skyhanni.features.inventory.keyboardcontrol

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.inventory.keyboardcontrol.KeyBinding.Companion.bindNumberKeysToItems
import at.hannibal2.skyhanni.features.inventory.keyboardcontrol.KeyBinding.Companion.createPatternBindings
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object MenuSacks {
    private val config get() = SkyHanniMod.feature.inventory.keyboardControl
    private val patternGroup = RepoPattern.group("keyboardcontrol.sacks")

    // -- BUTTONS --
    private val insertInventoryButtonPattern by patternGroup.pattern("button.insertinventory", "Insert inventory")
    private val goBackButtonPattern by patternGroup.pattern("button.back", "Go Back")
    private val closeButtonPattern by patternGroup.pattern("button.close", "Close")
    private val pickupAllButtonPattern by patternGroup.pattern("button.single.pickupall", "Pickup All")

    // -- TITLES --
    private val sackOfSacksTitlePattern by patternGroup.pattern("title.main", "Sack of Sacks")

    /**
     * REGEX-TEST: Dungeon Sack
     */
    private val singleSackTitlePattern by patternGroup.pattern("title.single", ".* Sack")

    private val menus = arrayOf(
        // Top level "Sack of Sacks" menu
        UiMenu(
            titlePattern = sackOfSacksTitlePattern,
            buttonPatterns = arrayOf(insertInventoryButtonPattern, goBackButtonPattern, closeButtonPattern),
            getBindings = { snapshot ->
                createPatternBindings {
                    config.sacks.insertInventory to insertInventoryButtonPattern
                    config.shared.back to goBackButtonPattern
                } + bindNumberKeysToItems(config, snapshot, intArrayOf(), 1)
            },
        ),

        // Individual sack page
        UiMenu(
            titlePattern = singleSackTitlePattern,
            buttonPatterns = arrayOf(pickupAllButtonPattern, insertInventoryButtonPattern, goBackButtonPattern),
            getBindings = { snapshot ->
                createPatternBindings {
                    config.sacks.pickupAll to pickupAllButtonPattern
                    config.sacks.insertInventory to insertInventoryButtonPattern
                    config.shared.back to goBackButtonPattern
                } + bindNumberKeysToItems(config, snapshot)
            },
        ),
    )

    init {
        Registry.registerMenus(menus)
    }
}
