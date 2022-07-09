package at.lorenz.mod.dungeon

import at.lorenz.mod.events.DungeonEnterEvent
import at.lorenz.mod.misc.ScoreboardData
import at.lorenz.mod.utils.LorenzUtils
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class DungeonData {

    var dungeonFloor: String? = null

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