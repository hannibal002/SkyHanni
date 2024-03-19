package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.FameRanks.getFameRankByNameOrNull
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ScoreboardChangeEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
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
        private set(value) {
            profileStorage?.bits = value
        }
    var currentFameRank: FameRank?
        get() = playerStorage?.currentFameRank?.let { getFameRankByNameOrNull(it) }
        private set(value) {
            if (value != null) {
                playerStorage?.currentFameRank = value.name
            }
        }
    var bitsToClaim: Int
        get() = profileStorage?.bitsToClaim ?: 0
        private set(value) {
            profileStorage?.bitsToClaim = value
        }

    private const val defaultcookiebits = 4800

    private val bitsDataGroup = RepoPattern.group("data.bits")

    // Scoreboard patterns
    val bitsScoreboardPattern by bitsDataGroup.pattern(
        "scoreboard",
        "^Bits: §b(?<amount>[\\d,.]+).*$"
    )

    // Chat patterns
    private val bitsChatGroup = bitsDataGroup.group("chat")

    private val bitsFromFameRankUpChatPattern by bitsChatGroup.pattern(
        "famerankup",
        "§eYou gained §3(?<amount>.*) Bits Available §ecompounded from all your §epreviously eaten §6cookies§e! Click here to open §6cookie menu§e!"
    )

    private val boosterCookieAte by bitsChatGroup.pattern(
        "boostercookieate",
        "§eYou consumed a §6Booster Cookie§e!.*"
    )

    // GUI patterns
    private val bitsGuiGroup = bitsDataGroup.group("gui")

    private val bitsAvailableMenuPattern by bitsGuiGroup.pattern(
        "availablemenu",
        "§7Bits Available: §b(?<toClaim>[\\d,]+)(§3.+)?"
    )

    private val fameRankSbMenuPattern by bitsGuiGroup.pattern(
        "sbmenufamerank",
        "§7Your rank: §e(?<rank>.*)"
    )

    private val fameRankCommunityShopPattern by bitsGuiGroup.pattern(
        "communityshopfamerank",
        "§7Fame Rank: §e(?<rank>.*)"
    )

    private val bitsGuiNamePattern by bitsGuiGroup.pattern(
        "mainmenuname",
        "^SkyBlock Menu$"
    )

    private val bitsGuiStackPattern by bitsGuiGroup.pattern(
        "mainmenustack",
        "^§6Booster Cookie$"
    )

    private val fameRankGuiNamePattern by bitsGuiGroup.pattern(
        "famerankmenuname",
        "^(Community Shop|Booster Cookie)$"
    )

    private val fameRankGuiStackPattern by bitsGuiGroup.pattern(
        "famerankmenustack",
        "^(§aCommunity Shop|§eFame Rank)$"
    )

    @SubscribeEvent
    fun onScoreboardChange(event: ScoreboardChangeEvent) {
        if (!isEnabled()) return
        for (line in event.newList) {
            val message = line.trimWhiteSpace().removeResets()

            bitsScoreboardPattern.matchMatcher(message) {
                val amount = group("amount").formatInt()

                if (amount > bits) {
                    bitsToClaim -= amount - bits
                    ChatUtils.debug("You have gained §3${amount - bits} Bits §7according to the scoreboard!")
                }
                bits = amount

                return
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message.trimWhiteSpace().removeResets()

        bitsFromFameRankUpChatPattern.matchMatcher(message) {
            val amount = group("amount").formatInt()
            bitsToClaim += amount

            return
        }

        boosterCookieAte.matchMatcher(message) {
            bitsToClaim += (defaultcookiebits * (currentFameRank?.bitsMultiplier ?: return)).toInt()

            return
        }
    }

    @SubscribeEvent
    fun onInventoryFullyLoaded(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return

        val stacks = event.inventoryItems

        if (bitsGuiNamePattern.matches(event.inventoryName)) {
            val cookieStack = stacks.values.lastOrNull { bitsGuiStackPattern.matches(it.displayName) }

            // If the cookie stack is null, then the player should not have any bits to claim
            if (cookieStack == null) {
                bitsToClaim = 0
                return
            }

            for (line in cookieStack.getLore()) {
                bitsAvailableMenuPattern.matchMatcher(line) {
                    bitsToClaim = group("toClaim").formatInt()

                    return
                }
            }
            return
        }

        if (fameRankGuiNamePattern.matches(event.inventoryName)) {
            val fameRankStack = stacks.values.lastOrNull { fameRankGuiStackPattern.matches(it.displayName) } ?: return

            line@ for (line in fameRankStack.getLore()) {
                fameRankCommunityShopPattern.matchMatcher(line) {
                    val rank = group("rank")

                    currentFameRank = getFameRankByNameOrNull(rank)
                        ?: return ErrorManager.logErrorWithData(
                            FameRankNotFoundException(rank),
                            "FameRank $rank not found",
                            "Rank" to rank,
                            "Lore" to fameRankStack.getLore(),
                            "FameRanks" to FameRanks.fameRanks
                        )

                    continue@line
                }

                fameRankSbMenuPattern.matchMatcher(line) {
                    val rank = group("rank")

                    currentFameRank = getFameRankByNameOrNull(rank)
                        ?: return ErrorManager.logErrorWithData(
                            FameRankNotFoundException(rank),
                            "FameRank $rank not found",
                            "Rank" to rank,
                            "Lore" to fameRankStack.getLore(),
                            "FameRanks" to FameRanks.fameRanks
                        )

                    continue@line
                }
            }
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && profileStorage != null

    class FameRankNotFoundException(rank: String) : Exception("FameRank not found: $rank")
}
