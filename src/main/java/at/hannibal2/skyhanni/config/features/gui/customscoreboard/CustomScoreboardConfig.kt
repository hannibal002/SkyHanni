package at.hannibal2.skyhanni.config.features.gui.customscoreboard

import at.hannibal2.skyhanni.utils.OSUtils.openBrowser
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CustomScoreboardConfig {

    @ConfigOption(
        name = "Removed Feature",
        desc = "SkyHanni's CustomScoreboard Has Been Removed. Please switch to the mod for more & unique features.",
    )
    @ConfigEditorButton(buttonText = "Download the Mod")
    val customScoreboardMod: Runnable = Runnable { openBrowser("https://modrinth.com/mod/skyblock-custom-scoreboard") }
}
