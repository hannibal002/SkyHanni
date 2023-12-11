package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.fishing.trophyfishing.ChatMessagesConfig.DesignFormat
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager.fishes
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager.getTooltip
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addOrPut
import at.hannibal2.skyhanni.utils.LorenzUtils.sumAllValues
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.ordinal
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TrophyFishMessages {
    private val trophyFishPattern =
        "§6§lTROPHY FISH! §r§bYou caught an? §r(?<displayName>§[0-9a-f](?:§k)?[\\w -]+)§r§r§r §r§l§r(?<displayRarity>§[0-9a-f]§l\\w+)§r§b\\.".toPattern()
    private val config get() = SkyHanniMod.feature.fishing.trophyFishing.chatMessages

    @SubscribeEvent
    fun onStatusBar(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        var displayName = ""
        var displayRarity = ""

        trophyFishPattern.matchMatcher(event.message) {
            displayName = group("displayName").replace("§k", "")
            displayRarity = group("displayRarity")
        } ?: return

        val internalName = displayName.replace("Obfuscated", "Obfuscated Fish")
            .replace("[- ]".toRegex(), "").lowercase().removeColor()
        val rawRarity = displayRarity.lowercase().removeColor()
        val rarity = TrophyRarity.getByName(rawRarity) ?: return

        val trophyFishes = fishes ?: return
        val trophyFishCounts = trophyFishes.getOrPut(internalName) { mutableMapOf() }
        val amount = trophyFishCounts.addOrPut(rarity, 1)
        event.blockedReason = "trophy_fish"

        if (config.enabled && config.design == DesignFormat.STYLE_1 && amount == 1) {
            LorenzUtils.chat("§6§lTROPHY FISH! §c§lFIRST §r$displayRarity $displayName", prefix = false)
            return
        }

        if (config.bronzeHider && rarity == TrophyRarity.BRONZE && amount != 1) return
        if (config.silverHider && rarity == TrophyRarity.SILVER && amount != 1) return
        val totalText = if (config.totalAmount) {
            val total = trophyFishCounts.sumAllValues()
            " §7(${total.addSeparators()}. total)"
        } else ""

        val component = ChatComponentText(
            if (config.enabled) {
                "§6§lTROPHY FISH! " + when (config.design) {
                    DesignFormat.STYLE_1 -> "§7$amount. §r$displayRarity $displayName$totalText"
                    DesignFormat.STYLE_2 -> "§bYou caught a $displayName $displayRarity§b. §7(${amount.addSeparators()})$totalText"
                    else -> "§bYou caught your ${amount.addSeparators()}${amount.ordinal()} $displayRarity $displayName§b.$totalText"
                }
            } else event.message
        )

        if (config.tooltip) {
            TrophyFishManager.getInfo(internalName)?.let {
                component.chatStyle = it.getTooltip(trophyFishCounts)
            }
        }

        Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(
            component, if (config.duplicateHider) (internalName + rarity).hashCode() else 0
        )
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "fishing.trophyCounter", "fishing.trophyFishing.chatMessages.enabled")
        event.move(2, "fishing.trophyDesign", "fishing.trophyFishing.chatMessages.design")
        event.move(2, "fishing.trophyFishTotalAmount", "fishing.trophyFishing.chatMessages.totalAmount")
        event.move(2, "fishing.trophyFishTooltip", "fishing.trophyFishing.chatMessages.tooltip")
        event.move(2, "fishing.trophyFishDuplicateHider", "fishing.trophyFishing.chatMessages.duplicateHider")
        event.move(2, "fishing.trophyFishBronzeHider", "fishing.trophyFishing.chatMessages.bronzeHider")
        event.move(2, "fishing.trophyFishSilverHider", "fishing.trophyFishing.chatMessages.silverHider")
        event.transform(14, "fishing.trophyFishing.chatMessages.design") { element ->
            ConfigUtils.migrateIntToEnum(element, DesignFormat::class.java)
        }
    }
}
