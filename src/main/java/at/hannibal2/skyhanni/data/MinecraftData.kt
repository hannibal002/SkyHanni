package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.minecraft.ServerTickEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.network.protocol.common.ClientboundPingPacket
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MinecraftData {

    var hasLeftMainScreen: Boolean = false
        private set

    @HandleEvent(priority = HandleEvent.LOW)
    fun onConnect() {
        hasLeftMainScreen = true
    }

    @HandleEvent(receiveCancelled = true)
    fun onPacket(event: PacketReceivedEvent) {
        val packet = event.packet as? ClientboundPingPacket ?: return

        if (lastPingParameter == packet.id) return
        lastPingParameter = packet.id

        ServerTickEvent(++totalServerTicks).post()
    }

    private var lastPingParameter = 0

    var totalServerTicks = 0
        private set

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        if (event.newItem != event.oldItem) {
            if (event.newItem != NeuInternalName.NONE) {
                InventoryUtils.recentItemsInHand.add(event.newItem)
                InventoryUtils.pastItemsInHand.add(SimpleTimeMark.now() to event.newItem)
            }
            InventoryUtils.itemInHandId = event.newItem
            InventoryUtils.latestItemInHand = event.newStack
            InventoryUtils.lastItemChangeTime = SimpleTimeMark.now()
        }
    }

    @HandleEvent
    fun onSecondPassed() {
        val cutoff = SimpleTimeMark.now() - 50.seconds
        InventoryUtils.pastItemsInHand.removeAll { it.first < cutoff }
    }

    @HandleEvent
    fun onWorldChange() {
        InventoryUtils.itemInHandId = NeuInternalName.NONE
        InventoryUtils.recentItemsInHand.clear()
    }
}
