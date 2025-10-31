package at.hannibal2.hanni.data.repo

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigManager
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.events.RepositoryReloadEvent
import at.hannibal2.hanni.hannimodule.HanniModule

@HanniModule
object HanniRepoManager : AbstractRepoManager<RepositoryReloadEvent>() {
    override val commonName = "Hanni"
    override val commonShortNameCased = "SH"
    override val configDirectory = ConfigManager.configDirectory
    override val config get() = HanniMod.feature.dev.repo
    override val backupRepoResourcePath: String = "assets/hanni/repo.zip"

    override val reloadCommand: String = "shreloadlocalrepo"
    override val statusCommand: String = "shrepostatus"
    override val updateCommand: String = "shupdaterepo"

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) = super.registerCommands(event)
}
