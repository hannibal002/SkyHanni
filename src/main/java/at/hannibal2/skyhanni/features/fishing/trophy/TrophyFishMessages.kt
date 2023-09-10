package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager.Companion.fishes
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addOrPut
import at.hannibal2.skyhanni.utils.LorenzUtils.sumAllValues
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.ordinal
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TrophyFishMessages {
    private val trophyFishPattern =
        Regex("§6§lTROPHY FISH! §r§bYou caught an? §r(?<displayName>§[0-9a-f](?:§k)?[\\w -]+)§r§r§r §r§l§r(?<displayRarity>§[0-9a-f]§l\\w+)§r§b\\.")
    private val config get() = SkyHanniMod.feature.fishing

    @SubscribeEvent
    fun onStatusBar(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val match = trophyFishPattern.matchEntire(event.message)?.groups ?: return
        val displayName = match["displayName"]!!.value.replace("§k", "")
        val displayRarity = match["displayRarity"]!!.value

        val internalName = displayName.replace("Obfuscated", "Obfuscated Fish")
            .replace("[- ]".toRegex(), "").lowercase().removeColor()
        val rawRarity = displayRarity.lowercase().removeColor()
        val rarity = TrophyRarity.getByName(rawRarity) ?: return

        val trophyFishes = fishes ?: return
        val trophyFishCounts = trophyFishes.getOrPut(internalName) { mutableMapOf() }
        val amount = trophyFishCounts.addOrPut(rarity, 1)
        event.blockedReason = "trophy_fish"

        if (config.trophyCounter && config.trophyDesign == 0 && amount == 1) {
            LorenzUtils.chat("§6§lTROPHY FISH! §c§lFIRST §r$displayRarity $displayName")
            return
        }

        if (config.trophyFishBronzeHider && rarity == TrophyRarity.BRONZE && amount != 1) return
        if (config.trophyFishSilverHider && rarity == TrophyRarity.SILVER && amount != 1) return
        val totalText = if (config.trophyFishTotalAmount) {
            val total = trophyFishCounts.sumAllValues()
            " §7(${total.addSeparators()}. total)"
        } else ""

        val component = ChatComponentText(
            if (config.trophyCounter) {
                "§6§lTROPHY FISH! " + when (config.trophyDesign) {
                    0 -> "§7$amount. §r$displayRarity $displayName$totalText"
                    1 -> "§bYou caught a $displayName $displayRarity§b. §7(${amount.addSeparators()})$totalText"
                    else -> "§bYou caught your ${amount.addSeparators()}${amount.ordinal()} $displayRarity $displayName§b.$totalText"
                }
            } else event.message
        )

        if (config.trophyFishTooltip) {
            TrophyFishManager.getInfo(internalName)?.let {
                component.chatStyle = it.getTooltip(trophyFishCounts)
            }
        }

        Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(
            component, if (config.trophyFishDuplicateHider) (internalName + rarity).hashCode() else 0
        )
    }
}
