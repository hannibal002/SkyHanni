
package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.Thread
import at.hannibal2.skyhanni.data.repo.AbstractRepoManager
import at.hannibal2.skyhanni.data.repo.AbstractRepoReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@Thread(DISPATCHER)
@PrimaryFunction("onNeuRepoReload")
class NeuRepositoryReloadEvent(
    override val manager: AbstractRepoManager<NeuRepositoryReloadEvent>
) : AbstractRepoReloadEvent(manager)
