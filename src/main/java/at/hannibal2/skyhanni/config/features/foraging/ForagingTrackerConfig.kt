package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyModern
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class ForagingTrackerConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Track drops from foraging.")
    @ConfigEditorBoolean
    @OnlyModern
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = ForagingTrackerConfig::class, field = "enabled")
    val position: Position = Position(-300, 300)

    @Expose
    @ConfigOption(name = "Compact Gifts", desc = "Compact the chat messages when you receive a tree gift.")
    @ConfigEditorBoolean
    @OnlyModern
    @FeatureToggle
    var compactGiftChats: Boolean = true


    @Expose
    @ConfigOption(name = "Compact Gifts Bonus Drops", desc = "Lets you decide what types of bonus drops should be included in Compact Gifts messages.")
    @ConfigEditorDraggableList
    val compactGiftBonusDropsList: Property<MutableList<TreeGiftBonusDropCategory>> = Property.of(
        mutableListOf(
            TreeGiftBonusDropCategory.UNCOMMON_DROPS,
            TreeGiftBonusDropCategory.ENCHANTED_BOOKS,
            TreeGiftBonusDropCategory.PHANTOMS,
            TreeGiftBonusDropCategory.BOOSTERS,
            TreeGiftBonusDropCategory.SHARDS,
            TreeGiftBonusDropCategory.RUNES,
            TreeGiftBonusDropCategory.TREE_THE_FISH,
        ),
    )

    @Suppress("MaxLineLength")
    enum class TreeGiftBonusDropCategory(private val displayName: String) {
        UNCOMMON_DROPS("§fUncommon Tree-Specific Drops\n§7(e.g. §aStretching Sticks§7)"),
        ENCHANTED_BOOKS("§fUltimate Enchantments\n§7(§d§lFirst Impression I §7& §d§lMissile I §7Books)"),
        PHANTOMS("§fHuntable Mobs\n§7(Phantoms)"),
        BOOSTERS("§fBoosters\n§7(Sweep, Foraging Wisdom)"),
        SHARDS("§fDirect Shard Drops\n§7(§bHummingbirds§7, §6Chameleons§7)"),
        RUNES("§fRunes\n§7(§fFading White §7and §aFading Green§7)"),
        TREE_THE_FISH("§cTree The Fish");

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Only Holding Axe", desc = "Only show the tracker while holding an axe.")
    @ConfigEditorBoolean
    @OnlyModern
    var onlyHoldingAxe: Boolean = true

    @Expose
    @ConfigOption(
        name = "Disappearing Delay",
        desc = "The delay in seconds before the tracker disappears after you stop holding an axe."
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 60f, minStep = 1f)
    @OnlyModern
    var disappearingDelay: Int = 15

    @Expose
    @ConfigOption(name = "Show Whole Trees", desc = "Estimate how many full trees you have chopped down, using percentage summing.")
    @ConfigEditorBoolean
    @OnlyModern
    var showWholeTrees: Boolean = true

}
