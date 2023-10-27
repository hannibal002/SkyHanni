package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiPlayerTabOverlay
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

// heavily inspired by SBA code
object TabListReader {
    private val config get() = SkyHanniMod.feature.misc.compactTabList
    // TODO USE SH-REPO
    var hypixelAdvertisingString = "HYPIXEL.NET"
    private val godPotPattern = "You have a God Potion active! (?<timer>[\\w ]+)".toPattern()
    private val activeEffectPattern = "Active Effects(?:§.)*(?:\\n(?:§.)*§7.+)*".toPattern()
    private val effectCountPattern = "You have (?<effectCount>[0-9]+) active effect".toPattern()
    private val cookiePattern = "Cookie Buff(?:§.)*(?:\\n(§.)*§7.+)*".toPattern()
    private val dungeonBuffPattern = "Dungeon Buffs(?:§.)*(?:\\n(§.)*§7.+)*".toPattern()
    private val upgradesPattern = "(?<firstPart>§e[A-Za-z ]+)(?<secondPart> §f[\\w ]+)".toPattern()
    private val tabListSPattern = "(?i)§S".toPattern()

    val renderColumns = mutableListOf<RenderColumn>()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!event.isMod(5)) return
        if (!config.enabled) return

        var tabLines = TabListData.getTabList()

        if (tabLines.size < 80) return

        tabLines = tabLines.subList(0, 80)

        val columns = parseColumns(tabLines)
        val footerColumn = parseFooterAsColumn()

        if (footerColumn != null) {
            columns.add(footerColumn)
        }

        parseSections(columns)

        renderColumns.clear()
        val renderColumn = RenderColumn()
        renderColumns.add(renderColumn)
        combineColumnsToRender(columns, renderColumn)
    }

    private fun parseColumns(original: List<String>): MutableList<TabColumn> {
        val columns = mutableListOf<TabColumn>()
        val fullTabList = AdvancedPlayerList.newSorting(original)

        for (entry in fullTabList.indices step 20) {
            val title = fullTabList[entry].trimWhiteSpaceAndResets()
            var column = getColumnFromName(columns, title)

            if (column == null) {
                column = TabColumn(title)
                columns.add(column)
            }

            for (columnEntry in (entry + 1) until fullTabList.size.coerceAtMost(entry + 20)) {
                column.addLine(fullTabList[columnEntry])
            }
        }
        return columns
    }

    private fun parseFooterAsColumn(): TabColumn? {
        val tabList = Minecraft.getMinecraft().ingameGUI.tabList as AccessorGuiPlayerTabOverlay

        if (tabList.footer_skyhanni == null) {
            return null
        }

        val column = TabColumn("§2§lOther")

        var footer = tabListSPattern.matcher(tabList.footer_skyhanni.formattedText).replaceAll("")

        var matcher = godPotPattern.matcher(tabList.footer_skyhanni.unformattedText)
        if (matcher.find()) {
            footer = activeEffectPattern.matcher(footer)
                .replaceAll("Active Effects:\n§cGod Potion§r: ${matcher.group("timer")}")
        } else {
            matcher = effectCountPattern.matcher(tabList.footer_skyhanni.unformattedText)
            footer = if (matcher.find()) {
                activeEffectPattern.matcher(footer).replaceAll("Active Effects: §r§e" + matcher.group("effectCount"))
            } else {
                activeEffectPattern.matcher(footer).replaceAll("Active Effects: §r§e0")
            }
        }

        matcher = cookiePattern.matcher(footer)
        if (matcher.find() && matcher.group().contains("Not active!")) {
            footer = matcher.replaceAll("Cookie Buff \n§r§7Not Active")
        }

        matcher = dungeonBuffPattern.matcher(footer)
        if (matcher.find() && matcher.group().contains("No Buffs active.")) {
            footer = matcher.replaceAll("Dungeon Buffs \n§r§7None Found")
        }

        for (line in footer.split("\n")) {
            if (line.contains(hypixelAdvertisingString)) continue

            var newLine = line
            matcher = upgradesPattern.matcher(newLine.removeResets())

            if (matcher.matches()) {
                var firstPart = matcher.group("firstPart").trimWhiteSpaceAndResets()
                if (!firstPart.contains("§l")) {
                    firstPart = " $firstPart"
                }
                column.addLine(firstPart)

                newLine = matcher.group("secondPart")
            }

            newLine = newLine.trimWhiteSpaceAndResets()
            if (!newLine.contains("§l")) {
                newLine = " $newLine"
            }
            column.addLine(newLine)
        }

        return column
    }

    private fun getColumnFromName(columns: List<TabColumn>, name: String): TabColumn? {
        for (tabColumn in columns) {
            if (name == tabColumn.columnTitle) {
                return tabColumn
            }
        }
        return null
    }

    private fun parseSections(columns: MutableList<TabColumn>) {
        for (column in columns) {
            var currentTabSection: TabSection? = null
            for (line in column.lines) {
                if (line.trimWhiteSpaceAndResets().isEmpty()) {
                    currentTabSection = null
                    continue
                }

                if (currentTabSection == null) {
                    column.addSection(TabSection(column).also { currentTabSection = it })
                }

                currentTabSection?.addLine(line)
            }
        }
    }

    private fun combineColumnsToRender(columns: MutableList<TabColumn>, firstColumn: RenderColumn) {
        var firstColumnCopy = firstColumn
        var lastTitle: String? = null

        for (column in columns) {
            for (section in column.sections) {
                var sectionSize = section.size()

                var needsTitle = false
                if (lastTitle != section.columnValue.columnTitle) {
                    needsTitle = true
                    sectionSize++
                }

                var currentCount = firstColumnCopy.size()

                if (sectionSize >= TabListRenderer.maxLines / 2) {
                    if (currentCount >= TabListRenderer.maxLines) {
                        renderColumns.add(RenderColumn().also { firstColumnCopy = it })
                        currentCount = 1
                    } else {
                        if (firstColumnCopy.size() > 0) {
                            firstColumnCopy.addLine(AdvancedPlayerList.createTabLine("", TabStringType.TEXT))
                        }
                    }

                    if (needsTitle) {
                        lastTitle = section.columnValue.columnTitle
                        firstColumnCopy.addLine(AdvancedPlayerList.createTabLine(lastTitle, TabStringType.TITLE))
                        currentCount++
                    }

                    for (line in section.lines) {
                        if (currentCount >= TabListRenderer.maxLines) {
                            renderColumns.add(RenderColumn().also { firstColumnCopy = it })
                            currentCount = 1
                        }

                        firstColumnCopy.addLine(AdvancedPlayerList.createTabLine(line, TabStringType.fromLine(line)))
                        currentCount++
                    }
                } else {
                    if (currentCount + sectionSize > TabListRenderer.maxLines) {
                        renderColumns.add(RenderColumn().also { firstColumnCopy = it })
                    } else {
                        if (firstColumnCopy.size() > 0) {
                            firstColumnCopy.addLine(AdvancedPlayerList.createTabLine("", TabStringType.TEXT))
                        }
                    }

                    if (needsTitle) {
                        lastTitle = section.columnValue.columnTitle
                        firstColumnCopy.addLine(AdvancedPlayerList.createTabLine(lastTitle, TabStringType.TITLE))
                    }

                    for (line in section.lines) {
                        firstColumnCopy.addLine(AdvancedPlayerList.createTabLine(line, TabStringType.fromLine(line)))
                    }
                }
            }
        }
    }
}