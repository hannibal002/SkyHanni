package at.hannibal2.hanni.events

import at.hannibal2.hanni.data.repo.AbstractRepoManager
import at.hannibal2.hanni.data.repo.AbstractRepoReloadEvent

class RepositoryReloadEvent(
    override val manager: AbstractRepoManager<RepositoryReloadEvent>
) : AbstractRepoReloadEvent(manager)
