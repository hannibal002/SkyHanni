package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI.DungeonFloor.Companion.toFloor
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonAPI {
    private val floorPattern = " §7⏣ §cThe Catacombs §7\\((?<floor>.*)\\)".toPattern()

    private val bossPattern =
        "View all your (?<name>\\w+) Collection".toPattern()
    private val levelPattern =
        " +(?<kills>\\d+).*".toPattern()
    private val killPattern = " +☠ Defeated (?<boss>\\w+).*".toPattern()
    private val bossList = listOf("Bonzo", "Scarf", "The Professor", "Thorn", "Livid", "Sadan", "Necron")

    private var bossCollections: MutableMap<DungeonFloor, Int> = mutableMapOf()


    companion object {
        var dungeonFloor: String? = null
        var started = false
        var inBossRoom = false
        val bossStorage: MutableMap<DungeonFloor, Int>? get() = ProfileStorageData.profileSpecific?.dungeons?.bosses
        private val areaPattern = "The Catacombs \\((?<floor>.+)\\)".toPattern()
        private val timePattern =
            "Time Elapsed:( )?(?:(?<minutes>\\d+)m)? (?<seconds>\\d+)s".toPattern() // Examples: Time Elapsed: 10m 10s, Time Elapsed: 2s

        fun inDungeon() = dungeonFloor != null

        fun isOneOf(vararg floors: String) = dungeonFloor?.equalsOneOf(floors) == true

        fun handleBossMessage(rawMessage: String) {
            if (!inDungeon()) return
            val message = rawMessage.removeColor()
            val bossName = message.substringAfter("[BOSS] ").substringBefore(":").trim()
            if (bossName != "The Watcher" && dungeonFloor != null && checkBossName(dungeonFloor!!, bossName)) {
                if (!inBossRoom) {
                    DungeonBossRoomEnterEvent().postAndCatch()
                    inBossRoom = true
                }
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

        fun getFloor(): Int? {
            val area = LorenzUtils.skyBlockArea
            areaPattern.matchMatcher(area) {
                if (matches()) return group("floor").last().digitToInt()
            }
            return null
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
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        dungeonFloor = null
        started = false
        inBossRoom = false
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        val floor = dungeonFloor
        if (floor != null) {
            if (event.message == "§e[NPC] §bMort§f: §rHere, I found this map when I first entered the dungeon.") {
                started = true
                DungeonStartEvent(floor).postAndCatch()
            }
        }
    }

    // This returns a map of boss name to the integer for the amount of kills the user has in the collection
    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Boss Collections") return
        nextItem@ for ((_, stack) in event.inventoryItems) {
            var name = ""
            var kills = 0
            nextLine@ for (line in stack.getLore()) {
                val colorlessLine = line.removeColor()
                bossPattern.matchMatcher(colorlessLine) {
                    if (matches()) {
                        name = group("name")
                        if (!bossList.contains(name)) continue@nextItem // to avoid kuudra, etc.
                    }
                }
                levelPattern.matchMatcher(colorlessLine) {
                    if (matches()) {
                        kills = group("kills").toInt()
                        break@nextLine
                    }
                }
            }
            val floor = name.toFloor()
            if (floor != null) bossCollections[floor] = kills
        }
        ProfileStorageData.profileSpecific?.dungeons?.bosses = bossCollections
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inDungeons) return
        killPattern.matchMatcher(event.message.removeColor()) {
            val boss = group("boss").toFloor()
            if (matches() && boss != null && bossCollections[boss] != null) {
                bossCollections[boss] = bossCollections[boss]!! + 1
                ProfileStorageData.profileSpecific?.dungeons?.bosses = bossCollections
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
            fun Int.toBoss(): DungeonFloor {
                return entries[this]
            }

            fun String.toFloor(): DungeonFloor? {
                return entries.firstOrNull { it.bossName == this }
            }
        }
    }
}