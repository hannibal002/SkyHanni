package at.hannibal2.skyhanni.config.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.data.DateFormat
//#if TODO
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.NumberDisplayFormat
//#endif
import at.hannibal2.skyhanni.utils.RenderUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

// todo 1.21 impl needed
class DisplayConfig {
    @Expose
    @ConfigOption(name = "Alignment Options", desc = "")
    @Accordion
    var alignment: AlignmentConfig = AlignmentConfig()

    @Expose
    @ConfigOption(name = "Arrow Options", desc = "")
    @Accordion
    var arrow: ArrowConfig = ArrowConfig()

    @Expose
    @ConfigOption(name = "Chunked Stats Options", desc = "")
    @Accordion
    var chunkedStats: ChunkedStatsConfig = ChunkedStatsConfig()

    @Expose
    @ConfigOption(name = "Events Options", desc = "")
    @Accordion
    var events: EventsConfig = EventsConfig()

    @Expose
    @ConfigOption(name = "Maxwell Options", desc = "")
    @Accordion
    var maxwell: MaxwellConfig = MaxwellConfig()

    @Expose
    @ConfigOption(name = "Mayor Options", desc = "")
    @Accordion
    var mayor: MayorConfig = MayorConfig()

    @Expose
    @ConfigOption(name = "Party Options", desc = "")
    @Accordion
    var party: PartyConfig = PartyConfig()

    @Expose
    @ConfigOption(name = "Title and Footer Options", desc = "")
    @Accordion
    var titleAndFooter: TitleAndFooterConfig = TitleAndFooterConfig()

    @Expose
    @ConfigOption(
        name = "Hide Vanilla Scoreboard",
        desc = "Hide the vanilla scoreboard.\n" +
            "§cMods that add their own scoreboard will not be affected by this setting!",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideVanillaScoreboard: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Show earned/lost", desc = "Show the amount you earned/lost on any Number display.")
    @ConfigEditorBoolean
    var showNumberDifference: Boolean = false

    @Expose
    @ConfigOption(name = "Use Custom Lines", desc = "Use custom lines instead of the default ones.")
    @ConfigEditorBoolean
    var useCustomLines: Boolean = true

    @Expose
    @ConfigOption(name = "Show unclaimed bits", desc = "Show the amount of available Bits that can still be claimed.")
    @ConfigEditorBoolean
    var showUnclaimedBits: Boolean = false

    @Expose
    @ConfigOption(
        name = "Show Max Island Players",
        desc = "Show the maximum amount of players that can join your current island.",
    )
    @ConfigEditorBoolean
    var showMaxIslandPlayers: Boolean = true

    @Expose
    @ConfigOption(name = "Powder Display", desc = "Select how the powder display should be formatted.")
    @ConfigEditorDropdown
    var powderDisplay: PowderDisplay = PowderDisplay.AVAILABLE

    enum class PowderDisplay(private val displayName: String) {
        AVAILABLE("Available"),
        TOTAL("Total"),
        BOTH("Available / All"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Number Format", desc = "Select how numbers should be formatted.")
    @ConfigEditorDropdown
    var numberFormat: NumberFormat = NumberFormat.LONG

    enum class NumberFormat(private val displayName: String) {
        LONG("1,234,567"),
        SHORT("1.2M");

        override fun toString(): String {
            return displayName
        }
    }

    //#if TODO
    @Expose
    @ConfigOption(
        name = "Number Display Format",
        desc = "Select how numbers with their prefix and color should be formatted.",
    )
    @ConfigEditorDropdown
    var numberDisplayFormat: NumberDisplayFormat = NumberDisplayFormat.TEXT_COLOR_NUMBER
    //#endif

    @Expose
    @ConfigOption(
        name = "SkyBlock Time 24h Format",
        desc = "Display the current SkyBlock time in 24hr format rather than 12h Format.",
    )
    @ConfigEditorBoolean
    var skyblockTime24hFormat: Boolean = false

    @Expose
    @ConfigOption(
        name = "SkyBlock Time Exact Minutes",
        desc = "Display the exact minutes in the SkyBlock time, rather than only 10 minute increments.",
    )
    @ConfigEditorBoolean
    var skyblockTimeExactMinutes: Boolean = false

    @Expose
    @ConfigOption(
        name = "Date in Lobby Code",
        desc = "Show the current date infront of the server name, like Hypixel does.",
    )
    @ConfigEditorBoolean
    var dateInLobbyCode: Boolean = true

    @Expose
    @ConfigOption(name = "Lobby Code Date Format", desc = "Select your preferred date format.")
    @ConfigEditorDropdown
    var dateFormat: DateFormat = DateFormat.US_SLASH_MMDDYYYY

    @Expose
    @ConfigOption(name = "Line Spacing", desc = "The amount of space between each line.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 20f, minStep = 1f)
    var lineSpacing: Int = 10

    @Expose
    @ConfigOption(
        name = "Text Alignment",
        desc = "Will align the text to the left, center or right, while not overriding certain lines, like title or footer.",
    )
    @ConfigEditorDropdown
    var textAlignment: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT

    @Expose
    @ConfigOption(name = "Show Profile Name", desc = "Show profile name instead of the type in the profile element.")
    @ConfigEditorBoolean
    var showProfileName: Boolean = false

    @Expose
    @ConfigOption(
        name = "Cache Scoreboard on Island Switch",
        desc = "Will stop the Scoreboard from updating while switching islands.\n" +
            "Removes the shaking when loading data.",
    )
    @ConfigEditorBoolean
    var cacheScoreboardOnIslandSwitch: Boolean = false
}
