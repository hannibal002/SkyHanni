package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.OwnInventorItemUpdateEvent
import at.hannibal2.skyhanni.events.PacketEvent
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class OwnInventoryData {

//    private var itemNames = mutableMapOf<Int, String>()
//    private var itemAmount = mutableMapOf<Int, Int>()
//    private var counter = mutableMapOf<String, Int>()

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        if (packet is S2FPacketSetSlot) {
            val windowId = packet.func_149175_c()
            if (windowId == 0) {
                val item = packet.func_149174_e() ?: return
                OwnInventorItemUpdateEvent(item).postAndCatch()
            }
        }
//        if (packet is S2FPacketSetSlot) {
////            println("S2FPacketSetSlot")
//            val windowId = packet.func_149175_c()
//            val item = packet.func_149174_e()
//            val slot = packet.func_149173_d()
//            if (windowId != 0) return
//
//            val name = item?.name ?: "null"
//
//            val oldItem = itemNames.getOrDefault(slot, "null")
//            val oldAmount = itemAmount.getOrDefault(slot, 0)
//
////            println(" ")
////            println("windowId: $windowId")
//            val amount = item?.stackSize ?: 0
//            if (name == oldItem) {
//                if (amount > oldAmount) {
//                    val diff = amount - oldAmount
////                    println("added $diff $name")
//                    add(name, diff)
//                }
//            } else {
//                if (name != "null") {
////                    println("added new $amount $name")
//                    add(name, amount)
//                }
//            }
////            println("$slot $oldItem x$oldAmount -> $name x$amount")
//            itemNames[slot] = name
//            itemAmount[slot] = amount
//        }
    }

//    private fun add(name: String, add: Int) {
//        if (name == "§fHay Bale") return
//        if (name == "§fSeeds") return
//        if (name.contains("Hoe")) return
//
//        // TODO remove later
//        if (name.contains("Mushroom")) return
//
////        println("added $add $name")
//        val old = counter.getOrDefault(name, 0)
////        if (name == "§fWheat") {
////            if (old == 1502) {
////                old = 2504173
////            }
////        }
//        val new = old + add
//        val format = LorenzUtils.formatInteger(new)
//        println("have $name $format")
//        counter[name] = new
//    }
}