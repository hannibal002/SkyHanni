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
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import kotlin.time.Duration.Companion.seconds
//#if MC < 1.21
//$$ import net.minecraft.network.packet.s2c.play.ConfirmScreenActionS2CPacket
//#else
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket
//#endif

@SkyHanniModule
object MinecraftData {

    @HandleEvent(receiveCancelled = true)
    fun onPacket(event: PacketReceivedEvent) {
        when (val packet = event.packet) {
            is PlaySoundS2CPacket -> {
                if (PlaySoundEvent(
                        //#if MC < 1.21
                        //$$ packet.soundName,
                        //#else
                        packet.sound.value().id.toString().removePrefix("minecraft:"),
                        //#endif
                        LorenzVec(packet.x, packet.y, packet.z), packet.pitch, packet.volume,
                    ).post()
                ) {
                    event.cancel()
                }
            }

            is ParticleS2CPacket -> {
                if (ReceiveParticleEvent(
                        //#if MC < 1.21
                        //$$ packet.particleType,
                        //#else
                        packet.parameters.type,
                        //#endif
                        LorenzVec(packet.x, packet.y, packet.z),
                        packet.count,
                        packet.speed,
                        LorenzVec(packet.offsetX, packet.offsetY, packet.offsetZ),
                        packet.shouldForceSpawn(),
                        //#if MC < 1.21
                        //$$ packet.particleArgs,
                        //#endif
                    ).post()
                ) {
                    event.cancel()
                }
            }

            //#if MC < 1.21
            //$$ is ConfirmScreenActionS2CPacket -> {
            //$$     if (packet.actionId > 0) return
                //#else
                is CommonPingS2CPacket -> {
                if (lastPingParameter == packet.parameter) return
                lastPingParameter = packet.parameter
                //#endif

                totalServerTicks++
                ServerTickEvent.post()
            }
        }
    }

    //#if MC > 1.21
    private var lastPingParameter = 0
    //#endif

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
