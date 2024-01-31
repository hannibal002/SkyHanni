package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.data.GetFromSackData
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import net.minecraft.inventory.Slot

object GetFromSackAPI {

    private const val command = "gfs"

    fun get(item: PrimitiveItemStack) = get(listOf(item))

    fun get(items: List<PrimitiveItemStack>) = GetFromSackData.addToQueue(items)

    fun chat(item: PrimitiveItemStack, text: String = "Click here to grab §9x${item.amount} ${item.name.asString()}§e from sacks!") =
        LorenzUtils.clickableChat(text, "$command ${item.name.asString()} ${item.amount}")

    fun slot(items: List<PrimitiveItemStack>, slotIndex: Int) = GetFromSackData.addToInventory(items, slotIndex)

    fun Slot.getFromSack(items: List<PrimitiveItemStack>) = slot(items, slotIndex)
}
