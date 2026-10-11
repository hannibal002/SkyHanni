package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.repo.AbstractRepoConfig
import at.hannibal2.skyhanni.data.repo.AbstractRepoLocationConfig
import at.hannibal2.skyhanni.data.repo.SkyHanniRepoManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class RepositoryConfig : AbstractRepoConfig {
    @Expose
    @ConfigOption(
        name = "Repo Auto Update",
        desc = "Update the repository on every startup and try to auto fix bugs.\n" +
            "§cOnly disable this if you know what you are doing!",
    )
    @ConfigEditorBoolean
    override var repoAutoUpdate: Boolean = true

    @ConfigOption(name = "Update Repo Now", desc = "Update your repository to the latest version")
    @ConfigEditorButton(buttonText = "Update")
    override val updateRepo: Runnable = Runnable { SkyHanniRepoManager.updateRepo("config button") }

    @Expose
    @ConfigOption(name = "Repository Location", desc = "")
    @Accordion
    override val location: RepositoryLocation = RepositoryLocation()

    class RepositoryLocation : AbstractRepoLocationConfig {
        @ConfigOption(name = "Reset Repository Location", desc = "Reset your repository location to the default.")
        @ConfigEditorButton(buttonText = "Reset")
        val resetRepoLocation: Runnable = Runnable { reset() }

        @Expose
        @ConfigOption(name = "Repository User", desc = "The Repository User, default: SkyHanniStudios")
        @ConfigEditorText
        override var user: String = "SkyHanniStudios"

        @Expose
        @ConfigOption(name = "Repository Name", desc = "The Repository Name, default: SkyHanni-REPO")
        @ConfigEditorText
        override var repoName: String = "SkyHanni-REPO"

        @Expose
        @ConfigOption(name = "Repository Branch", desc = "The Repository Branch, default: main")
        @ConfigEditorText
        override var branch: String = "main"

        @Transient
        override val defaultUser = "SkyHanniStudios"

        @Transient
        override val defaultRepoName = "SkyHanni-REPO"

        @Transient
        override val defaultBranch = "main"

        @SkyHanniModule
        companion object {
            @HandleEvent
            private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
                // The repo moved from hannibal002 to SkyHanniStudios. Only that owner is replaced, any other
                // owner stays.
                event.transform(148, "dev.repo.location.user") { element ->
                    val owner = element.takeIf { it.isJsonPrimitive }?.asString
                    if (owner.equals("hannibal002", ignoreCase = true)) JsonPrimitive("SkyHanniStudios") else element
                }
            }
        }
    }

    @Expose
    @ConfigOption(
        name = "Unzip Repo to Memory",
        desc = "Instead of unzipping the repo directly to disk, unzip it to memory first, " +
            "and start a background thread to write it to disk.",
    )
    @ConfigEditorBoolean
    override var unzipToMemory: Boolean = false

}
