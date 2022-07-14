package at.lorenz.mod.dungeon

import at.lorenz.mod.LorenzMod
import at.lorenz.mod.events.CheckRenderEntityEvent
import at.lorenz.mod.events.DamageIndicatorFinalBossEvent
import at.lorenz.mod.events.LorenzChatEvent
import at.lorenz.mod.events.PacketEvent
import at.lorenz.mod.utils.LorenzUtils
import at.lorenz.mod.utils.LorenzUtils.matchRegex
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityGuardian
import net.minecraft.network.play.server.S1CPacketEntityMetadata
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonCleanEnd {

    private var bossDone = false
    private var chestsSpawned = false
    private var lastBossId: Int = -1

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inDungeons) return
        if (!LorenzMod.feature.dungeon.cleanEnd) return

        val message = event.message

        if (message.matchRegex("([ ]*)§r§c(The|Master Mode) Catacombs §r§8- §r§eFloor (.*)")) {
            chestsSpawned = true
        }
    }

    private fun shouldBlock(): Boolean {
        if (!LorenzUtils.inDungeons) return false
        if (!LorenzMod.feature.dungeon.cleanEnd) return false

        if (!bossDone) return false

        return true
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        bossDone = false
        chestsSpawned = false
        lastBossId = -1
    }

    @SubscribeEvent
    fun onBossDead(event: DamageIndicatorFinalBossEvent) {
        if (!LorenzUtils.inDungeons) return
        if (bossDone) return

        if (lastBossId == -1) {
            lastBossId = event.id
        }
    }

    @SubscribeEvent
    fun onHealthUpdatePacket(event: PacketEvent.ReceiveEvent) {
        if (!LorenzUtils.inDungeons) return
        if (!LorenzMod.feature.dungeon.cleanEnd) return

        if (bossDone) return
        if (lastBossId == -1) return

        val packet = event.packet
        if (packet !is S1CPacketEntityMetadata) return
        if (packet.entityId != lastBossId) return

        for (watchableObject in packet.func_149376_c()) {
            if (watchableObject.dataValueId == 6) {
                val health = watchableObject.`object` as Float
                if (health < 1) {
                    val dungeonFloor = DungeonData.dungeonFloor
                    LorenzUtils.chat("§eFloor $dungeonFloor done!")
                    bossDone = true
                }
            }
        }
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!shouldBlock()) return

        val entity = event.entity

        if (entity == Minecraft.getMinecraft().thePlayer) return

        if (LorenzMod.feature.dungeon.cleanEndF3IgnoreGuardians) {
            if (DungeonData.isOneOf("F3", "M3")) {
                if (entity is EntityGuardian) {
                    if (entity.entityId != lastBossId) {
                        if (Minecraft.getMinecraft().thePlayer.isSneaking) {
                            return
                        }
                    }
                }
            }
        }

        if (chestsSpawned) {
            if (entity is EntityArmorStand) {
                if (!entity.hasCustomName()) {
                    return
                }
            }
            if (entity is EntityOtherPlayerMP) {
                return
            }
        }

        event.isCanceled = true
    }

    @SubscribeEvent
    fun onReceivePacket(event: PacketEvent.ReceiveEvent) {
        if (!shouldBlock()) return


        if (event.packet is S2APacketParticles) {
            event.isCanceled = true
        }
    }
}