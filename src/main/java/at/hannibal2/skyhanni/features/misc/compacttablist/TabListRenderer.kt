package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.GlobalRender
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SkipTabListLineEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isActive
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.filterToMutable
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.network.chat.Component

@SkyHanniModule
object TabListRenderer {

    private val config get() = SkyHanniMod.feature.gui.compactTabList

    const val MAX_LINES = 22
    private const val LINE_HEIGHT = 8 + 1
    private const val TAB_PADDING = 3
    private const val COLUMN_SPACING = 6

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlayPre(event: GameOverlayRenderPreEvent) {
        if (GlobalRender.renderDisabled || event.type != RenderLayer.PLAYER_LIST || !config.enabled.get()) return
        event.cancel()

        if (config.toggleTab) return
        drawTabList()
    }

    private var isPressed = false
    private var isTabToggled = false

    @HandleEvent(onlyOnSkyblock = true, priority = HandleEvent.LOWEST)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (GlobalRender.renderDisabled || !config.enabled.get() || !config.toggleTab) return
        if (Minecraft.getInstance().screen != null) return

        val playerListKeyActive = Minecraft.getInstance().options.keyPlayerList.isActive()
        if (playerListKeyActive && !isPressed) {
            isPressed = true
            isTabToggled = !isTabToggled
        } else if (!playerListKeyActive) {
            isPressed = false
        }

        if (isTabToggled) drawTabList()
    }

    private fun drawTabList() {
        val columns = TabListReader.renderColumns

        if (columns.isEmpty()) return

        var maxLines = 0
        var totalWidth = 0 - COLUMN_SPACING

        for (column in columns) {
            maxLines = maxLines.coerceAtLeast(column.size())
            totalWidth += column.getMaxWidth() + COLUMN_SPACING
        }

        var totalHeight = maxLines * LINE_HEIGHT

        val (header, footer) = listOf(
            TabListData.header,
            TabListData.footer,
        ).map { component ->
            if (config.hideAdverts) return@map emptyList()
            val componentHeader: Component = component ?: Component.empty()
            val componentLines = TextHelper.split(componentHeader, "\n") ?: listOf(componentHeader)
            val filteredLines = componentLines.filter { line -> line.string.contains(TabListReader.hypixelAdvertisingString) }
            totalHeight += filteredLines.size * LINE_HEIGHT + TAB_PADDING
            filteredLines.toMutableList()
        }

        val minecraft = Minecraft.getInstance()
        val screenWidth = GuiScreenUtils.scaledWindowWidth / 2
        val x = screenWidth - totalWidth / 2
        val y = 10

        if (!config.hideTabBackground) GuiRenderUtils.drawRect(
            x - COLUMN_SPACING,
            y - TAB_PADDING,
            screenWidth + totalWidth / 2 + COLUMN_SPACING,
            10 + totalHeight + TAB_PADDING,
            -0x80000000,
        )

        var headerY = y
        for (line in header) {
            GuiRenderUtils.drawString(
                line,
                x + totalWidth / 2f - minecraft.font.width(line) / 2f,
                headerY.toFloat(),
                -1,
            )
            headerY += 8 + 1
        }

        drawColumns(x, headerY, columns)

        if (footer.isNotEmpty()) {
            var footerY = y + totalHeight - footer.size * LINE_HEIGHT + TAB_PADDING / 2 + 1
            for (line in footer) {
                GuiRenderUtils.drawString(
                    line,
                    x + totalWidth / 2f - minecraft.font.width(line) / 2f,
                    footerY.toFloat(),
                    -1,
                )
                footerY += LINE_HEIGHT
            }
        }
    }

    private fun drawColumns(x: Int, headerY: Int, columns: List<RenderColumn>) {
        var middleX = x
        var lastTitle: TabLine? = null
        var lastSubTitle: TabLine? = null
        for (originalColumn in columns) {
            var middleY = if (config.hideAdverts) headerY else headerY + TAB_PADDING + 2

            val column = originalColumn.lines.filterToMutable { tabLine ->
                if (tabLine.type == TabStringType.TITLE) {
                    lastSubTitle = null
                    lastTitle = tabLine
                }
                if (tabLine.type == TabStringType.SUB_TITLE) {
                    lastSubTitle = tabLine
                }
                !SkipTabListLineEvent(tabLine, lastSubTitle, lastTitle).post()
            }.let(::RenderColumn)

            GuiRenderUtils.drawRect(
                middleX - TAB_PADDING + 1,
                middleY - TAB_PADDING + 1,
                middleX + column.getMaxWidth() + TAB_PADDING - 2,
                middleY + column.size() * LINE_HEIGHT + TAB_PADDING - 2,
                if (config.hideTabBackground) 0x8F262626.toInt() else 0x20AAAAAA,
            )

            for (tabLine in column.lines) {
                val savedX = middleX

                val hideIcons = config.advancedPlayerList.hidePlayerIcons && !AdvancedPlayerList.ignoreCustomTabList()
                if (tabLine.type == TabStringType.PLAYER && !hideIcons) {
                    val playerInfo = tabLine.getInfo()
                    if (playerInfo != null) {
                        val texture = playerInfo.skin.body().id()
                        PlayerFaceRenderer.draw(
                            DrawContextUtils.drawContext, texture, middleX, middleY, 8, playerInfo.showHat(), false, -1,
                        )
                    }
                    middleX += 8 + 2
                }

                val drawX = middleX + if (tabLine.type == TabStringType.TITLE) column.getMaxWidth() / 2f - tabLine.getWidth() / 2f else 0f
                val drawY = middleY.toFloat()
                if (AdvancedPlayerList.ignoreCustomTabList() || tabLine.customName == null) {
                    GuiRenderUtils.drawString(tabLine.component, drawX, drawY, -1)
                } else {
                    GuiRenderUtils.drawString(tabLine.customName, drawX, drawY, -1)
                }

                middleY += LINE_HEIGHT
                middleX = savedX
            }
            middleX += column.getMaxWidth() + COLUMN_SPACING
        }
    }

    private val fireSalePattern by RepoPattern.pattern(
        "tablist.firesaletitle",
        "§.§lFire Sales: §r§f\\([0-9]+\\)",
    )

    @HandleEvent
    fun onSkipTablistLine(event: SkipTabListLineEvent) {
        if (config.hideFiresales && event.lastSubTitle != null && fireSalePattern.matches(event.lastSubTitle.component)) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(31, "misc.compactTabList", "gui.compactTabList")
    }
}
