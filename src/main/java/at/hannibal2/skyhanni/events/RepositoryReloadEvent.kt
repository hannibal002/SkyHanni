package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.repo.AbstractRepoManager
import at.hannibal2.skyhanni.data.repo.AbstractRepoReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onRepositoryReload")
class RepositoryReloadEvent(
    override val manager: AbstractRepoManager<RepositoryReloadEvent>,
) : AbstractRepoReloadEvent(manager)
