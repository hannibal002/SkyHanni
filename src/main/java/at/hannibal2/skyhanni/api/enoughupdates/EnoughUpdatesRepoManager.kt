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
object EnoughUpdatesRepoManager : AbstractRepoManager() {

    override val commonName = "NotEnoughUpdates"
    override val commonShortNameCased = "NEU"
    override val configDirectory = EnoughUpdatesManager.configDirectory
    override val config get(): NeuRepositoryConfig = SkyHanniMod.feature.dev.neuRepo

    override val shouldRegisterReloadCommand: Boolean = !PlatformUtils.isNeuLoaded()
    override val shouldRegisterUpdateCommand: Boolean = !PlatformUtils.isNeuLoaded()

    @HandleEvent
    override fun onCommandRegistration(event: CommandRegistrationEvent) = super.registerCommands(event)

    override fun fireReloadEvent(
        manager: AbstractRepoManager,
        onError: (Throwable) -> Unit,
    ): Boolean = NeuRepositoryReloadEvent(manager).post(onError)

    override fun reportExtraStatusInfo() = EnoughUpdatesManager.reportItemStatus()
    override fun extraReloadWork() = EnoughUpdatesManager.prepRepoReload()
    override suspend fun extraReloadCoroutineWork() = EnoughUpdatesManager.reloadItemsFromRepo()
}
