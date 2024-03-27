package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.gui.compacttablist.TabLine
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
data class SkipTabListLineEvent(
    val line: TabLine,
    val lastSubTitle: TabLine?,
    val lastTitle: TabLine?,
) : LorenzEvent()
