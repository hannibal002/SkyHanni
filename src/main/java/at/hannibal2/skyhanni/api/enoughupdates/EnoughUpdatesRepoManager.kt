package at.hannibal2.skyhanni.api.enoughupdates

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.dev.NeuRepositoryConfig
import at.hannibal2.skyhanni.data.repo.AbstractRepoManager
import at.hannibal2.skyhanni.data.repo.ChatProgressUpdates
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import java.io.File

@SkyHanniModule
object EnoughUpdatesRepoManager : AbstractRepoManager<NeuRepositoryReloadEvent>() {
    override val commonName = "NotEnoughUpdates"
    override val commonShortNameCased = "NEU"
    override val config get(): NeuRepositoryConfig = SkyHanniMod.feature.dev.neuRepo
    override val backupRepoResourcePath: String = "assets/skyhanni/neu-repo.tar.gz"
    override val legacyConfigDirectory = File("config/notenoughupdates")

    override val reloadCommand: String = "neureloadrepo"
    override val progressCategory = ChatProgressUpdates.category("NotEnoughUpdates Repo")
    override val statusCommand: String = "neurepostatus"
    override val updateCommand: String = "neuresetrepo"

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) = super.registerCommands(event)

    override fun reportExtraStatusInfo() = with(EnoughUpdatesManager) {
        reportRecipeStatus()
    }
    override suspend fun extraReloadCoroutineWork(progress: ChatProgressUpdates) = EnoughUpdatesManager.reloadItemsFromRepo(progress)
}
