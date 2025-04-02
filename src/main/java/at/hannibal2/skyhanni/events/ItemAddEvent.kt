package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.data.ItemSources
import at.hannibal2.skyhanni.utils.NeuInternalName

class ItemAddEvent(
    val internalName: NeuInternalName,
    val amount: Int,
    val source: ItemSources.Source,
    val exactSource: ItemSources.ExactSource = ItemSources.ExactSource.NONE,
) : CancellableSkyHanniEvent()
