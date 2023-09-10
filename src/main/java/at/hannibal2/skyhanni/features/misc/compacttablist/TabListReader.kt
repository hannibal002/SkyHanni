package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiPlayerTabOverlay
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TabListData
import com.google.common.collect.Ordering
import net.minecraft.client.Minecraft
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object TabListReader {
    private val playerOrdering = Ordering.from(TabListData.PlayerComparator())

    var hypixelAdvertisingString = "HYPIXEL.NET"
    private val godPotPattern = "You have a God Potion active! (?<timer>[\\w ]+)".toPattern()
    private val activeEffectPattern = "Active Effects(?:§.)*(?:\\n(?:§.)*§7.+)*".toPattern()
    private val effectCountPattern = "You have (?<effectCount>[0-9]+) active effect".toPattern()
    private val cookiePattern = "Cookie Buff(?:§.)*(?:\\n(§.)*§7.+)*".toPattern()
    private val dungeonBuffPattern = "Dungeon Buffs(?:§.)*(?:\\n(§.)*§7.+)*".toPattern()
    private val upgradesPattern = "(?<firstPart>§e[A-Za-z ]+)(?<secondPart> §f[\\w ]+)".toPattern()
    private val tabListSPattern = "(?i)§S".toPattern()

    val renderColumns = mutableListOf<RenderColumn>()
    // heavily inspired by SBA code
    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.isMod(5)) return
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.misc.compactTabList.enabled) return
        val thePlayer = Minecraft.getMinecraft()?.thePlayer ?: return

        if (thePlayer.sendQueue == null) return

        var tabLines = playerOrdering.sortedCopy(thePlayer.sendQueue.playerInfoMap)

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

    private fun parseColumns(fullTabList: List<NetworkPlayerInfo>): MutableList<TabColumn> {
        val columns = mutableListOf<TabColumn>()
        val tabList = Minecraft.getMinecraft().ingameGUI.tabList

        for (entry in fullTabList.indices step 20) {
            val title = StringUtils.trimWhiteSpaceAndResets(tabList.getPlayerName(fullTabList[entry]))
            var column = getColumnFromName(columns, title)

            if (column == null) {
                column = TabColumn(title)
                columns.add(column)
            }

            for (columnEntry in (entry + 1) until fullTabList.size.coerceAtMost(entry + 20)) {
                column.addLine(tabList.getPlayerName(fullTabList[columnEntry]))
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
            footer = activeEffectPattern.matcher(footer).replaceAll("Active Effects:\n§cGod Potion§r: ${matcher.group("timer")}")
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
            matcher = upgradesPattern.matcher(StringUtils.removeResets(newLine))

            if (matcher.matches()) {
                var firstPart = StringUtils.trimWhiteSpaceAndResets(matcher.group("firstPart"))
                if (!firstPart.contains("§l")) {
                    firstPart = " $firstPart"
                }
                column.addLine(firstPart)

                newLine = matcher.group("secondPart")
            }

            newLine = StringUtils.trimWhiteSpaceAndResets(newLine)
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
                if (StringUtils.trimWhiteSpaceAndResets(line).isEmpty()) {
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
                            firstColumnCopy.addLine(TabLine("", TabStringType.TEXT))
                        }
                    }

                    if (needsTitle) {
                        lastTitle = section.columnValue.columnTitle
                        firstColumnCopy.addLine(TabLine(lastTitle, TabStringType.TITLE))
                        currentCount++
                    }

                    for (line in section.lines) {
                        if (currentCount >= TabListRenderer.maxLines) {
                            renderColumns.add(RenderColumn().also { firstColumnCopy = it })
                            currentCount = 1
                        }

                        firstColumnCopy.addLine(TabLine(line, TabStringType.fromLine(line)))
                        currentCount++
                    }
                } else {
                    if (currentCount + sectionSize > TabListRenderer.maxLines) {
                        renderColumns.add(RenderColumn().also { firstColumnCopy = it })
                    } else {
                        if (firstColumnCopy.size() > 0) {
                            firstColumnCopy.addLine(TabLine("", TabStringType.TEXT))
                        }
                    }

                    if (needsTitle) {
                        lastTitle = section.columnValue.columnTitle
                        firstColumnCopy.addLine(TabLine(lastTitle, TabStringType.TITLE))
                    }

                    for (line in section.lines) {
                        firstColumnCopy.addLine(TabLine(line, TabStringType.fromLine(line)))
                    }
                }
            }
        }
    }
}