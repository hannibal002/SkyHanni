package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.DungeonEnterEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class DungeonData {

    companion object {
        var dungeonFloor: String? = null

        fun isOneOf(vararg floors: String): Boolean {
            for (floor in floors) {
                if (dungeonFloor == floor) {
                    return true
                }
            }

            return false
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (LorenzUtils.inDungeons) {
            if (dungeonFloor == null) {
                for (line in ScoreboardData.sidebarLines) {
                    if (line.contains("The Catacombs (")) {
                        dungeonFloor = line.substringAfter("(").substringBefore(")")
                        DungeonEnterEvent(dungeonFloor!!).postAndCatch()
                        break
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        dungeonFloor = null
    }
}