
package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.config.features.dev.NeuRepositoryConfig
import at.hannibal2.skyhanni.data.repo.AbstractRepoManager
import at.hannibal2.skyhanni.data.repo.AbstractRepoReloadEvent

class NeuRepositoryReloadEvent(
    override val manager: AbstractRepoManager<NeuRepositoryConfig, NeuRepositoryReloadEvent>
) : AbstractRepoReloadEvent(manager)
