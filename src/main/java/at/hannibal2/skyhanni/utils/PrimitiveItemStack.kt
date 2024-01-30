package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.NEUItems.getItemStack

// TODO move to some other spot. This can be used at other features as well
data class PrimitiveItemStack(val name: NEUInternalName, val amount: Int) {

    fun createItem() = name.getItemStack().apply { stackSize = amount }
}
