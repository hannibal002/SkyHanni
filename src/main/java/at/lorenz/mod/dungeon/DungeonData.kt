package at.lorenz.mod.dungeon

import at.lorenz.mod.events.DungeonEnterEvent
import at.lorenz.mod.misc.ScoreboardData
import at.lorenz.mod.utils.LorenzUtils
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