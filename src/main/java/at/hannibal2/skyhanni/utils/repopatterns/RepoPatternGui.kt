package at.hannibal2.skyhanni.utils.repopatterns

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import at.hannibal2.skyhanni.utils.XmlUtils
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.observer.ObservableList
import io.github.notenoughupdates.moulconfig.xml.Bind
import java.util.regex.Pattern

/**
 * Gui for analyzing [RepoValue]s (including Regex Patterns and Strings)
 */
class RepoPatternGui private constructor() {

    @SkyHanniModule
    companion object {

        @HandleEvent
        private fun onCommandRegistration(event: CommandRegistrationEvent) {

            /**
             * Open the [RepoPatternGui]
             */
            event.registerBrigadier("shrepopatterns") {
                description = "See where regexes and strings are loaded from"
                category = CommandCategory.DEVELOPER_TEST
                simpleCallback {
                    val location = MyResourceLocation("skyhanni", "gui/regexes.xml")
                    XmlUtils.openXmlScreen(RepoPatternGui(), location)
                }
            }
        }
    }

    @field:Bind
    var search: String = ""
    private var lastSearch = null as String?

    // Changed from RepoPatternManager.allPatterns to RepoPatternManager.allValues
    private val allKeys = RepoPatternManager.allValues
        .sortedBy { it.key }
        .map { RepoPatternInfo(it) }

    private var searchCache = ObservableList(mutableListOf<RepoPatternInfo>())

    class RepoPatternInfo(
        repoValueImpl: RepoValue<*, *>,
    ) {

        @field:Bind
        val key: StructuredText = repoValueImpl.key.asStructuredText()

        // Extract value smartly regardless of if it's a List<Pattern>, Pattern, or String
        val remoteData: List<String> = when (val v = repoValueImpl.value) {
            is List<*> -> v.map { (it as? Pattern)?.pattern() ?: it.toString() }
            is Pattern -> listOf(v.pattern())
            else -> listOf(v.toString())
        }

        @field:Bind
        val regex: StructuredText = remoteData.joinToString("\n").asStructuredText()

        @field:Bind
        val hoverRegex: List<String> = run {
            // Fetch local fallbacks safely from the new base classes
            val localPatterns: List<String> = when (repoValueImpl) {
                is BaseSingleRepoValue<*> -> listOf(repoValueImpl.defaultRaw)
                is BaseListRepoValue<*> -> repoValueImpl.defaultRaw
                else -> emptyList()
            }

            if (repoValueImpl.isLoadedRemotely) {
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
        val overriden: StructuredText = (
            if (repoValueImpl.wasOverridden) "§9Overriden"
            else if (repoValueImpl.isLoadedRemotely) "§aRemote"
            else "§cLocal"
            ).asStructuredText()
    }

    @Bind
    fun poll(): StructuredText {
        if (search != lastSearch) {
            searchCache.clear()
            searchCache.addAll(allKeys.filter { search in it.key.text })
            lastSearch = search
        }
        return "".asStructuredText()
    }

    @Bind
    fun searchResults(): ObservableList<RepoPatternInfo> {
        return searchCache
    }
}
