package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.model.TabWidget

class TabWidgetUpdate(
    val widget: TabWidget,
    val lines: List<String>,
) : LorenzEvent()
