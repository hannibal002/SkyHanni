package at.hannibal2.skyhanni.config.features.misc.compacttablist

import at.hannibal2.skyhanni.config.HasLegacyId
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AdvancedPlayerListConfig {
    @Expose
    @ConfigOption(name = "Player Sort", desc = "Change the sort order of player names in the tab list.")
    @ConfigEditorDropdown
    var playerSortOrder: PlayerSortEntry = PlayerSortEntry.RANK

    enum class PlayerSortEntry(
        private val displayName: String,
        private val legacyId: Int = -1,
    ) : HasLegacyId {
        RANK("Rank (Default)", 0),
        SB_LEVEL("SB Level", 1),
        NAME("Name (Abc)", 2),
        PROFILE_TYPE("Ironman/Bingo", 3),
        SOCIAL_STATUS("Party/Friends/Guild", 4),
        RANDOM("Random", 5),
        ;

        override fun getLegacyId() = legacyId
        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Invert Sort", desc = "Flip the player list order on its head (also works with default rank).")
    @ConfigEditorBoolean
    var reverseSort: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Player Icons", desc = "Hide the icons/skins of player in the tab list.")
    @ConfigEditorBoolean
    var hidePlayerIcons: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Rank Color", desc = "Hide the player rank color.")
    @ConfigEditorBoolean
    var hideRankColor: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Emblems", desc = "Hide the emblems behind the player name.")
    @ConfigEditorBoolean
    var hideEmblem: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Level", desc = "Hide the SkyBlock level numbers.")
    @ConfigEditorBoolean
    var hideLevel: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hide Level Brackets",
        desc = "Hide the gray brackets in front of and behind the level numbers."
    )
    @ConfigEditorBoolean
    var hideLevelBrackets: Boolean = false

    @Expose
    @ConfigOption(name = "Level Color As Name", desc = "Use the color of the SkyBlock level for the player color.")
    @ConfigEditorBoolean
    var useLevelColorForName: Boolean = false

    @Expose
    @ConfigOption(
        name = "Bingo Rank Number",
        desc = "Show the number of the bingo rank next to the icon. Useful if you are not so familiar with bingo."
    )
    @ConfigEditorBoolean
    var showBingoRankNumber: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Factions", desc = "Hide the icon of the Crimson Isle Faction in the tab list.")
    @ConfigEditorBoolean
    var hideFactions: Boolean = false

    @Expose
    @ConfigOption(
        name = "Mark Special Persons",
        desc = "Show special icons behind the name of guild members, party members, friends, and marked players."
    )
    @ConfigEditorBoolean
    var markSpecialPersons: Boolean = false
}
