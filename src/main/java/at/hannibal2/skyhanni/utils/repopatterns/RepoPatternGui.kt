package at.hannibal2.skyhanni.utils.repopatterns

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.gui.GuiComponentWrapper
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.observer.ObservableList
import io.github.notenoughupdates.moulconfig.xml.Bind
import io.github.notenoughupdates.moulconfig.xml.XMLUniverse

/**
 * Gui for analyzing [RepoPattern]s
 */
class RepoPatternGui private constructor() {

    @SkyHanniModule
    companion object {

        /**
         * Open the [RepoPatternGui]
         */
        fun open() {
            SkyHanniMod.screenToOpen = GuiComponentWrapper(
                GuiContext(
                    XMLUniverse.getDefaultUniverse()
                        .load(RepoPatternGui(), MyResourceLocation("skyhanni", "gui/regexes.xml"))
                )
            )
        }

        @HandleEvent
        fun onCommandRegistration(event: CommandRegistrationEvent) {
            event.registerBrigadier("shrepopatterns") {
                description = "See where regexes are loaded from"
                category = CommandCategory.DEVELOPER_TEST
                simpleCallback { open() }
            }
        }
    }

    @field:Bind
    var search: String = ""
    private var lastSearch = null as String?
    private val allKeys = RepoPatternManager.allPatterns
        .sortedBy { it.key }
        .map { RepoPatternInfo(it) }
    private var searchCache = ObservableList(mutableListOf<RepoPatternInfo>())

    class RepoPatternInfo(
        repoPatternImpl: CommonPatternInfo<*, *>,
    ) {

        @field:Bind
        val key: String = repoPatternImpl.key

        val remoteData = when (repoPatternImpl) {
            is RepoPatternList -> repoPatternImpl.value.map { it.pattern() }
            is RepoPattern -> listOf(repoPatternImpl.value.pattern())
        }

        @field:Bind
        val regex: String = remoteData.joinToString("\n")

        @field:Bind
        val hoverRegex: List<String> = run {
            val localPatterns = when (repoPatternImpl) {
                is RepoPatternList -> repoPatternImpl.defaultPattern
                is RepoPattern -> listOf(repoPatternImpl.defaultPattern)
            }
            if (repoPatternImpl.isLoadedRemotely) {
                listOf(
                    "§aLoaded remotely",
                    "§7Remote:",
                ) + remoteData.map { " §f- $it" } + listOf(
                    "§7Local:",
                ) + localPatterns.map { " §f- $it" }
            } else {
                listOf("§cLoaded locally", "§7Local:") + localPatterns.map { " §f- $it" }
            }
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
