package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesRepo
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class NeuRepositoryConfig {

    @Expose
    @ConfigOption(
        name = "NEU Repo Auto Update",
        desc = "Update the NEU repository on every startup.\n" +
            "§cOnly disable this if you know what you are doing!\n " +
            "§eThis only works if NEU is not installed, if it is use their settings.",
    )
    @ConfigEditorBoolean
    var repoAutoUpdate: Boolean = true

    @ConfigOption(name = "Update NEU Repo Now", desc = "Update your NEU repository to the latest version")
    @ConfigEditorButton(buttonText = "Update")
    val updateRepo: Runnable = Runnable(EnoughUpdatesRepo::downloadRepo)

    @Expose
    @ConfigOption(name = "NEU Repository Location", desc = "")
    @Accordion
    val location: RepositoryLocation = RepositoryLocation()

    class RepositoryLocation {
        @ConfigOption(name = "Reset Repository Location", desc = "Reset your NEU repository location to the default.")
        @ConfigEditorButton(buttonText = "Reset")
        val resetRepoLocation: Runnable = Runnable { EnoughUpdatesRepo.resetRepoLocation() }

        @Expose
        @ConfigOption(name = "Repository User", desc = "The Repository Branch, default: NotEnoughUpdates")
        @ConfigEditorText
        var user: String = "NotEnoughUpdates"

        @Expose
        @ConfigOption(name = "Repository Name", desc = "The Repository Name, default: NotEnoughUpdates-REPO")
        @ConfigEditorText
        var name: String = "NotEnoughUpdates-REPO"

        @Expose
        @ConfigOption(name = "Repository Branch", desc = "The Repository Branch, default: master")
        @ConfigEditorText
        var branch: String = "master"
    }
}
