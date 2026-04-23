package at.hannibal2.skyhanni.features.misc.update

import moe.nea.libautoupdate.PotentialUpdate
import moe.nea.libautoupdate.UpdateAction
import moe.nea.libautoupdate.UpdateTarget

object NoOpUpdateTarget : UpdateTarget {

    override fun generateUpdateActions(update: PotentialUpdate) = emptyList<UpdateAction>()
}
