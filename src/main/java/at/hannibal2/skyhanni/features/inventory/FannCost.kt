package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Matcher
import java.util.regex.Pattern

@SkyHanniModule
object FannCost {

    private val config get() = SkyHanniMod.feature.inventory.fannCost
    private val showCoins get() = config.coinsPerXP
    private val showBits get() = config.xpPerBit
    private var isDayTrainingMode = true

    private val patternGroup = RepoPattern.group("fann.inventory")
    private val trainingSlotInventoryPattern by patternGroup.pattern(
        "training",
        "Training Slot [1-3]",
    )
    private val anvilPattern by patternGroup.pattern(
        "anvil",
        "§aBegin Training",
    )
    private val expEarnedPattern by patternGroup.pattern(
        "exp",
        """Will earn a total of (\d{1,3}(?:,\d{3})*(?:\.\d+)?|\d+\.?\d*) EXP\.?""",
    )
    private val coinsPattern by patternGroup.pattern(
        "coin",
        """(\d{1,3}(?:,\d{3})*(?:\.\d+)?|\d+\.?\d*) Coins""",
    )
    private val desiredLevelPatter by patternGroup.pattern(
        "slot24.name.level",
        "Desired Level: (200|1?[0-9]?[0-9])",
    )
    private val userInputPattern by patternGroup.pattern(
        "slot24.name.input",
        "User Input",
    )

    @SubscribeEvent
    fun onFannAnvilTooltip(event: LorenzToolTipEvent) {
        if (!trainingSlotInventoryPattern.matches(InventoryUtils.openInventoryName())) return
        if (!anvilPattern.matches(event.itemStack.displayName)) return
        val tooltip = event.toolTip
        ChatUtils.debug("exp earned: ${tooltip.getExpEarned()}")
        ChatUtils.debug("coins: ${tooltip.getCoins()}")
        ChatUtils.debug("isDayTrainingMode: $isDayTrainingMode")
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!trainingSlotInventoryPattern.matches(event.inventoryName.removeColor())) return
        val slot24 = event.inventoryItems[24]
        if (slot24 != null) {
            val name = slot24.displayName.removeColor()
            if (desiredLevelPatter.matches(name)) {
                isDayTrainingMode = false
            } else if (userInputPattern.matches(name)) {
                isDayTrainingMode = true
            }
        }
    }


    private fun Pattern.read(lore: List<String>): Double? {
        for (line in lore) {
            val linePlain = line.removeColor()
            val matcher = matcher(linePlain)
            if (matcher.find()) {
                val res = matcher.group(1).replace(",", "").toDouble()
                return res
            }
        }
        return null
    }

    private fun List<String>.getCoins(): Double {
        return coinsPattern.read(this) ?: 0.0
    }

    private fun List<String>.getExpEarned(): Double? {
        return expEarnedPattern.read(this)
    }


}
