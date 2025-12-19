package at.hannibal2.skyhanni.utils.repopatterns

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.observer.ObservableList
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import io.github.notenoughupdates.moulconfig.xml.Bind
import io.github.notenoughupdates.moulconfig.xml.XMLUniverse
import net.minecraft.network.chat.Component

/**
 * Gui for analyzing [RepoPattern]s
 */
class RepoPatternGui private constructor() {

    @SkyHanniModule
    companion object {

        @HandleEvent
        fun onCommandRegistration(event: CommandRegistrationEvent) {

            /**
             * Open the [RepoPatternGui]
             */
            event.registerBrigadier("shrepopatterns") {
                description = "See where regexes are loaded from"
                category = CommandCategory.DEVELOPER_TEST
                simpleCallback {
                    val location = MyResourceLocation("skyhanni", "gui/regexes.xml")
                    val universe = XMLUniverse.getDefaultUniverse()
                    val context = GuiContext(universe.load(RepoPatternGui(), location))
                    SkyHanniMod.screenToOpen = MoulConfigScreenComponent(Component.empty(), context, null)
                }
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
        val key: StructuredText = repoPatternImpl.key.asStructuredText()

        val remoteData = when (repoPatternImpl) {
            is RepoPatternList -> repoPatternImpl.value.map { it.pattern() }
            is RepoPattern -> listOf(repoPatternImpl.value.pattern())
        }

        @field:Bind
        val regex: StructuredText = remoteData.joinToString("\n").asStructuredText()

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
        val overriden: StructuredText = (
            if (repoPatternImpl.wasOverridden) "§9Overriden"
            else if (repoPatternImpl.isLoadedRemotely) "§aRemote"
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
