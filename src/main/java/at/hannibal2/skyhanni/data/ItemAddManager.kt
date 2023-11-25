package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.entity.ItemAddInInventoryEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matches
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

class ItemAddManager {
    enum class Source {
        ITEM_ADD,
        SACKS,
        ;
    }

    private val ARCHFIEND_DICE = "ARCHFIEND_DICE".asInternalName()
    private val HIGH_CLASS_ARCHFIEND_DICE = "HIGH_CLASS_ARCHFIEND_DICE".asInternalName()

    private val diceRollChatPattern =
        "§eYour §r§(5|6High Class )Archfiend Dice §r§erolled a §r§.(?<number>.)§r§e! Bonus: §r§.(?<hearts>.*)❤".toPattern()

    @SubscribeEvent
    fun onSackChange(event: SackChangeEvent) {
        if (!LorenzUtils.inSkyBlock) return

        for (sackChange in event.sackChanges) {
            val change = sackChange.delta
            if (change > 0) {
                val internalName = sackChange.internalName
                Source.SACKS.addItem(internalName, change)
            }
        }
    }

    @SubscribeEvent
    fun onItemAdd(event: ItemAddInInventoryEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val internalName = event.internalName
        if (internalName == ARCHFIEND_DICE || internalName == HIGH_CLASS_ARCHFIEND_DICE) {
            if (lastDiceRoll.passedSince() < 500.milliseconds) {
                return
            }
        }

        Source.ITEM_ADD.addItem(internalName, event.amount)
    }

    private fun Source.addItem(internalName: NEUInternalName, amount: Int) {
        ItemAddEvent(internalName, amount, this).postAndCatch()
    }

    private var lastDiceRoll = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (diceRollChatPattern.matches(event.message)) {
            lastDiceRoll = SimpleTimeMark.now()
        }
    }
}
