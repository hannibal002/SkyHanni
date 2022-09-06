package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.test.LorenzTest
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonHideItems {

    private val hideParticles = mutableMapOf<EntityArmorStand, Long>()

    private val blessingTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZT" +
                "kzZTIwNjg2MTc4NzJjNTQyZWNkYTFkMjdkZjRlY2U5MWM2OTk5MDdiZjMyN2M0ZGRiODUzMDk0MTJkMzkzOSJ9fX0="

    private val reviveStoneTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJ" +
            "lcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjZhNzZjYzIyZTdjMmFiOWM1NDBkMTI0NGVhZGJhNTgxZ" +
            "jVkZDllMThmOWFkYWNmMDUyODBhNWI0OGI4ZjYxOCJ9fX0K"

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!LorenzUtils.inDungeons) return

        val entity = event.entity
        if (entity !is EntityArmorStand) return

        if (SkyHanniMod.feature.dungeon.hideSuperboomTNT) {
            if (entity.name.startsWith("§9Superboom TNT")) {
                event.isCanceled = true
            }

            val itemStack = entity.inventory[4]
            if (itemStack != null) {
                if (itemStack.cleanName() == "Superboom TNT") {
                    event.isCanceled = true
                    hideParticles[entity] = System.currentTimeMillis()
                }
            }
        }

        if (SkyHanniMod.feature.dungeon.hideBlessings) {
            if (entity.name.startsWith("§dBlessing of ")) {
                event.isCanceled = true
            }

            val itemStack = entity.inventory[4]
            if (itemStack != null) {
                if (itemStack.getSkullTexture() == blessingTexture) {
                    event.isCanceled = true
                }
            }
        }

        if (SkyHanniMod.feature.dungeon.hideReviveStones) {
            if (entity.name == "§6Revive Stone") {
                event.isCanceled = true
            }

            val itemStack = entity.inventory[4]
            if (itemStack != null) {
                if (itemStack.getSkullTexture() == reviveStoneTexture) {
                    event.isCanceled = true
                    hideParticles[entity] = System.currentTimeMillis()
                }
            }
        }
    }

    @SubscribeEvent
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!LorenzUtils.inDungeons) return
        if (!SkyHanniMod.feature.dungeon.hideSuperboomTNT && !SkyHanniMod.feature.dungeon.hideReviveStones) return

        val packet = event.packet
        if (packet is S2APacketParticles) {
            val packetLocation = LorenzVec(packet.xCoordinate, packet.yCoordinate, packet.zCoordinate)
            for (armorStand in hideParticles.filter { it.value + 100 > System.currentTimeMillis() }.map { it.key }) {
                val distance = packetLocation.distance(armorStand.getLorenzVec())
                if (distance < LorenzTest.a.toInt()) {
                    //only hiding white "sparkling" particles
                    if (packet.particleType.particleID == 3) {
                        event.isCanceled = true
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        hideParticles.clear()
    }
}