package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.events.mining.FossilExcavationEvent
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FossilExcavatorProfitTracker {

    @SubscribeEvent
    fun onFossilExcavatrion(event: FossilExcavationEvent) {
        for ((name, amount) in event.loot) {
            println("")
            println("name: '$name'")
            println("amount: $amount")
            val internalName = NEUInternalName.fromItemNameOrNull(name) ?: continue
            val itemStack = internalName.makePrimitiveStack(amount)
            println("itemStack: '$itemStack'")
        }
    }
}
