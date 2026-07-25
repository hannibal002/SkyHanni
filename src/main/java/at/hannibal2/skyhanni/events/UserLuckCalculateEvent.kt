package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.SafeItemStack

// gets fired when we want to calculate what the current player user luck values are
class UserLuckCalculateEvent : SkyHanniEvent() {

    private var totalLuck = 0f
    lateinit var mainLuckStack: SafeItemStack
    private val stacks = mutableMapOf<Int, SafeItemStack>()
    private val validItemSlots = (10..53).filter { it !in listOf(17, 18, 26, 27, 35, 36) && it !in 44..53 }.sorted()

    fun addLuck(luck: Float) {
        totalLuck += luck
    }

    fun getTotalLuck(): Float {
        return totalLuck
    }

    fun addItem(stack: SafeItemStack) {
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

    fun getStack(slot: Int): SafeItemStack? {
        val stack = stacks.getOrDefault(slot, null) ?: return null
        return stack
    }
}
