package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.events.TabWidgetUpdate
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

enum class TabWidget(pattern0: String) {

    ;

    val pattern by RepoPattern.pattern("tab.widget.$name", pattern0)

    fun postEvent(lines: List<String>) = TabWidgetUpdate(this, lines).postAndCatch()

    fun isEventForThis(event: TabWidgetUpdate) = event.widget == this

    companion object {
        init {
            entries.forEach { it.pattern }

        }
    }
}
