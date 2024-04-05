package at.hannibal2.skyhanni.features.dungeon.m7

import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DragonInfoUtils {
    @SubscribeEvent
    fun onDragonSpawn(event: MobEvent.Spawn) {
        if (DungeonAPI.dungeonFloor != "M7") return
        if (event.mob.name != "Ender Dragon") return

        val location = event.mob.baseEntity.position.toLorenzVec()
        M7DragonInfo.entries.filter { it.dragonLocation.spawnLocation == location }.forEach {
            ChatUtils.debug("Spawned Dragon ${it.name}")
            it.status = M7SpawnedStatus.ALIVE
            it.status.id = event.mob.baseEntity.entityId
        }
    }

    @SubscribeEvent
    fun onDragonKill(event: MobEvent.DeSpawn) {
        if (DungeonAPI.dungeonFloor != "M7") return
        if (event.mob.name != "Ender Dragon") return

        val location = event.mob.baseEntity.position.toLorenzVec()
        M7DragonInfo.entries.filter { it.status.id == event.mob.baseEntity.entityId }.forEach {
            if (it.dragonLocation.deathBox.isInside(location)) {
                ChatUtils.debug("Killed Dragon ${it.name}, inside box")
                it.status = M7SpawnedStatus.DEFEATED
            } else {
                ChatUtils.debug("Killed Dragon ${it.name}, outside box")
                it.status = M7SpawnedStatus.UNDEFEATED
            }
            it.status.id = -1
        }
    }

    @SubscribeEvent
    fun onParticles(event: PacketEvent.ReceiveEvent) {
        if (!DungeonAPI.inMaster7Phase5) return
        if (event.packet !is S2APacketParticles) return

        val particle = event.packet
        if (particle.particleType != EnumParticleTypes.FLAME) return
        M7DragonInfo.entries.filter { it.status == M7SpawnedStatus.UNDEFEATED }.forEach {
            it.status = M7SpawnedStatus.SPAWNING
            ChatUtils.debug("${it.name} is now spawning")
        }
    }
}