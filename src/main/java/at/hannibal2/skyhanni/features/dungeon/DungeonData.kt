package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.DungeonBossRoomEnterEvent
import at.hannibal2.skyhanni.events.DungeonEnterEvent
import at.hannibal2.skyhanni.events.DungeonStartEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.regex.Pattern

class DungeonData {

    private val pattern = Pattern.compile(" §7⏣ §cThe Catacombs §7\\((.*)\\)")

    companion object {
        var dungeonFloor: String? = null
        var inBossRoom = false
        var started = false

        fun inDungeon() = dungeonFloor != null

        fun isOneOf(vararg floors: String): Boolean {
            for (floor in floors) {
                if (dungeonFloor == floor) {
                    return true
                }
            }

            return false
        }

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
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (dungeonFloor == null) {
            for (line in ScoreboardData.sidebarLinesFormatted) {
                val matcher = pattern.matcher(line)
                if (matcher.matches()) {
                    val floor = matcher.group(1)
                    dungeonFloor = floor
                    DungeonEnterEvent(floor).postAndCatch()
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
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