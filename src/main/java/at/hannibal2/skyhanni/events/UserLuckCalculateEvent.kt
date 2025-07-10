package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.item.ItemStack

// gets fired when we want to calculate what the current player user luck values are
class UserLuckCalculateEvent : SkyHanniEvent() {

    private var totalLuck = 0f
    lateinit var mainLuckStack: ItemStack
    private val stacks = mutableMapOf<Int, ItemStack>()
    private val validItemSlots = (10..53).filter { it !in listOf(17, 18, 26, 27, 35, 36) && it !in 44..53 }.sorted()

    fun addLuck(luck: Float) {
        totalLuck += luck
    }

    fun getTotalLuck(): Float {
        return totalLuck
    }

    fun addItem(stack: ItemStack) {
        var slot: Int = -1
        for (validItemSlot in validItemSlots) {
            if (!stacks.contains(validItemSlot)) {
                slot = validItemSlot
                break
            }
        }
        if (slot == -1) {
            ErrorManager.skyHanniError(
                "Looks like we ran out of space in the user luck menu! This means *someone* has to add pages to it :)"
            )
        }
        stacks[slot] = stack
    }

    fun getStack(slot: Int): ItemStack? {
        val stack = stacks.getOrDefault(slot, null) ?: return null
        return stack
    }
}
