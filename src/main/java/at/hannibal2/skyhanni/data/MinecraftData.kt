package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.ServerTickEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MinecraftData {

    @HandleEvent(receiveCancelled = true)
    fun onPacket(event: PacketReceivedEvent) {
        when (val packet = event.packet) {
            is ClientboundSoundPacket -> {
                if (PlaySoundEvent(
                        packet.sound.value().location().toString().removePrefix("minecraft:"),
                        LorenzVec(packet.x, packet.y, packet.z), packet.pitch, packet.volume,
                    ).post()
                ) {
                    event.cancel()
                }
            }

            is ClientboundLevelParticlesPacket -> {
                if (ReceiveParticleEvent(
                        packet.particle.type,
                        LorenzVec(packet.x, packet.y, packet.z),
                        packet.count,
                        packet.maxSpeed,
                        LorenzVec(packet.xDist, packet.yDist, packet.zDist),
                        packet.isOverrideLimiter,
                    ).post()
                ) {
                    event.cancel()
                }
            }

            is ClientboundPingPacket -> {
                if (lastPingParameter == packet.id) return
                lastPingParameter = packet.id

                totalServerTicks++
                ServerTickEvent.post()
            }
        }
    }

    private var lastPingParameter = 0

    var totalServerTicks: Long = 0L
        private set

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        val hand = InventoryUtils.getItemInHand()
        val newItem = hand?.getInternalName() ?: NeuInternalName.NONE
        val oldItem = InventoryUtils.itemInHandId
        if (newItem != oldItem) {
            if (newItem != NeuInternalName.NONE) {
                InventoryUtils.recentItemsInHand.add(newItem)
                InventoryUtils.pastItemsInHand.add(Pair(SimpleTimeMark.now(), newItem))
            }
            InventoryUtils.itemInHandId = newItem
            InventoryUtils.latestItemInHand = hand
            InventoryUtils.lastItemChangeTime = SimpleTimeMark.now()
            ItemInHandChangeEvent(newItem, oldItem).post()
        }
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        val cutoff = SimpleTimeMark.now() - 50.seconds
        InventoryUtils.pastItemsInHand.removeAll { it.first < cutoff }
    }

    @HandleEvent
    fun onWorldChange() {
        InventoryUtils.itemInHandId = NeuInternalName.NONE
        InventoryUtils.recentItemsInHand.clear()
    }
}
