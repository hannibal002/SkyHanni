package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FossilExcavatorProfitTracker {

    private val patternGroup = RepoPattern.group("mining.fossil.excavator")
    private val chatPatternGroup = patternGroup.group("chat")

    /**
     * REGEX-TEST:   §r§6§lEXCAVATION COMPLETE
     */
    private val startPattern by chatPatternGroup.pattern("start", " {2}§r§6§lEXCAVATION COMPLETE ")

    /**
     * REGEX-TEST: §a§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    private val endPattern by chatPatternGroup.pattern("end", "§a§l▬{64}")

    /**
     * REGEX-TEST:     §r§6Tusk Fossil
     */
    private val itemPattern by chatPatternGroup.pattern("item", " {4}§r(?<item>.+)")

    private var inLoot = false
    private var newItems = mutableListOf<PrimitiveItemStack>()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val message = event.message

        if (startPattern.matches(message)) {
            inLoot = true
            return
        }

        if (!inLoot) return

        if (endPattern.matches(message)) {
            inLoot = false
            newExcavation()
            return
        }

        val (name, amount) = itemPattern.matchMatcher(message) {
            val itemLine = group("item")
            val newLine = itemLine.replace("§r", "")
            ItemUtils.readItemAmount(newLine) ?: return
        } ?: return
        val internalName = NEUInternalName.fromItemNameOrNull(name) ?: return
        val itemStack = internalName.makePrimitiveStack(amount)
        println("itemStack: '$itemStack'")
        newItems.add(itemStack)
    }

    private fun newExcavation() {

    }
}
