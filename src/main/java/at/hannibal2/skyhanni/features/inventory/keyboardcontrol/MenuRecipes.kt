package at.hannibal2.skyhanni.features.inventory.keyboardcontrol

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.inventory.keyboardcontrol.KeyBinding.Companion.bindNumberKeysToItems
import at.hannibal2.skyhanni.features.inventory.keyboardcontrol.KeyBinding.Companion.bindNumberKeysToSlots
import at.hannibal2.skyhanni.features.inventory.keyboardcontrol.KeyBinding.Companion.createPatternBindings
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import org.lwjgl.input.Keyboard

@SkyHanniModule
object MenuRecipes {
    private val config get() = SkyHanniMod.feature.inventory.keyboardControl
    private val patternGroup = RepoPattern.group("keyboardcontrol.recipes")

    // -- BUTTONS --
    private val previousPageButtonPattern by patternGroup.pattern("button.page.previous", "Previous Page")
    private val nextPageButtonPattern by patternGroup.pattern("button.page.next", "Next Page")
    private val closeButtonPattern by patternGroup.pattern("button.close", "Close")
    private val searchRecipesButtonPattern by patternGroup.pattern("button.list.search", "Search Recipes")

    /**
     * REGEX-TEST: Supercraft
     * REGEX-TEST: Supercraft (69/420)
     */
    private val supercraftButtonPattern by patternGroup.pattern("button.specific.supercraft", "Supercraft.*")
    private val goBackButtonPattern by patternGroup.pattern("button.back", "Go Back")
    private val previousRecipeButtonPattern by patternGroup.pattern("button.specific.previousrecipe", "Previous Recipe")
    private val nextRecipeButtonPattern by patternGroup.pattern("button.specific.nextrecipe", "Next Recipe")

    // -- TITLES --
    /**
     * REGEX-TEST: "sa" Recipes (1/3)
     * REGEX-TEST: "golden c" Recipes (1/1)
     */
    private val recipesListTitlePattern by patternGroup.pattern("title.list", ".* Recipes .*")

    /**
     * REGEX-TEST: Spooky Sack Recipe
     */
    private val specificRecipeTitlePattern by patternGroup.pattern("title.specific", ".* Recipe")

    // 3x3 crafting table grid.
    @Suppress("MagicNumber")
    private val recipeItemsSelectionSlots = intArrayOf(
        10, 11, 12,
        19, 20, 21,
        28, 29, 30,
    )

    private val menus = arrayOf(
        // Top-level recipe list menu
        UiMenu(
            titlePattern = recipesListTitlePattern,
            buttonPatterns = arrayOf(
                searchRecipesButtonPattern,
                previousPageButtonPattern,
                nextPageButtonPattern,
                goBackButtonPattern,
                closeButtonPattern,
            ),
            getBindings = { snapshot ->
                createPatternBindings {
                    config.shared.search to searchRecipesButtonPattern
                    config.shared.previousPage to previousPageButtonPattern
                    config.shared.nextPage to nextPageButtonPattern
                    config.shared.back to goBackButtonPattern
                } + bindNumberKeysToItems(config, snapshot, intArrayOf(), 1)
            },
        ),

        // Specific recipe menu
        UiMenu(
            titlePattern = specificRecipeTitlePattern,
            buttonPatterns = arrayOf(
                supercraftButtonPattern,
                previousRecipeButtonPattern,
                nextRecipeButtonPattern,
                goBackButtonPattern,
                closeButtonPattern,
            ),
            getBindings = { snapshot ->
                createPatternBindings {
                    // normal key press maps to LMB click (craft one)
                    config.recipes.supercraft to supercraftButtonPattern
                    // shift-press maps to shift LMB click (set craft amount to max)
                    config.recipes.supercraft to supercraftButtonPattern with intArrayOf(Keyboard.KEY_LSHIFT) mouse 0 mode 1
                    // ctrl-press maps to RMB click (pick amount)
                    config.recipes.supercraft to supercraftButtonPattern with intArrayOf(Keyboard.KEY_LCONTROL) mouse 1 mode 0
                    config.shared.previousPage to previousRecipeButtonPattern
                    config.shared.nextPage to nextRecipeButtonPattern
                    config.shared.back to goBackButtonPattern
                } + bindNumberKeysToSlots(config, recipeItemsSelectionSlots)
            },
        ),
    )


    init {
        Registry.registerMenus(menus)
    }
}
