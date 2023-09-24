package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonData {
    private val floorPattern = " §7⏣ §cThe Catacombs §7\\((?<floor>.*)\\)".toPattern()
    private val uniqueClassBonus = "^Your ([A-Za-z]+) stats are doubled because you are the only player using this class!$".toRegex()

    companion object {
        var dungeonFloor: String? = null
        var started = false
        var inBossRoom = false
        var playerClass: DungeonClass? = null
        var playerClassLevel = -1
        var isUniqueClass = false

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
            val playerTeam = TabListData.getTabList().firstOrNull { it.contains(LorenzUtils.getPlayerName()) }?.removeColor() ?: ""

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

    enum class DungeonClass(public val scoreboardName: String) {
        ARCHER("Archer"),
        BERSERK("Berserk"),
        HEALER("Healer"),
        MAGE("Mage"),
        TANK("Tank")
    }
}