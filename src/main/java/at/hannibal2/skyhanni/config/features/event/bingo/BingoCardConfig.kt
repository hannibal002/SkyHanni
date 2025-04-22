package at.hannibal2.skyhanni.config.features.event.bingo

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class BingoCardConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Display the Bingo Card.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Quick Toggle",
        desc = "Quickly show/hide the Bingo Card (when enabled above) or the step helper by sneaking with SkyBlock Menu in hand."
    )
    @ConfigEditorBoolean
    var quickToggle: Boolean = true

    @Expose
    @ConfigOption(
        name = "Bingo Steps",
        desc = "Show help with the next step in Bingo instead of the Bingo Card.\n" +
            "§cThis feature is in early development. Expect bugs and missing goals."
    )
    @ConfigEditorBoolean
    var stepHelper: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Community Goals", desc = "Hide Community Goals from the Bingo Card display.")
    @ConfigEditorBoolean
    var hideCommunityGoals: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Show Guide",
        desc = "Show tips and difficulty for bingo goals inside the Bingo Card inventory.\n" +
            "These tips are made from inspirations and guides from the community, aiming to help you to complete the bingo card."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var bingoSplashGuide: Boolean = true

    @Expose
    @ConfigLink(owner = BingoCardConfig::class, field = "enabled")
    var bingoCardPos: Position = Position(10, 10, false, true)

    @Expose
    @ConfigOption(
        name = "Next Tip Duration",
        desc = "Show the duration until the next hidden personal goal gets a tip revealed."
    )
    @ConfigEditorBoolean
    var nextTipDuration: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Hide Difficulty When Done",
        desc = "Remove the background difficulty color in the bingo card inventory when the goal is done."
    )
    @ConfigEditorBoolean
    var hideDoneDifficulty: Boolean = true

    @Expose
    @ConfigOption(
        name = "Community Percentages",
        desc = "Send a chat message with the change of community goal percentages after opening the bingo card inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var communityGoalProgress: Boolean = true
}
