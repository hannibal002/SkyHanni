package at.hannibal2.skyhanni.utils.repopatterns

import at.hannibal2.skyhanni.SkyHanniMod
import io.github.moulberry.moulconfig.common.MyResourceLocation
import io.github.moulberry.moulconfig.gui.GuiContext
import io.github.moulberry.moulconfig.gui.GuiScreenElementWrapperNew
import io.github.moulberry.moulconfig.observer.ObservableList
import io.github.moulberry.moulconfig.xml.Bind
import io.github.moulberry.moulconfig.xml.XMLUniverse

/**
 * Gui for analyzing [RepoPattern]s
 */
class RepoPatternGui private constructor() {
    companion object {
        /**
         * Open the [RepoPatternGui]
         */
        fun open() {
            SkyHanniMod.screenToOpen = GuiScreenElementWrapperNew(
                GuiContext(
                    XMLUniverse.getDefaultUniverse()
                        .load(RepoPatternGui(), MyResourceLocation("skyhanni", "gui/regexes.xml"))
                )
            )
        }
    }

    @field:Bind
    var search: String = ""
    private var lastSearch = null as String?
    private val allKeys = RepoPatternManager.allPatterns.toList()
        .sortedBy { it.key }
        .map { RepoPatternInfo(it) }
    private var searchCache = ObservableList(mutableListOf<RepoPatternInfo>())


    class RepoPatternInfo(
        repoPatternImpl: RepoPatternImpl
    ) {
        @field:Bind
        val key: String = repoPatternImpl.key

        @field:Bind
        val regex: String = repoPatternImpl.value.pattern()

        @field:Bind
        val hoverRegex: List<String> = if (repoPatternImpl.isLoadedRemotely) {
            listOf(
                "§aLoaded remotely",
                "§7Remote: " + repoPatternImpl.compiledPattern.pattern(),
                "§7Local: " + repoPatternImpl.defaultPattern,
            )
        } else {
            listOf("§cLoaded locally", "§7Local: " + repoPatternImpl.defaultPattern)
        }

        @field:Bind
        val keyW = listOf(key)

        @field:Bind
        val overriden: String =
            if (repoPatternImpl.wasOverridden) "§9Overriden"
            else if (repoPatternImpl.isLoadedRemotely) "§aRemote"
            else "§cLocal"
    }

    @Bind
    fun poll(): String {
        if (search != lastSearch) {
            searchCache.clear()
            searchCache.addAll(allKeys.filter { search in it.key })
            lastSearch = search
        }
        return ""
    }

    @Bind
    fun searchResults(): ObservableList<RepoPatternInfo> {
        return searchCache
    }
}
