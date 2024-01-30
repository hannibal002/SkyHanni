package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.data.GetFromSackData
import at.hannibal2.skyhanni.utils.PrimitiveItemStack

object GetFromSackAPI {

    fun get(item: PrimitiveItemStack) = get(listOf(item))

    fun get(items: List<PrimitiveItemStack>) = GetFromSackData.addToQueue(items)

    fun chat(item: PrimitiveItemStack, text: String) = chat(listOf(item), text)

    fun chat(items: List<PrimitiveItemStack>, text: String) {} // TODO

    // fun slot(item: NEUInternalName,amount: Int) TODO
}
