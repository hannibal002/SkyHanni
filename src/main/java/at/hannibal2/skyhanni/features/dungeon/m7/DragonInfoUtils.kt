package at.hannibal2.skyhanni.features.dungeon.m7

import at.hannibal2.skyhanni.events.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.DungeonM7Phase5Start
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.M7DragonChangeEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DragonInfoUtils {
    private var inPhase5 = false

    @SubscribeEvent
    fun onDragonSpawn(event: MobEvent.Spawn) {
        if (inPhase5) return
        if (event.mob.name != "Ender Dragon") return

        val location = event.mob.baseEntity.position.toLorenzVec()
        M7DragonInfo.entries.filter { it.dragonLocation.spawnLocation == location }.forEach {
            ChatUtils.debug("Spawned Dragon ${it.name}")
            M7DragonChangeEvent(it, M7SpawnedStatus.ALIVE).postAndCatch()
            it.status = M7SpawnedStatus.ALIVE
            it.status.id = event.mob.baseEntity.entityId
        }
    }

    @SubscribeEvent
    fun onDragonKill(event: MobEvent.DeSpawn) {
        if (inPhase5) return
        if (event.mob.name != "Ender Dragon") return

        val location = event.mob.baseEntity.position.toLorenzVec()
        M7DragonInfo.entries.filter { it.status.id == event.mob.baseEntity.entityId }.forEach {
            if (it.dragonLocation.deathBox.isInside(location)) {
                ChatUtils.debug("Killed Dragon ${it.name}, inside box")
                it.status = M7SpawnedStatus.DEFEATED
                M7DragonChangeEvent(it, M7SpawnedStatus.DEFEATED).postAndCatch()
            } else {
                ChatUtils.debug("Killed Dragon ${it.name}, outside box")
                it.status = M7SpawnedStatus.UNDEFEATED
                M7DragonChangeEvent(it, M7SpawnedStatus.UNDEFEATED).postAndCatch()
            }
            it.status.id = -1
        }
    }

    @SubscribeEvent
    fun onParticles(event: PacketEvent.ReceiveEvent) {
        if (!inPhase5) return
        if (event.packet !is S2APacketParticles) return

        val particle = event.packet
        if (particle.particleType != EnumParticleTypes.FLAME ||
            particle.particleCount != 20 ||
            particle.particleSpeed != 0.0f ||
            particle.xOffset != 2.0f ||
            particle.yOffset != 3.0f ||
            particle.zOffset != 2.0f ||
            !particle.isLongDistance ||
            particle.xCoordinate % 1 != 0.0 ||
            particle.yCoordinate % 1 != 0.0 ||
            particle.zCoordinate % 1 != 0.0) return

        M7DragonInfo.entries.filter { it.status == M7SpawnedStatus.UNDEFEATED }.forEach {
            if (it.dragonLocation.particleBox.isInside(event.packet.toLorenzVec())) {
                it.status = M7SpawnedStatus.SPAWNING
                ChatUtils.debug("${it.name} is now spawning")
                M7DragonChangeEvent(it, M7SpawnedStatus.SPAWNING).postAndCatch()
            }
        }
    }

    @SubscribeEvent
    fun onStart(event: DungeonM7Phase5Start) {
        if (inPhase5) return
        ChatUtils.debug("Starting Phase5")
        inPhase5 = true
    }

    @SubscribeEvent
    fun onEnd(event: DungeonCompleteEvent) {
        if (inPhase5) inPhase5 = false
    }

    @SubscribeEvent
    fun onLeave(event: LorenzWorldChangeEvent) {
        if (inPhase5) inPhase5 = false
    }
}