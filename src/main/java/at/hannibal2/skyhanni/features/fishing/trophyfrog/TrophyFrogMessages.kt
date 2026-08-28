package at.hannibal2.skyhanni.features.fishing.trophyfrog

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.fishing.trophyfrog.ChatMessagesConfig.DesignFormat
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.fishing.TrophyFrogCaughtEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.ordinal
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object TrophyFrogMessages {
    private val config get() = SkyHanniMod.feature.fishing.trophyFrogs.chatMessages

    // Leading run only allows colour codes and non-word symbols/glyphs, never player-chat text, so a
    // player typing a fake "TROPHY FROG!" line (with their [rank] name: prefix) can't be counted.
    // TODO: verify the exact catch-message format in-game; overridable via the repo without a code change.
    /**
     * REGEX-TEST: TROPHY FROG! You caught a Common Frog BRONZE!
     * REGEX-TEST: §2§lTROPHY FROG! §r§fYou caught a §r§9Common Frog §r§8§lBRONZE§r§f!
     */
    @Suppress("MaxLineLength")
    private val trophyFrogPattern by RepoPattern.pattern(
        "fishing.trophy.trophyfrog",
        "(?:§.|\\W)*?TROPHY FROG! (?:§.)*You caught an? (?:§.)*(?<displayName>[\\w -]+?) (?:§.)*(?<displayRarity>BRONZE|SILVER|GOLD|DIAMOND)(?:§.)*!",
    )

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val (displayName, displayRarity) = trophyFrogPattern.matchMatcher(event.message) {
            group("displayName") to group("displayRarity")
        } ?: return

        val name = displayName.removeColor()
        val rarity = TrophyRarity.getByName(displayRarity.lowercase().removeColor()) ?: return

        val trophyFrogs = TrophyFrogManager.frog ?: return
        val counts = trophyFrogs.getOrPut(name) { mutableMapOf() }
        val amount = counts.addOrPut(rarity, 1)
        TrophyFrogCaughtEvent(name, rarity).post()

        if (shouldBlock(rarity, amount)) {
            event.blockedReason = "low_trophy_frog"
            return
        }

        if (config.duplicateHider) event.chatLineId = (name + rarity).hashCode()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Modify) {
        val (displayName, displayRarity) = trophyFrogPattern.matchMatcher(event.message) {
            group("displayName") to group("displayRarity")
        } ?: return

        val name = displayName.removeColor()
        val rarity = TrophyRarity.getByName(displayRarity.lowercase().removeColor()) ?: return

        val trophyFrogs = TrophyFrogManager.frog ?: return
        val counts = trophyFrogs.getOrPut(name) { mutableMapOf() }
        val amount = counts[rarity] ?: 1

        val coloredName = TrophyFrogManager.getDisplayName(name)
        val rarityDisplay = "${rarity.formatCode}§l${rarity.name}"

        if (config.goldAlert && rarity == TrophyRarity.GOLD) {
            sendTitle(coloredName, rarityDisplay, amount)
            if (config.playSound) SoundUtils.playBeepSound()
        }

        if (config.diamondAlert && rarity == TrophyRarity.DIAMOND) {
            sendTitle(coloredName, rarityDisplay, amount)
            if (config.playSound) SoundUtils.playBeepSound()
        }

        val edited = if (config.enabled) {
            val designFormat = when (config.design) {
                DesignFormat.STYLE_1 -> if (amount == 1) "§c§lFIRST §r$rarityDisplay $coloredName"
                else "§7$amount${amount.ordinal()} §r$rarityDisplay $coloredName"

                DesignFormat.STYLE_2 -> "§bYou caught a $coloredName $rarityDisplay§b. §7(${amount.addSeparators()})"
                else -> "§bYou caught your ${amount.addSeparators()}${amount.ordinal()} $rarityDisplay $coloredName§b."
            }
            "§2§lTROPHY FROG! $designFormat".asComponent()
        } else event.chatComponent.copy()

        if (config.totalAmount) {
            val total = counts.sumAllValues()
            edited.append(" §7(${total.addSeparators()}${total.ordinal()} total)")
        }

        if (config.tooltip) {
            TrophyFrogApi.getTooltip(name)?.let {
                edited.toFlatList(it)
            }
        }

        event.replaceComponent(edited, "TROPHY_FROG")
    }

    private fun sendTitle(displayName: String, displayRarity: String, amount: Int) {
        TitleManager.sendTitle("$displayName $displayRarity §8$amount§c!")
    }

    private fun shouldBlock(rarity: TrophyRarity, amount: Int) =
        config.bronzeHider &&
            rarity == TrophyRarity.BRONZE &&
            amount != 1 ||
            config.silverHider &&
            rarity == TrophyRarity.SILVER &&
            amount != 1
}
