package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.ScoreboardChangeEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

enum class Fame(
    val rank: String,
    val fameRequired: Long,
    val bitsMultiplier: Double,
    val electionVotes: Int,
) {
    NEW_PLAYER("New Player", 0, 1.0, 1),
    SETTLER("Settler", 20_000, 1.1, 1),
    CITIZEN("Citizen", 80_000, 1.2, 2),
    CONTRIBUTOR("Contributor", 200_000, 1.3, 3),
    PHILANTHROPIST("Philanthropist", 40_0000, 1.4, 5),
    PATRON("Patron", 800_000, 1.6, 10),
    FAMOUS_PLAYER("Famous Player", 1_500_000, 1.8, 20),
    ATTACHE("Attaché", 3_000_000, 1.9, 25),
    AMBASSADOR("Ambassador", 10_000_000, 2.0, 50),
    STATESPERSON("Statesperson", 20_000_000, 2.04, 75),
    SENATOR("Senator", 33_000_000, 2.08, 100),
    DIGNITARY("Dignitary", 50_000_000, 2.12, 100),
    COUNCILOR("Councilor", 72_000_000, 2.16, 100),
    MINISTER("Minister", 100_000_000, 2.2, 100),
    PREMIER("Premier", 135_000_000, 2.22, 100),
    CHANCELLOR("Chancellor", 178_000_000, 2.24, 100),
    SUPREME("Supreme", 230_000_000, 2.26, 100)
}


object BitsAPI {
    var bits = 0
    var currentFameRank = Fame.NEW_PLAYER
    var bitsToClaim = 0

    private const val defaultcookiebits = 4800

    private val group = RepoPattern.group("data.BitsAPI")
    private val bitsScoreboardPattern by group.pattern("scoreboard.bits", "Bits: §b(?<amount>.*) §3\\(\\+(?<earned>.*)\\)")
    private val bitsFromFameRankUpChatPattern by group.pattern("chat.bitsFromFameRankup", "§eYou gained §3(?<amount>.*) Bits Available §ecompounded from all your §epreviously eaten §6cookies§e! Click here to open §6cookie menu§e!")
    private val bitsEarnedChatPattern by group.pattern("chat.earned", "§f\\s+§8\\+§b(?<amount>.*)\\s+Bits\n")
    private val boosterCookieAte by group.pattern("chat.boosterCookieAte", "§eYou consumed a §6Booster Cookie§e! §d.+")
    private val bitsAvailableMenu by group.pattern("gui.bitsAvailableMenu", "§7Bits Available: §b(?<toClaim>[\\w,]+)(§3.+)?")
    private val fameRankSbmenu by group.pattern("gui.FameRankSbmenu", "§7Your rank: §e(?<rank>.*)")
    private val fameRankCommunityShop by group.pattern("gui.FameRankCommunityShop", "§7Fame Rank: §e(?<rank>.*)")

    @SubscribeEvent
    fun onScoreboardChange(event: ScoreboardChangeEvent){
        for (line in event.newList) {
            val message = line.trimWhiteSpaceAndResets().removeResets()

            bitsScoreboardPattern.matchMatcher(message) {
                val amount = group("amount").formatNumber().toInt()
                val earned = group("earned").formatNumber().toInt()
                bits = amount
                bitsToClaim -= earned

                save()
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val message = event.message.trimWhiteSpaceAndResets().removeResets()

        bitsFromFameRankUpChatPattern.matchMatcher(message) {
            val amount = group("amount").formatNumber().toInt()
            bitsToClaim += amount

            save()
        }

        bitsEarnedChatPattern.matchMatcher(message) {
            // Only two locations where the bits line isn't shown, but you can still get bits
            if (!listOf(IslandType.CATACOMBS, IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)) return

            val amount = group("amount").formatNumber().toInt()
            bits += amount
            bitsToClaim -= amount

            LorenzUtils.chat("§a +$amount bits, to claim: $bitsToClaim")

            save()
        }

        boosterCookieAte.matchMatcher(message) {
            bitsToClaim += (defaultcookiebits * currentFameRank.bitsMultiplier).toInt()

            LorenzUtils.chat("§a +${(defaultcookiebits * currentFameRank.bitsMultiplier).toInt()} bits to claim")

            save()
        }
    }

    @SubscribeEvent
    fun onInventoryFullyLoaded(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val allowedNames = listOf("SkyBlock Menu", "Booster Cookie", "Community Shop")
        if (!allowedNames.contains(event.inventoryName)) return

        val stacks = event.inventoryItems
        for (stack in stacks.values) {
            val lore = stack.getLore()
            if (lore.isEmpty()) continue

            for (line in lore) {
                bitsAvailableMenu.matchMatcher(line) {
                    val toClaim = group("toClaim").formatNumber().toInt()
                    bitsToClaim = toClaim

                    save()
                }

                fameRankSbmenu.matchMatcher(line) {
                    val rank = group("rank")
                    currentFameRank = Fame.entries.firstOrNull { it.rank == rank } ?: Fame.NEW_PLAYER

                    save()
                }

                fameRankCommunityShop.matchMatcher(line) {
                    val rank = group("rank")
                    currentFameRank = Fame.entries.firstOrNull { it.rank == rank } ?: Fame.NEW_PLAYER

                    save()
                }
            }
        }
    }


    // Handle Storage data
    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val config = ProfileStorageData.profileSpecific ?: return
        bits = config.bits.bits
        currentFameRank = config.bits.currentFameRank
        bitsToClaim = config.bits.bitsToClaim
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        val config = ProfileStorageData.profileSpecific ?: return
        config.bits.bits = bits
        config.bits.currentFameRank = currentFameRank
        config.bits.bitsToClaim = bitsToClaim
    }

    private fun save() {
        val config = ProfileStorageData.profileSpecific ?: return
        config.bits.bits = bits
        config.bits.currentFameRank = currentFameRank
        config.bits.bitsToClaim = bitsToClaim
    }
}
