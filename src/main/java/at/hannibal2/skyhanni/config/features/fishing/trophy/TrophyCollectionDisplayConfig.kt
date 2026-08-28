package at.hannibal2.skyhanni.config.features.fishing.trophy

import at.hannibal2.skyhanni.config.core.config.Position
import io.github.notenoughupdates.moulconfig.observer.Property

/**
 * Shared config surface for the trophy collection displays (Trophy Fish, Trophy Frogs).
 * Implemented by the concrete display configs so [TrophyCollectionDisplay] can read them generically.
 *
 * This is an interface rather than a base class on purpose: the config tooling iterates
 * `javaClass.declaredFields` (non-inherited), so the `@Expose` fields must be declared on each
 * concrete config. Sharing them via a base class would hide them from those passes.
 */
@Suppress("StorageNeedsExpose")
interface TrophyCollectionDisplayConfig {
    val enabled: Property<Boolean>
    val whenToShow: Property<WhenToShow>
    val keybind: Int
    val requireArmor: Property<Boolean>
    val highlightNew: Property<Boolean>
    val extraSpace: Property<Int>
    val sortingType: Property<TrophySorting>
    val reverseOrder: Property<Boolean>
    val textOrder: Property<MutableList<TextPart>>
    val showCross: Property<Boolean>
    val showCheckmark: Property<Boolean>
    val onlyShowMissing: Property<HideCaught>
    val showCaughtHigher: Property<Boolean>
    val position: Position

    enum class WhenToShow(private val displayName: String) {
        ALWAYS("Always"),
        ONLY_IN_INVENTORY("In inventory"),
        ONLY_WITH_ROD_IN_HAND("Rod in hand"),
        ONLY_WITH_KEYBIND("On keybind"),
        ;

        override fun toString() = displayName
    }

    enum class TrophySorting(private val displayName: String) {
        ITEM_RARITY("Item Rarity"),
        TOTAL_AMOUNT("Total Amount"),
        BRONZE_AMOUNT("Bronze Amount"),
        SILVER_AMOUNT("Silver Amount"),
        GOLD_AMOUNT("Gold Amount"),
        DIAMOND_AMOUNT("Diamond Amount"),
        HIGHEST_RARITY("Highest Rarity"),
        NAME("Name Alphabetical"),
        ;

        override fun toString() = displayName
    }

    enum class TextPart(private val displayName: String) {
        ICON("Icon"),
        NAME("Name"),
        BRONZE("Amount Bronze"),
        SILVER("Amount Silver"),
        GOLD("Amount Gold"),
        DIAMOND("Amount Diamond"),
        TOTAL("Amount Total"),
        ;

        override fun toString() = displayName
    }

    enum class HideCaught(private val displayName: String) {
        NONE("Show All"),
        BRONZE("Bronze"),
        SILVER("Silver"),
        GOLD("Gold"),
        DIAMOND("Diamond"),
        ;

        override fun toString() = displayName
    }
}
