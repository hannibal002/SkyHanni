package at.hannibal2.skyhanni.data

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
    var bits: Int
        get() = ProfileStorageData.profileSpecific?.bits?.bits ?: 0
        set(value) {
            ProfileStorageData.profileSpecific?.bits?.bits = value
        }
    var currentFameRank: FameRank
        get() = ProfileStorageData.profileSpecific?.bits?.currentFameRank ?: FameRank.NEW_PLAYER
        set(value) {
            ProfileStorageData.profileSpecific?.bits?.currentFameRank = value
        }
    var bitsToClaim: Int
        get() = ProfileStorageData.profileSpecific?.bits?.bitsToClaim ?: 0
        set(value) {
            ProfileStorageData.profileSpecific?.bits?.bitsToClaim = value
        }

    private const val defaultcookiebits = 4800

    private val group = RepoPattern.group("data.bits")
    val bitsScoreboardPattern by group.pattern(
        "scoreboard",
        "^Bits: §b(?<amount>[\\d,]+\\.?\\d*) ?§?3?(?:\\((?<earned>[+-][,\\d]+)?\\)?)?\$"
    )
    private val bitsFromFameRankUpChatPattern by group.pattern(
        "chat.bits.famerankup",
        "§eYou gained §3(?<amount>.*) Bits Available §ecompounded from all your §epreviously eaten §6cookies§e! Click here to open §6cookie menu§e!"
    )
    private val bitsEarnedChatPattern by group.pattern("chat.earned", "§f\\s+§8\\+§b(?<amount>.*)\\s+Bits\n")
    private val boosterCookieAte by group.pattern("chat.boostercookie.ate", "§eYou consumed a §6Booster Cookie§e!.*")
    private val bitsAvailableMenu by group.pattern(
        "gui.bitsavailablemenu",
        "§7Bits Available: §b(?<toClaim>[\\w,]+)(§3.+)?"
    )
    private val fameRankSbmenu by group.pattern("gui.sbmenu.famerank", "§7Your rank: §e(?<rank>.*)")
    private val fameRankCommunityShop by group.pattern("gui.famerankcommunityshop", "§7Fame Rank: §e(?<rank>.*)")
    private val bitsGuiNamePattern by group.pattern("gui.mainmenuname", "^SkyBlock Menu$")
    private val bitsGuiStackPattern by group.pattern("gui.mainmenustack", "^§6Booster Cookie$")
    private val fameRankGuiNamePattern by group.pattern("gui.famerankmenuname", "^(Community Shop|Booster Cookie)$")
    private val fameRankGuiStackPattern by group.pattern("gui.famerankmenustack", "^(§aCommunity Shop|§eFame Rank)$")

    @SubscribeEvent
    fun onScoreboardChange(event: ScoreboardChangeEvent) {
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
        if (!LorenzUtils.inSkyBlock) return

        val stacks = event.inventoryItems

        if (bitsGuiNamePattern.matches(event.inventoryName)) {
            val cookieStack = stacks.values.lastOrNull { bitsGuiStackPattern.matches(it.displayName) }
            if (cookieStack != null) {
                for (line in cookieStack.getLore()) {
                    bitsAvailableMenu.matchMatcher(line) {
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
                for (line in fameRankStack.getLore()) {
                    fameRankCommunityShop.matchMatcher(line) {
                        val rank = group("rank")
                        currentFameRank = FameRank.entries.firstOrNull { it.rank == rank } ?: FameRank.NEW_PLAYER

                        return
                    }

                    fameRankSbmenu.matchMatcher(line) {
                        val rank = group("rank")
                        currentFameRank = FameRank.entries.firstOrNull { it.rank == rank } ?: FameRank.NEW_PLAYER

                        return
                    }
                }
            }
        }
    }
}
