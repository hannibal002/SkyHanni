package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack

data class PrimitiveItemStack(val internalName: NEUInternalName, val amount: Int) {

    fun createItem() = internalName.getItemStack().apply { stackSize = amount }

    val itemName by lazy { internalName.itemName }

    companion object {

        fun NEUInternalName.makePrimitiveStack(amount: Int) = PrimitiveItemStack(this, amount)
    }
}
