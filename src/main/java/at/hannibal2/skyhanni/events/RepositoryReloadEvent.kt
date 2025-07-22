package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.repo.AbstractRepoManager
import at.hannibal2.skyhanni.data.repo.AbstractRepoReloadEvent

class RepositoryReloadEvent(
    override val manager: AbstractRepoManager<RepositoryReloadEvent>
) : AbstractRepoReloadEvent(manager)
