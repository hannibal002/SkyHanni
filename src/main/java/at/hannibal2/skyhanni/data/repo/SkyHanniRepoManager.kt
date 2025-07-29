package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object SkyHanniRepoManager : AbstractRepoManager<RepositoryReloadEvent>() {
    override val commonName = "SkyHanni"
    override val commonShortNameCased = "SH"
    override val configDirectory = ConfigManager.configDirectory
    override val config get() = SkyHanniMod.feature.dev.repo
    override val backupRepoResourcePath: String = "assets/skyhanni/repo.zip"

    override val reloadCommand: String = "shreloadlocalrepo"
    override val statusCommand: String = "shrepostatus"
    override val updateCommand: String = "shupdaterepo"

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) = super.registerCommands(event)
}
