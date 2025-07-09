package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.config.features.dev.RepositoryConfig
import at.hannibal2.skyhanni.data.repo.AbstractRepoManager
import at.hannibal2.skyhanni.data.repo.AbstractRepoReloadEvent

class RepositoryReloadEvent(
    override val manager: AbstractRepoManager<RepositoryConfig, RepositoryReloadEvent>
) : AbstractRepoReloadEvent(manager)
