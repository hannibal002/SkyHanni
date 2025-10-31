package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.CancellableHanniEvent
import at.hannibal2.hanni.features.misc.compacttablist.TabLine

data class SkipTabListLineEvent(
    val line: TabLine,
    val lastSubTitle: TabLine?,
    val lastTitle: TabLine?,
) : CancellableHanniEvent()
