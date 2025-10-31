package at.hannibal2.hanni.api.enoughupdates

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.config.features.dev.NeuRepositoryConfig
import at.hannibal2.hanni.data.repo.AbstractRepoManager
import at.hannibal2.hanni.data.repo.ChatProgressUpdates
import at.hannibal2.hanni.events.NeuRepositoryReloadEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.system.PlatformUtils

@HanniModule
object EnoughUpdatesRepoManager : AbstractRepoManager<NeuRepositoryReloadEvent>() {
    override val commonName = "NotEnoughUpdates"
    override val commonShortNameCased = "NEU"
    override val configDirectory = EnoughUpdatesManager.configDirectory
    override val config get(): NeuRepositoryConfig = HanniMod.feature.dev.neuRepo
    override val backupRepoResourcePath: String get() = when (PlatformUtils.isNeuLoaded()) {
        true -> "assets/notenoughupdates/repo.zip"
        else -> "assets/hanni/neu-repo.zip"
    }

    override val reloadCommand: String = "neureloadrepo"
    override val statusCommand: String = "neurepostatus"
    override val updateCommand: String = "neuresetrepo"

    override val shouldRegisterReloadCommand: Boolean = !PlatformUtils.isNeuLoaded()
    override val shouldRegisterUpdateCommand: Boolean = !PlatformUtils.isNeuLoaded()

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) = super.registerCommands(event)

    override fun reportExtraStatusInfo() = EnoughUpdatesManager.reportItemStatus()
    override suspend fun extraReloadCoroutineWork(progress: ChatProgressUpdates) = EnoughUpdatesManager.reloadItemsFromRepo(progress)
}
