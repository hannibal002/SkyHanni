package at.hannibal2.skyhanni.utils.repopatterns

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.SkyHanniScreenTheme
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.searchableList
import at.hannibal2.skyhanni.SkyHanniMod

@SkyHanniModule
object RepoPatternGui {

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shrepopatterns") {
            description = "See where regexes are loaded from"
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback {
                val patterns = RepoPatternManager.allPatterns.sortedBy { it.key }
                SkyHanniMod.screenToOpen = RepoPatternScreen(patterns)
            }
        }
    }

    fun buildContent(screen: RepoPatternScreen): Renderable {
        val items = screen.allPatterns.map { pattern ->
            pattern.key to buildPatternRow(pattern)
        }
        return Renderable.searchableList(
            items = items,
            height = 280,
            scrollValue = screen.scrollValue,
            searchInput = screen.searchInput,
            isSearchActive = { screen.isSearchActive },
            onSearchActivate = {
                screen.isSearchActive = true
                screen.rebuildDisplay()
            },
        )
    }

    private fun buildPatternRow(pattern: CommonPatternInfo<*, *>): Renderable {
        val remotePatterns = when (pattern) {
            is RepoPatternList -> pattern.value.map { it.pattern() }
            is RepoPattern -> listOf(pattern.value.pattern())
        }
        val localPatterns = when (pattern) {
            is RepoPatternList -> pattern.defaultPattern
            is RepoPattern -> listOf(pattern.defaultPattern)
        }
        val statusColor = when {
            pattern.wasOverridden -> "§9"
            pattern.isLoadedRemotely -> "§a"
            else -> "§c"
        }
        val statusLabel = when {
            pattern.wasOverridden -> "Overridden"
            pattern.isLoadedRemotely -> "Remote"
            else -> "Local"
        }
        val regexDisplay = remotePatterns.firstOrNull() ?: localPatterns.firstOrNull().orEmpty()

        val row = Renderable.horizontal(
            listOf(
                Renderable.fixedSizeLine(Renderable.text(pattern.key), width = 220),
                Renderable.fixedSizeLine(Renderable.text(regexDisplay), width = 180),
                Renderable.text("$statusColor$statusLabel"),
            ),
            spacing = 8,
        )

        val hoveredRow = Renderable.drawInsideRoundedRect(row, SkyHanniScreenTheme.COLOR_ROW_HOVER, padding = 2, radius = 3)

        val tooltip = buildTooltip(pattern, remotePatterns, localPatterns)
        return Renderable.hoverable(
            hoveredRow,
            row,
            bypassChecks = true,
            onHover = { RenderableTooltips.setTooltipForRender(tooltip) },
        )
    }

    /**
     * Builds a tooltip Renderable list showing a diff between remote and local patterns.
     *
     * @param pattern The pattern info whose state is being described.
     * @param remotePatterns The compiled remote regex strings for this pattern.
     * @param localPatterns The local fallback regex strings for this pattern.
     */
    private fun buildTooltip(
        pattern: CommonPatternInfo<*, *>,
        remotePatterns: List<String>,
        localPatterns: List<String>,
    ): List<Renderable> = buildList {
        if (!pattern.isLoadedRemotely) {
            add(Renderable.text("§cLocal only"))
            localPatterns.forEach { add(Renderable.text("  §7Local: §f$it")) }
            return@buildList
        }

        add(Renderable.text("§aLoaded remotely"))
        val maxSize = maxOf(remotePatterns.size, localPatterns.size)
        for (i in 0 until maxSize) {
            val remote = remotePatterns.getOrNull(i)
            val local = localPatterns.getOrNull(i)
            when {
                remote != null && local != null -> {
                    if (remote == local) {
                        add(Renderable.text("  §7Remote: §f$remote"))
                        add(Renderable.text("  §7Local: §a(matches remote)"))
                    } else {
                        add(Renderable.text("  §7Remote: §f$remote"))
                        add(Renderable.text("  §7Local: §e$local"))
                    }
                }
                remote != null -> add(Renderable.text("  §7Remote: §f$remote §c(no local fallback)"))
                local != null -> add(Renderable.text("  §7Local: §f$local §c(no remote value)"))
            }
        }
    }
}
