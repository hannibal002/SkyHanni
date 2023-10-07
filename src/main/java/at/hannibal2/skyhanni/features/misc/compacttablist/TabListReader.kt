package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.FriendAPI
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiPlayerTabOverlay
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import at.hannibal2.skyhanni.utils.TabListData
import com.google.common.cache.CacheBuilder
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.TimeUnit
import kotlin.random.Random

// heavily inspired by SBA code
object TabListReader {
    private val config get() = SkyHanniMod.feature.misc.compactTabList
    var hypixelAdvertisingString = "HYPIXEL.NET"
    private val godPotPattern = "You have a God Potion active! (?<timer>[\\w ]+)".toPattern()
    private val activeEffectPattern = "Active Effects(?:§.)*(?:\\n(?:§.)*§7.+)*".toPattern()
    private val effectCountPattern = "You have (?<effectCount>[0-9]+) active effect".toPattern()
    private val cookiePattern = "Cookie Buff(?:§.)*(?:\\n(§.)*§7.+)*".toPattern()
    private val dungeonBuffPattern = "Dungeon Buffs(?:§.)*(?:\\n(§.)*§7.+)*".toPattern()
    private val upgradesPattern = "(?<firstPart>§e[A-Za-z ]+)(?<secondPart> §f[\\w ]+)".toPattern()
    private val tabListSPattern = "(?i)§S".toPattern()
    private var playerDatas = mutableMapOf<String, PlayerData>()

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
        val fullTabList = newSorting(original)

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

    private fun newSorting(original: List<String>): List<String> {
        if (LorenzUtils.inKuudraFight) return original
        if (LorenzUtils.inDungeons) return original

        if (LorenzUtils.isControlKeyDown()) return original

        val pattern = ".*\\[(?<level>.*)] (?<name>.*)".toPattern()
        val newList = mutableListOf<String>()
        val currentData = mutableMapOf<String, PlayerData>()
        newList.add(original.first())

        var extraTitles = 0
        var i = 0

        for (line in original) {
            i++
            if (i == 1) continue
            if (line.isEmpty() || line.contains("Server Info")) break
            if (line.contains("§r§a§lPlayers")) {
                extraTitles++
                continue
            }
            pattern.matchMatcher(line) {
                val levelText = group("level")
                val removeColor = levelText.removeColor()
                try {
                    val playerData = PlayerData(removeColor.toInt())
                    currentData[line] = playerData

                    val fullName = group("name")
                    val name = fullName.split(" ")
                    val coloredName = name[0]
                    playerData.coloredName = coloredName
                    playerData.name = coloredName.removeColor()
                    playerData.levelText = levelText
                    if (name.size > 1) {
                        val nameSuffix = name.drop(1).joinToString(" ")
                        playerData.nameSuffix = nameSuffix
                        if (nameSuffix.contains("♲")) {
                            playerData.ironman = true
                        } else {
                            playerData.bingoLevel = getBingoRank(line)
                        }
                    } else {
                        playerData.nameSuffix = ""
                    }

                } catch (e: NumberFormatException) {
                    val message = "Special user (youtube or admin?): '$line'"
                    LorenzUtils.debug(message)
                    println(message)
                }
            }
        }
        playerDatas = currentData
        val prepare = currentData.entries

        val sorted = when (config.playerSortOrder) {

            // Rank (Default)
            1 -> prepare.sortedBy { -(it.value.sbLevel) }

            // Name (Abc)
            2 -> prepare.sortedBy { it.value.name.lowercase().replace("_", "") }

            // Ironman/Bingo
            3 -> prepare.sortedBy { -if (it.value.ironman) 10 else it.value.bingoLevel }

            // Party/Friends/Guild First
            4 -> prepare.sortedBy { -socialScore(it.value.name) }

            // Random
            5 -> prepare.sortedBy { getRandomOrder(it.value.name) }

            else -> prepare
        }

        var newPlayerList = sorted.map { it.key }.toMutableList()
        if (config.reverseSort) {
            newPlayerList = newPlayerList.reversed().toMutableList()
        }
        if (extraTitles > 0) {
            newPlayerList.add(19, original.first())
        }
        newList.addAll(newPlayerList)

        val rest = original.drop(playerDatas.size + extraTitles + 1)
        newList.addAll(rest)
        return newList
    }

    private fun createCustomName(data: PlayerData): String {
        val playerName = if (config.useLevelColorForName) {
            val c = data.levelText[3]
            "§$c" + data.name
        } else if (config.hideRankColor) "§b" + data.name else data.coloredName

        val level = if (!config.hideLevel) {
            if (config.hideLevelBrackets) data.levelText else "§8[${data.levelText}§8]"
        } else ""

        val suffix = if (config.hideEmblem) {
            if (data.ironman) {
                "§8[§7Iron Man§8]"
            } else {
                if (data.bingoLevel > -1) {
                    "§rBingo ${data.bingoLevel}"
                } else ""
            }
        } else data.nameSuffix

        return "$level $playerName $suffix"
    }

    private var randomOrderCache =
        CacheBuilder.newBuilder().expireAfterWrite(20, TimeUnit.MINUTES).build<String, Int>()

    private fun getRandomOrder(name: String): Int {
        val saved = randomOrderCache.getIfPresent(name)
        if (saved != null) {
            return saved
        }
        val r = (Random.nextDouble() * 500).toInt()
        randomOrderCache.put(name, r)
        return r
    }

    private fun socialScore(name: String) = when {
        LorenzUtils.getPlayerName() == name -> 5
        MarkedPlayerManager.isMarkedPlayer(name) -> 4
        PartyAPI.partyMembers.contains(name) -> 3
        FriendAPI.getAllFriends().any { it.name == name } -> 2
        // TODO add guild

        else -> 1
    }

    private fun getBingoRank(text: String) = when {
        text.contains("§7Ⓑ") -> 0 //No Rank
        text.contains("§aⒷ") -> 1 //Rank 1
        text.contains("§9Ⓑ") -> 2 //Rank 2
        text.contains("§5Ⓑ") -> 3 //Rank 3
        text.contains("§6Ⓑ") -> 4 //Rank 4

        else -> -1
    }

    class PlayerData(val sbLevel: Int) {
        var name: String = "?"
        var coloredName: String = "?"
        var nameSuffix: String = "?"
        var levelText: String = "?"
        var ironman: Boolean = false
        var bingoLevel: Int = -1
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
                            firstColumnCopy.addLine(createTabLine("", TabStringType.TEXT))
                        }
                    }

                    if (needsTitle) {
                        lastTitle = section.columnValue.columnTitle
                        firstColumnCopy.addLine(createTabLine(lastTitle, TabStringType.TITLE))
                        currentCount++
                    }

                    for (line in section.lines) {
                        if (currentCount >= TabListRenderer.maxLines) {
                            renderColumns.add(RenderColumn().also { firstColumnCopy = it })
                            currentCount = 1
                        }

                        firstColumnCopy.addLine(createTabLine(line, TabStringType.fromLine(line)))
                        currentCount++
                    }
                } else {
                    if (currentCount + sectionSize > TabListRenderer.maxLines) {
                        renderColumns.add(RenderColumn().also { firstColumnCopy = it })
                    } else {
                        if (firstColumnCopy.size() > 0) {
                            firstColumnCopy.addLine(createTabLine("", TabStringType.TEXT))
                        }
                    }

                    if (needsTitle) {
                        lastTitle = section.columnValue.columnTitle
                        firstColumnCopy.addLine(createTabLine(lastTitle, TabStringType.TITLE))
                    }

                    for (line in section.lines) {
                        firstColumnCopy.addLine(createTabLine(line, TabStringType.fromLine(line)))
                    }
                }
            }
        }
    }

    private fun createTabLine(text: String, type: TabStringType) = playerDatas[text]?.let {
        TabLine(text, type, createCustomName(it))
    } ?: TabLine(text, type)
}