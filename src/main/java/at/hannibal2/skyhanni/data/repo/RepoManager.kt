package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object RepoManager : AbstractRepoManager(
    eventConstructor = { RepositoryReloadEvent(it) },
) {
    override val commonName = "SkyHanni"
    override val commonShortNameCased = "SH"
    override val configDirectory = ConfigManager.configDirectory
    override val config get() = SkyHanniMod.feature.dev.repo

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) = super.registerCommands(event)
}
