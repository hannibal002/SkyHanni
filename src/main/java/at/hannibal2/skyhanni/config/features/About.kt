package at.hannibal2.skyhanni.config.features

import at.hannibal2.skyhanni.features.misc.update.ConfigVersionDeprecatedDisplay
import at.hannibal2.skyhanni.features.misc.update.ConfigVersionDisplay
import at.hannibal2.skyhanni.utils.OSUtils.openBrowser
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import io.github.notenoughupdates.moulconfig.observer.Property

class About {
    @ConfigOption(name = "", desc = "")
    @ConfigVersionDeprecatedDisplay
    @Transient
    var deprecatedVersionWarning: Unit? = null

    @Suppress("unused")
    @ConfigOption(name = "Current Version", desc = "This is the SkyHanni version you are currently running")
    @ConfigVersionDisplay
    @SearchTag("check download update")
    @Transient
    var currentVersion: Unit? = null

    @ConfigOption(name = "Update Stream", desc = "How frequently you want updates for SkyHanni")
    @Expose
    @ConfigEditorDropdown
    val updateStream: Property<UpdateStream> = Property.of(UpdateStream.RELEASES)

    @Suppress("unused")
    @ConfigOption(name = "Used Software", desc = "Information about used software and licenses")
    @Accordion
    @Expose
    val licenses: Licenses = Licenses()

    enum class UpdateStream(private val label: String, val stream: String) {
        BETA("Beta", "pre"),
        RELEASES("Full", "full");

        override fun toString() = label
    }

    class Licenses {
        @ConfigOption(name = "MoulConfig", desc = "MoulConfig is available under the LGPL 3.0 License or later version")
        @ConfigEditorButton(buttonText = "Source")
        val moulConfig: Runnable = Runnable { openBrowser("https://github.com/NotEnoughUpdates/MoulConfig") }

        @ConfigOption(name = "NotEnoughUpdates-REPO", desc = "NotEnoughUpdates-REPO is available under the MIT License")
        @ConfigEditorButton(buttonText = "Source")
        val notEnoughUpdatesRepo: Runnable = Runnable { openBrowser("https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO") }

        @ConfigOption(name = "Fabric Loader", desc = "Fabric Loader is available under the Apache-2.0 license")
        @ConfigEditorButton(buttonText = "Source")
        val fabricLoader: Runnable = Runnable { openBrowser("https://github.com/FabricMC/fabric-loader") }

        @ConfigOption(name = "Fabric API", desc = "Fabric API is available under the Apache-2.0 license")
        @ConfigEditorButton(buttonText = "Source")
        val fabricApi: Runnable = Runnable { openBrowser("https://github.com/FabricMC/fabric-api") }

        @ConfigOption(name = "LibAutoUpdate", desc = "LibAutoUpdate is available under the BSD 2 Clause License")
        @ConfigEditorButton(buttonText = "Source")
        val libAutoUpdate: Runnable = Runnable { openBrowser("https://github.com/nea89o/libautoupdate") }

        @ConfigOption(name = "Mixin", desc = "Mixin is available under the MIT License")
        @ConfigEditorButton(buttonText = "Source")
        val mixin: Runnable = Runnable { openBrowser("https://github.com/FabricMC/Mixin") }

        @ConfigOption(name = "MixinExtras", desc = "MixinExtras is available under the MIT License")
        @ConfigEditorButton(buttonText = "Source")
        val mixinExtras: Runnable = Runnable { openBrowser("https://github.com/LlamaLad7/MixinExtras") }
    }
}
