package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.fishing.trophyfishing.ChatMessagesConfig.DesignFormat
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.fishing.TrophyFishCaughtEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager.getTooltip
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.ordinal
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.compat.appendComponent
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TrophyFishMessages {
    private val config get() = SkyHanniMod.feature.fishing.trophyFishing.chatMessages

    /**
     * REGEX-TEST: §6♔ §r§6§lTROPHY FISH! §r§fYou caught a §r§9Lavahorse §r§6§lGOLD§r§f!
     * REGEX-TEST: §6♔ §r§6§lTROPHY FISH! §r§fYou caught a §r§5Soul Fish §r§8§lBRONZE§r§f!
     * REGEX-TEST: §6♔ §r§6§lTROPHY FISH! §r§fYou caught a §r§9Mana Ray §r§8§lBRONZE§r§f!
     * REGEX-TEST: §6♔ §r§6§lTROPHY FISH! §r§fYou caught a §r§fBlobfish §r§7§lSILVER§r§f!
     * REGEX-TEST: §6♔ §r§6§lTROPHY FISH! §r§fYou caught a §r§6Golden Fish §r§7§lSILVER§r§f!
     */
    @Suppress("MaxLineLength")
    val trophyFishPattern by RepoPattern.pattern(
        "fishing.trophy.trophyfish",
        "§6♔ §r§6§lTROPHY FISH! §r§fYou caught an? §r(?<displayName>§[0-9a-f](?:§k)?[\\w -]+) §r(?<displayRarity>§[0-9a-f]§l\\w+)§r§f!"
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent) {
        val (displayName, displayRarity) = trophyFishPattern.matchMatcher(event.message) {
            group("displayName").replace("§k", "") to
                group("displayRarity")
        } ?: return

        val internalName = getInternalName(displayName)
        val rarity = TrophyRarity.getByName(displayRarity.lowercase().removeColor()) ?: return

        val trophyFishes = TrophyFishManager.fish ?: return
        val trophyFishCounts = trophyFishes.getOrPut(internalName) { mutableMapOf() }
        val amount = trophyFishCounts.addOrPut(rarity, 1)
        TrophyFishCaughtEvent(internalName, rarity).post()

        if (shouldBlockTrophyFish(rarity, amount)) {
            event.blockedReason = "low_trophy_fish"
            return
        }
        if (config.goldAlert && rarity == TrophyRarity.GOLD) {
            sendTitle(displayName, displayRarity, amount)
            if (config.playSound) SoundUtils.playBeepSound()
        }

        if (config.diamondAlert && rarity == TrophyRarity.DIAMOND) {
            sendTitle(displayName, displayRarity, amount)
            if (config.playSound) SoundUtils.playBeepSound()
        }

        val original = event.chatComponent
        var edited = original

        if (config.enabled) {
            edited = (
                "§6§lTROPHY FISH! " + when (config.design) {
                    DesignFormat.STYLE_1 -> if (amount == 1) "§c§lFIRST §r$displayRarity $displayName"
                    else "§7$amount${amount.ordinal()} §r$displayRarity $displayName"

                    DesignFormat.STYLE_2 -> "§bYou caught a $displayName $displayRarity§b. §7(${amount.addSeparators()})"
                    else -> "§bYou caught your ${amount.addSeparators()}${amount.ordinal()} $displayRarity $displayName§b."
                }
                ).asComponent()
        }

        if (config.totalAmount) {
            val total = trophyFishCounts.sumAllValues()

            edited.appendComponent((" §7(${total.addSeparators()}${total.ordinal()} total)").asComponent())
        }

        if (config.tooltip) {
            getTooltip(internalName)?.let {
                edited.chatStyle = it
            }
        }

        event.chatComponent = edited

        if (config.duplicateHider) {
            event.chatLineId = (internalName + rarity).hashCode()
        }
    }

    private fun sendTitle(displayName: String, displayRarity: String?, amount: Int) {
        val text = "$displayName $displayRarity §8$amount§c!"
        TitleManager.sendTitle(text, 3.seconds, 2.8, 7f)
    }

    val regex = "[- ]".toRegex()

    fun getInternalName(displayName: String): String {
        return displayName.replace("Obfuscated", "Obfuscated Fish")
            .replace(regex, "").lowercase().removeColor()
    }

    private fun shouldBlockTrophyFish(rarity: TrophyRarity, amount: Int) =
        config.bronzeHider &&
            rarity == TrophyRarity.BRONZE &&
            amount != 1 ||
            config.silverHider &&
            rarity == TrophyRarity.SILVER &&
            amount != 1

    @HandleEvent
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
