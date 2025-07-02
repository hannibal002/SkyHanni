package at.hannibal2.skyhanni.config.features.garden.tracking

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.garden.FarmingStatusTracker
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption


class FarmingStatusTrackerConfig {

    @ConfigOption(
        name = "§cNotice",
        desc = "This feature allows you to send your in-game farming stats to a Discord webhook " +
            "that §6YOU §7choose. §4NO §7sensitive data, e.g. session tokens, is ever collected or sent.",
    )
    @ConfigEditorInfoText
    var notice: String = ""

    @ConfigOption(
        name = "Source",
        desc = "Click to open the source code for this feature.\n" +
            "§eClicking this will open a webpage in your browser.",
    )
    @ConfigEditorButton(buttonText = "OPEN")
    val sourceCodeUrl: Runnable = Runnable {
        OSUtils.openBrowser(
            "https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/" +
                "hannibal2/skyhanni/features/garden/tracking/FarmingStatusTracker.kt",
        )
    }

    @ConfigOption(
        name = "Tutorial",
        desc = "Click to get a tutorial sent into your chat.",
    )
    @ConfigEditorButton(buttonText = "SEND")
    val tutorial: Runnable = Runnable(FarmingStatusTracker::sendTutorial)

    @Expose
    @ConfigOption(name = "Enabled", desc = "Send an embed with the options you selected below to your specified webhook.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Webhook Settings", desc = "")
    @Accordion
    val webhook: WebhookConfig = WebhookConfig()

    @Expose
    @ConfigOption(name = "Embed Settings", desc = "")
    @Accordion
    val embed: EmbedConfig = EmbedConfig()
}
