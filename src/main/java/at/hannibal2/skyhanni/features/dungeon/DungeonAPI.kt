package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.DungeonBossRoomEnterEvent
import at.hannibal2.skyhanni.events.DungeonEnterEvent
import at.hannibal2.skyhanni.events.DungeonStartEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addOrPut
import at.hannibal2.skyhanni.utils.LorenzUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.LorenzUtils.getOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonAPI {
    private val floorPattern = " §7⏣ §cThe Catacombs §7\\((?<floor>.*)\\)".toPattern()
    private val uniqueClassBonus =
        "^Your ([A-Za-z]+) stats are doubled because you are the only player using this class!$".toRegex()

    private val bossPattern =
        "View all your (?<name>\\w+) Collection".toPattern()
    private val levelPattern =
        " +(?<kills>\\d+).*".toPattern()
    private val killPattern = " +☠ Defeated (?<boss>\\w+).*".toPattern()
    private val totalKillsPattern = "§7Total Kills: §e(?<kills>.*)".toPattern()

    companion object {
        var dungeonFloor: String? = null
        var started = false
        var inBossRoom = false
        var playerClass: DungeonClass? = null
        var playerClassLevel = -1
        var isUniqueClass = false

        val bossStorage: MutableMap<DungeonFloor, Int>? get() = ProfileStorageData.profileSpecific?.dungeons?.bosses
        private val timePattern =
            "Time Elapsed:( )?(?:(?<minutes>\\d+)m)? (?<seconds>\\d+)s".toPattern() // Examples: Time Elapsed: 10m 10s, Time Elapsed: 2s

        fun inDungeon() = dungeonFloor != null

        fun isOneOf(vararg floors: String) = dungeonFloor?.equalsOneOf(floors) == true

        fun handleBossMessage(rawMessage: String) {
            if (!inDungeon()) return
            val message = rawMessage.removeColor()
            val bossName = message.substringAfter("[BOSS] ").substringBefore(":").trim()
            if (bossName != "The Watcher" && dungeonFloor != null && checkBossName(dungeonFloor!!, bossName) &&
                !inBossRoom
            ) {
                DungeonBossRoomEnterEvent().postAndCatch()
                inBossRoom = true
            }
        }

        private fun checkBossName(floor: String, bossName: String): Boolean {
            val correctBoss = when (floor) {
                "E" -> "The Watcher"
                "F1", "M1" -> "Bonzo"
                "F2", "M2" -> "Scarf"
                "F3", "M3" -> "The Professor"
                "F4", "M4" -> "Thorn"
                "F5", "M5" -> "Livid"
                "F6", "M6" -> "Sadan"
                "F7", "M7" -> "Maxor"
                else -> null
            } ?: return false

            // Livid has a prefix in front of the name, so we check ends with to cover all the livids
            return bossName.endsWith(correctBoss)
        }

        fun getTime(): String {
            loop@ for (line in ScoreboardData.sidebarLinesFormatted) {
                timePattern.matchMatcher(line.removeColor()) {
                    if (!matches()) continue@loop
                    return "${group("minutes") ?: "00"}:${group("seconds")}" // 03:14
                }
            }
            return ""
        }

        fun getCurrentBoss(): DungeonFloor? {
            val floor = dungeonFloor ?: return null
            return DungeonFloor.valueOf(floor.replace("M", "F"))
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (dungeonFloor == null) {
            for (line in ScoreboardData.sidebarLinesFormatted) {
                floorPattern.matchMatcher(line) {
                    val floor = group("floor")
                    dungeonFloor = floor
                    DungeonEnterEvent(floor).postAndCatch()
                }
            }
        }
        if (dungeonFloor != null && playerClass == null) {
            val playerTeam =
                TabListData.getTabList().firstOrNull {
                    it.contains(LorenzUtils.getPlayerName())
                }?.removeColor() ?: ""

            DungeonClass.entries.forEach {
                if (playerTeam.contains("(${it.scoreboardName} ")) {
                    val level = playerTeam.split(" ").last().trimEnd(')').romanToDecimalIfNeeded()
                    playerClass = it
                    playerClassLevel = level
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        dungeonFloor = null
        started = false
        inBossRoom = false
        isUniqueClass = false
        playerClass = null
        playerClassLevel = -1
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        val floor = dungeonFloor
        if (floor != null) {
            if (event.message == "§e[NPC] §bMort§f: §rHere, I found this map when I first entered the dungeon.") {
                started = true
                DungeonStartEvent(floor).postAndCatch()
            }
            if (event.message.removeColor().matches(uniqueClassBonus)) {
                isUniqueClass = true
            }
        }
    }

    // This returns a map of boss name to the integer for the amount of kills the user has in the collection
    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        val bossCollections = bossStorage ?: return

        if (event.inventoryName == "Boss Collections") {
            readallCollections(bossCollections, event.inventoryItems)
        } else if (event.inventoryName.endsWith(" Collection")) {
            readOneMaxCollection(bossCollections, event.inventoryItems, event.inventoryName)
        }
    }

    private fun readOneMaxCollection(
        bossCollections: MutableMap<DungeonFloor, Int>,
        inventoryItems: Map<Int, ItemStack>,
        inventoryName: String
    ) {
        inventoryItems[48]?.let { item ->
            if (item.name == "§aGo Back") {
                item.getLore().getOrNull(0)?.let { firstLine ->
                    if (firstLine == "§7To Boss Collections") {
                        val name = inventoryName.split(" ").dropLast(1).joinToString(" ")
                        val floor = DungeonFloor.byBossName(name) ?: return
                        val lore = inventoryItems[4]?.getLore() ?: return
                        val line = lore.find { it.contains("Total Kills:") } ?: return
                        val kills = totalKillsPattern.matchMatcher(line) {
                            group("kills").formatNumber().toInt()
                        } ?: return
                        bossCollections[floor] = kills
                    }
                }
            }
        }
    }

    private fun readallCollections(
        bossCollections: MutableMap<DungeonFloor, Int>,
        inventoryItems: Map<Int, ItemStack>,
    ) {
        nextItem@ for (stack in inventoryItems.values) {
            var name = ""
            var kills = 0
            nextLine@ for (line in stack.getLore()) {
                val colorlessLine = line.removeColor()
                bossPattern.matchMatcher(colorlessLine) {
                    if (matches()) {
                        name = group("name")
                    }
                }
                levelPattern.matchMatcher(colorlessLine) {
                    if (matches()) {
                        kills = group("kills").toInt()
                        break@nextLine
                    }
                }
            }
            val floor = DungeonFloor.byBossName(name) ?: continue
            bossCollections[floor] = kills
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inDungeons) return
        killPattern.matchMatcher(event.message.removeColor()) {
            val bossCollections = bossStorage ?: return
            val boss = DungeonFloor.byBossName(group("boss"))
            if (matches() && boss != null && boss !in bossCollections) {
                bossCollections.addOrPut(boss, 1)
            }
        }
    }

    enum class DungeonFloor(private val bossName: String) {
        ENTRANCE("The Watcher"),
        F1("Bonzo"),
        F2("Scarf"),
        F3("The Professor"),
        F4("Thorn"),
        F5("Livid"),
        F6("Sadan"),
        F7("Necron");

        companion object {
            fun byBossName(bossName: String) = DungeonFloor.entries.firstOrNull { it.bossName == bossName }
        }
    }

    enum class DungeonClass(val scoreboardName: String) {
        ARCHER("Archer"),
        BERSERK("Berserk"),
        HEALER("Healer"),
        MAGE("Mage"),
        TANK("Tank")
    }
}
