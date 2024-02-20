package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.NEUItems.getItemStack

data class PrimitiveItemStack(val name: NEUInternalName, val amount: Int) {

    fun createItem() = name.getItemStack().apply { stackSize = amount }

    companion object {

        fun NEUInternalName.makePrimitiveStack(amount: Int) = PrimitiveItemStack(this, amount)
    }
}
