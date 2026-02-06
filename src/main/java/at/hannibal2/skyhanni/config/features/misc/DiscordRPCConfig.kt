package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.misc.discordrpc.AutoStatus
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

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
        PriorityEntry.AFK,
    )

    enum class PriorityEntry(private val displayName: String, val associatedAutoStatus: AutoStatus) {
        CROP_MILESTONES("Crop Milestones", AutoStatus.CROP_MILESTONES),
        SLAYER("Slayer", AutoStatus.SLAYER),
        STACKING_ENCHANT("Stacking Enchantment", AutoStatus.STACKING),
        DUNGEONS("Dungeon", AutoStatus.DUNGEONS),
        AFK("AFK Indicator", AutoStatus.AFK),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Dynamic Fallback",
        desc = "What to show when none of your \"Dynamic Priority\" statuses are active.",
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

    enum class LineEntry(private val displayName: String) {
        NOTHING("Nothing"),
        LOCATION("Location"),
        PURSE("Purse"),
        BITS("Bits"),
        STATS("Stats"),
        HELD_ITEM("Held Item"),
        SKYBLOCK_DATE("SkyBlock Date"),
        PROFILE("Profile"),
        SLAYER("Slayer"),
        CUSTOM("Custom"),
        DYNAMIC("Dynamic"),
        CROP_MILESTONE("Crop Milestone"),
        CURRENT_PET("Current Pet"),
        ;

        override fun toString() = displayName
    }

    @ConfigOption(
        name = "Credits",
        desc = "Rich presence assets were created by\n" +
            "Hypixel Pack HQ (packshq.com) for the old images\n" +
            "and @unfamiliartunes (unfamiliartunes.straw.page) for the new images",
    )
    @ConfigEditorInfoText
    val credits = ""
}
