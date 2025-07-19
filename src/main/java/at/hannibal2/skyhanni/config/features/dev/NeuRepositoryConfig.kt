package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesRepoManager
import at.hannibal2.skyhanni.data.repo.AbstractRepoConfig
import at.hannibal2.skyhanni.data.repo.AbstractRepoLocationConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class NeuRepositoryConfig : AbstractRepoConfig<NeuRepositoryConfig.NeuRepositoryLocation>() {

    @Expose
    @ConfigOption(
        name = "NEU Repo Auto Update",
        desc = "Update the NEU repository on every startup.\n" +
            "§cOnly disable this if you know what you are doing!\n " +
            "§eThis only works if NEU is not installed, if it is use their settings.",
    )
    @ConfigEditorBoolean
    override var repoAutoUpdate: Boolean = true

    @ConfigOption(name = "Update NEU Repo Now", desc = "Update your NEU repository to the latest version")
    @ConfigEditorButton(buttonText = "Update")
    override val updateRepo: Runnable = Runnable(EnoughUpdatesRepoManager::updateRepo)

    @Expose
    @ConfigOption(name = "NEU Repository Location", desc = "")
    @Accordion
    override val location: NeuRepositoryLocation = NeuRepositoryLocation()

    class NeuRepositoryLocation : AbstractRepoLocationConfig() {
        @ConfigOption(name = "Reset Repository Location", desc = "Reset your NEU repository location to the default.")
        @ConfigEditorButton(buttonText = "Reset")
        val resetRepoLocation: Runnable = Runnable { reset() }

        @Expose
        @ConfigOption(name = "Repository User", desc = "The Repository Branch, default: NotEnoughUpdates")
        @ConfigEditorText
        override var user: String = "NotEnoughUpdates"

        @Expose
        @ConfigOption(name = "Repository Name", desc = "The Repository Name, default: NotEnoughUpdates-REPO")
        @ConfigEditorText
        override var repoName: String = "NotEnoughUpdates-REPO"

        @Expose
        @ConfigOption(name = "Repository Branch", desc = "The Repository Branch, default: master")
        @ConfigEditorText
        override var branch: String = "master"

        @Transient override val defaultUser = "NotEnoughUpdates"
        @Transient override val defaultRepoName = "NotEnoughUpdates-REPO"
        @Transient override val defaultBranch = "master"
    }

    @Expose
    @ConfigOption(
        name = "Unzip Repo to Memory",
        desc = "Instead of unzipping the repo directly to disk, unzip it to memory first, " +
            "and start a background thread to write it to disk.",
    )
    @ConfigEditorBoolean
    override var unzipToMemory: Boolean = true

}
