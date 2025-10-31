
package at.hannibal2.hanni.events

import at.hannibal2.hanni.data.repo.AbstractRepoManager
import at.hannibal2.hanni.data.repo.AbstractRepoReloadEvent

class NeuRepositoryReloadEvent(
    override val manager: AbstractRepoManager<NeuRepositoryReloadEvent>
) : AbstractRepoReloadEvent(manager)
