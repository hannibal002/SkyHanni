package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ScoreboardChangeEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object BitsAPI {
    private val profileStorage get() = ProfileStorageData.profileSpecific?.bits
    private val playerStorage get() = SkyHanniMod.feature.storage
    var bits: Int
        get() = profileStorage?.bits ?: 0
        set(value) {
            profileStorage?.bits = value
        }
    var currentFameRank: FameRank
        get() = playerStorage?.currentFameRank ?: FameRank.NEW_PLAYER
        set(value) {
            playerStorage?.currentFameRank = value
        }
    var bitsToClaim: Int
        get() = profileStorage?.bitsToClaim ?: 0
        set(value) {
            profileStorage?.bitsToClaim = value
        }

    private const val defaultcookiebits = 4800

    private val bitsDataGroup = RepoPattern.group("data.bits")

    // Scoreboard patterns
    val bitsScoreboardPattern by bitsDataGroup.pattern(
        "scoreboard",
        "^Bits: §b(?<amount>[\\d,]+\\.?\\d*) ?§?3?(?:\\((?<earned>[+-][,\\d]+)?\\)?)?\$"
    )

    // Chat patterns related to bits
    private val bitsChatGroup = bitsDataGroup.group("chat")

    val bitsFromFameRankUpChatPattern by bitsChatGroup.pattern(
        "famerankup",
        "§eYou gained §3(?<amount>.*) Bits Available §ecompounded from all your §epreviously eaten §6cookies§e! Click here to open §6cookie menu§e!"
    )

    val bitsEarnedChatPattern by bitsChatGroup.pattern(
        "earned",
        "§f\\s+§8\\+§b(?<amount>.*)\\s+Bits\n"
    )

    val boosterCookieAte by bitsChatGroup.pattern(
        "boostercookieate",
        "§eYou consumed a §6Booster Cookie§e!.*"
    )

    // GUI patterns
    private val bitsGuiGroup = bitsDataGroup.group("gui")

    val bitsAvailableMenuPattern by bitsGuiGroup.pattern(
        "availablemenu",
        "§7Bits Available: §b(?<toClaim>[\\w,]+)(§3.+)?"
    )

    val fameRankSbMenuPattern by bitsGuiGroup.pattern(
        "sbmenufamerank",
        "§7Your rank: §e(?<rank>.*)"
    )

    val fameRankCommunityShopPattern by bitsGuiGroup.pattern(
        "communityshopfamerank",
        "§7Fame Rank: §e(?<rank>.*)"
    )

    val bitsGuiNamePattern by bitsGuiGroup.pattern(
        "mainmenuname",
        "^SkyBlock Menu$"
    )

    val bitsGuiStackPattern by bitsGuiGroup.pattern(
        "mainmenustack",
        "^§6Booster Cookie$"
    )

    val fameRankGuiNamePattern by bitsGuiGroup.pattern(
        "famerankmenuname",
        "^(Community Shop|Booster Cookie)$"
    )

    val fameRankGuiStackPattern by bitsGuiGroup.pattern(
        "famerankmenustack",
        "^(§aCommunity Shop|§eFame Rank)$"
    )

    @SubscribeEvent
    fun onScoreboardChange(event: ScoreboardChangeEvent) {
        if (!isEnabled()) return
        for (line in event.newList) {
            val message = line.trimWhiteSpace().removeResets()

            bitsScoreboardPattern.matchMatcher(message) {
                val amount = group("amount").formatNumber().toInt()
                val earned = group("earned")?.formatNumber()?.toInt() ?: 0
                bits = amount
                if (earned > 0) bitsToClaim -= earned

                return
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message.trimWhiteSpace().removeResets()

        bitsFromFameRankUpChatPattern.matchMatcher(message) {
            val amount = group("amount").formatNumber().toInt()
            bitsToClaim += amount

            return
        }

        bitsEarnedChatPattern.matchMatcher(message) {
            // Only two locations where the bits line isn't shown, but you can still get bits
            if (LorenzUtils.inAnyIsland(IslandType.CATACOMBS, IslandType.THE_RIFT)) return

            val amount = group("amount").formatNumber().toInt()
            bits += amount
            bitsToClaim -= amount

            return
        }

        boosterCookieAte.matchMatcher(message) {
            bitsToClaim += (defaultcookiebits * currentFameRank.bitsMultiplier).toInt()

            return
        }
    }

    @SubscribeEvent
    fun onInventoryFullyLoaded(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return

        val stacks = event.inventoryItems

        if (bitsGuiNamePattern.matches(event.inventoryName)) {
            val cookieStack = stacks.values.lastOrNull { bitsGuiStackPattern.matches(it.displayName) }
            if (cookieStack != null) {
                for (line in cookieStack.getLore()) {
                    bitsAvailableMenuPattern.matchMatcher(line) {
                        bitsToClaim = group("toClaim").formatNumber().toInt()

                        return
                    }
                }
            }
            return
        }

        if (fameRankGuiNamePattern.matches(event.inventoryName)) {
            val fameRankStack = stacks.values.lastOrNull { fameRankGuiStackPattern.matches(it.displayName) }
            if (fameRankStack != null) {
                line@for (line in fameRankStack.getLore()) {
                    fameRankCommunityShopPattern.matchMatcher(line) {
                        val rank = group("rank")
                        currentFameRank = FameRank.entries.firstOrNull { it.rank == rank } ?: FameRank.NEW_PLAYER

                        continue@line
                    }

                    fameRankSbMenuPattern.matchMatcher(line) {
                        val rank = group("rank")
                        currentFameRank = FameRank.entries.firstOrNull { it.rank == rank } ?: FameRank.NEW_PLAYER

                        return
                    }
                }
            }
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && profileStorage != null && playerStorage != null
}
