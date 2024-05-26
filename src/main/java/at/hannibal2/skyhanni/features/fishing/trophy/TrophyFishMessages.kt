package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.fishing.trophyfishing.ChatMessagesConfig.DesignFormat
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager.getTooltip
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.ordinal
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TrophyFishMessages {
    private val config get() = SkyHanniMod.feature.fishing.trophyFishing.chatMessages

    private val trophyFishPattern by RepoPattern.pattern(
        "fishing.trophy.trophyfish",
        "§6§lTROPHY FISH! §r§bYou caught an? §r(?<displayName>§[0-9a-f](?:§k)?[\\w -]+) §r(?<displayRarity>§[0-9a-f]§l\\w+)§r§b\\."
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
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

        val trophyFishes = TrophyFishManager.fish ?: return
        val trophyFishCounts = trophyFishes.getOrPut(internalName) { mutableMapOf() }
        val amount = trophyFishCounts.addOrPut(rarity, 1)

        if (shouldBlockTrophyFish(rarity, amount)) {
            event.blockedReason = "low_trophy_fish"
            return
        }

        val original = event.chatComponent
        var edited = original

        if (config.enabled) {
            edited = ChatComponentText(
                "§6§lTROPHY FISH! " + when (config.design) {
                    DesignFormat.STYLE_1 -> if (amount == 1) "§c§lFIRST §r$displayRarity $displayName"
                    else "§7$amount${amount.ordinal()} §r$displayRarity $displayName"

                    DesignFormat.STYLE_2 -> "§bYou caught a $displayName $displayRarity§b. §7(${amount.addSeparators()})"
                    else -> "§bYou caught your ${amount.addSeparators()}${amount.ordinal()} $displayRarity $displayName§b."
                }
            )
        }

        if (config.totalAmount) {
            val total = trophyFishCounts.sumAllValues()

            edited.appendSibling(ChatComponentText(" §7(${total.addSeparators()}${total.ordinal()} total)"))
        }

        if (config.tooltip) {
            TrophyFishManager.getInfo(internalName)?.let {
                edited.chatStyle = it.getTooltip(trophyFishCounts)
            }
        }

        event.chatComponent = edited

        if (config.duplicateHider) {
            event.chatLineId = (internalName + rarity).hashCode()
        }
    }

    private fun shouldBlockTrophyFish(rarity: TrophyRarity, amount: Int) =
        config.bronzeHider && rarity == TrophyRarity.BRONZE && amount != 1
            || config.silverHider && rarity == TrophyRarity.SILVER && amount != 1

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "fishing.trophyCounter", "fishing.trophyFishing.chatMessages.enabled")
        event.move(2, "fishing.trophyDesign", "fishing.trophyFishing.chatMessages.design")
        event.move(2, "fishing.trophyFishTotalAmount", "fishing.trophyFishing.chatMessages.totalAmount")
        event.move(2, "fishing.trophyFishTooltip", "fishing.trophyFishing.chatMessages.tooltip")
        event.move(2, "fishing.trophyFishDuplicateHider", "fishing.trophyFishing.chatMessages.duplicateHider")
        event.move(2, "fishing.trophyFishBronzeHider", "fishing.trophyFishing.chatMessages.bronzeHider")
        event.move(2, "fishing.trophyFishSilverHider", "fishing.trophyFishing.chatMessages.silverHider")

        event.transform(15, "fishing.trophyFishing.chatMessages.design") { element ->
            ConfigUtils.migrateIntToEnum(element, DesignFormat::class.java)
        }
    }
}
