package at.hannibal2.skyhanni.utils

import net.minecraft.world.item.Item

object ItemBlink {

    private val offsets = mutableMapOf<Item, Long>()
    private var lastOffset = 0L
    private var endOfBlink = 0L
    private var blinkItem: SafeItemStack? = null

    fun setBlink(item: SafeItemStack?, durationMillis: Long) {
        endOfBlink = System.currentTimeMillis() + durationMillis
        blinkItem = item
    }

    fun SafeItemStack.checkBlinkItem(): SafeItemStack {
        val stack = blinkItem ?: return this
        if (System.currentTimeMillis() > endOfBlink) return this

        val offset: Long = offsets.getOrPut(itemType) {
            lastOffset += 200
            val number = lastOffset % 1000
            offsets[itemType] = number
            number
        }

        return if ((offset + System.currentTimeMillis()) % 1000 > 500) stack else this
    }
}
