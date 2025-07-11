package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.HasLegacyId
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import java.util.*

class DiscordRPCConfig {
    @Expose
    @ConfigOption(name = "Enable Discord RPC", desc = "Details about your SkyBlock session displayed through Discord.")
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "First Line", desc = "Decide what to show in the first line.")
    @ConfigEditorDropdown
    val firstLine: Property<LineEntry> = Property.of(LineEntry.NOTHING)

    @Expose
    @ConfigOption(name = "Second Line", desc = "Decide what to show in the second line.")
    @ConfigEditorDropdown
    val secondLine: Property<LineEntry> = Property.of(LineEntry.NOTHING)

    @Expose
    @ConfigOption(name = "Custom", desc = "What should be displayed if you select \"Custom\" above.")
    @ConfigEditorText
    val customText: Property<String> = Property.of("")

    @Expose
    @ConfigOption(
        name = "Dynamic Priority",
        desc = "Disable certain dynamic statuses, or change the priority in case " +
            "two are triggered at the same time (higher up means higher priority)."
    )
    @ConfigEditorDraggableList
    val autoPriority: MutableList<PriorityEntry> = mutableListOf(
        PriorityEntry.CROP_MILESTONES,
        PriorityEntry.SLAYER,
        PriorityEntry.STACKING_ENCHANT,
        PriorityEntry.DUNGEONS,
        PriorityEntry.AFK
    )

    enum class PriorityEntry(
        private val displayName: String,
        private val legacyId: Int = -1,
    ) : HasLegacyId {
        CROP_MILESTONES("Crop Milestones", 0),
        SLAYER("Slayer", 1),
        STACKING_ENCHANT("Stacking Enchantment", 2),
        DUNGEONS("Dungeon", 3),
        AFK("AFK Indicator", 4),
        ;

        override fun getLegacyId() = legacyId
        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Dynamic Fallback",
        desc = "What to show when none of your \"Dynamic Priority\" statuses are active."
    )
    @ConfigEditorDropdown
    val auto: Property<LineEntry> = Property.of(LineEntry.NOTHING)

    @Expose
    @ConfigOption(name = "Show Button for SkyCrypt", desc = "Add a button to the RPC that opens your SkyCrypt profile.")
    @ConfigEditorBoolean
    val showSkyCryptButton: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Show Button for EliteBot", desc = "Add a button to the RPC that opens your EliteBot profile.")
    @ConfigEditorBoolean
    val showEliteBotButton: Property<Boolean> = Property.of(true)

    enum class LineEntry(
        private val displayName: String,
        private val legacyId: Int = -1,
    ) : HasLegacyId {
        NOTHING("Nothing", 0),
        LOCATION("Location", 1),
        PURSE("Purse", 2),
        BITS("Bits", 3),
        STATS("Stats", 4),
        HELD_ITEM("Held Item", 5),
        SKYBLOCK_DATE("SkyBlock Date", 6),
        PROFILE("Profile", 7),
        SLAYER("Slayer", 8),
        CUSTOM("Custom", 9),
        DYNAMIC("Dynamic", 10),
        CROP_MILESTONE("Crop Milestone", 11),
        CURRENT_PET("Current Pet", 12),
        ;

        override fun getLegacyId() = legacyId
        override fun toString() = displayName
    }
}
