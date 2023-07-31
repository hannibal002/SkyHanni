package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.LorenzUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonData {
    private val floorPattern = " §7⏣ §cThe Catacombs §7\\((?<floor>.*)\\)".toPattern()

    companion object {
        var dungeonFloor: String? = null
        var started = false
        var inBossRoom = false

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
}