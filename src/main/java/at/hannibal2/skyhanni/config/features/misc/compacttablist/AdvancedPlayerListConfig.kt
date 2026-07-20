package at.hannibal2.skyhanni.config.features.misc.compacttablist

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class AdvancedPlayerListConfig {
    @Expose
    @ConfigOption(name = "Player Sort", desc = "Change the sort order of player names in the tab list.")
    @ConfigEditorDropdown
    var playerSortOrder: Property<PlayerSortEntry> = Property.of(PlayerSortEntry.RANK)

    enum class PlayerSortEntry(private val displayName: String) {
        RANK("Rank (Default)"),
        SB_LEVEL("SB Level"),
        NAME("Name (Abc)"),
        PROFILE_TYPE("Ironman/Bingo"),
        SOCIAL_STATUS("Party/Friends/Guild"),
        RANDOM("Random"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Invert Sort", desc = "Flip the player list order on its head (also works with default rank).")
    @ConfigEditorBoolean
    val reverseSort: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Hide Player Icons", desc = "Hide the icons/skins of players in the tab list.")
    @ConfigEditorBoolean
    val hidePlayerIcons: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Hide Rank Color", desc = "Hide the player rank color.")
    @ConfigEditorBoolean
    val hideRankColor: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Hide Emblems", desc = "Hide the emblems behind the player name.")
    @ConfigEditorBoolean
    val hideEmblem: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Hide Level", desc = "Hide the SkyBlock level numbers.")
    @ConfigEditorBoolean
    val hideLevel: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Hide Level Brackets",
        desc = "Hide the gray brackets in front of and behind the level numbers."
    )
    @ConfigEditorBoolean
    val hideLevelBrackets: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Level Color As Name", desc = "Use the color of the SkyBlock level for the player color.")
    @ConfigEditorBoolean
    val useLevelColorForName: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Bingo Rank Number",
        desc = "Show the number of the bingo rank next to the icon. Useful if you are not so familiar with bingo."
    )
    @ConfigEditorBoolean
    val showBingoRankNumber: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Hide Factions", desc = "Hide the icon of the Crimson Isle Faction in the tab list.")
    @ConfigEditorBoolean
    val hideFactions: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Mark Special Persons",
        desc = "Show special icons behind the name of guild members, party members, friends, and marked players."
    )
    @ConfigEditorBoolean
    val markSpecialPersons: Property<Boolean> = Property.of(false)
}
