package at.hannibal2.skyhanni.api.enoughupdates

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.dev.NeuRepositoryConfig
import at.hannibal2.skyhanni.data.repo.AbstractRepoManager
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.system.PlatformUtils

@SkyHanniModule
object EnoughUpdatesRepoManager : AbstractRepoManager<NeuRepositoryReloadEvent>() {
    override val commonName = "NotEnoughUpdates"
    override val commonShortNameCased = "NEU"
    override val configDirectory = EnoughUpdatesManager.configDirectory
    override val config get(): NeuRepositoryConfig = SkyHanniMod.feature.dev.neuRepo
    override val backupRepoResourcePath: String = "assets/notenoughupdates/repo.zip"

    override val reloadCommand: String = "neureloadrepo"
    override val statusCommand: String = "neurepostatus"
    override val updateCommand: String = "neuresetrepo"

    override val shouldRegisterReloadCommand: Boolean = !PlatformUtils.isNeuLoaded()
    override val shouldRegisterUpdateCommand: Boolean = !PlatformUtils.isNeuLoaded()

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) = super.registerCommands(event)

    override fun reportExtraStatusInfo() = EnoughUpdatesManager.reportItemStatus()
    override suspend fun extraReloadCoroutineWork() = EnoughUpdatesManager.reloadItemsFromRepo()
}
