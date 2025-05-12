package at.hannibal2.skyhanni.features.commands.cooldown

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object AuctionBazaarCooldown {

    private val patternGroup = RepoPattern.group("misc.commands.auction-bazaar")

    /**
     * REGEX-TEST: /ah
     * REGEX-TEST: /ahs test
     * REGEX-TEST: /auction
     * REGEX-TEST: /auctions
     * REGEX-TEST: /ah oBlazin
     */
    private val auctionHouseCommands by patternGroup.pattern(
        "auction-house",
        "\\/(?<command>ahs?|auctions?)(?: (?<searchTerm>.*))?"
    )

    /**
     * REGEX-TEST: /bz
     * REGEX-TEST: /baz
     * REGEX-TEST: /bazaar
     * REGEX-TEST: /bz coal
     * REGEX-TEST: /bazaar wheat
     */
    private val bazaarCommands by patternGroup.pattern(
        "bazaar",
        "\\/ba?z(?:aar)?(?: (?<product>.*))?"
    )

    private val config get() = SkyHanniMod.feature.misc.commands
    private var lastRunCompleted: SimpleTimeMark = SimpleTimeMark.farPast()
    private var action: (() -> Unit)? = null

    @HandleEvent
    fun onWorldChange() {
        if (!config.auctionBazaarCooldown || lastRunCompleted.isInFuture()) return
        lastRunCompleted = DelayedRun.runDelayed(4.seconds) {
            action?.invoke()
            action = null
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onCommand(event: MessageSendToServerEvent) {
        if (!config.auctionBazaarCooldown || lastRunCompleted.isInPast()) return
        auctionHouseCommands.matchMatcher(event.message) {
            val searchTerm = groupOrNull("searchTerm")
            action = when (searchTerm) {
                null -> HypixelCommands::ah
                else -> when (group("command")) {
                    // AHS is the only exception to the rule of 'look up the player'
                    "ahs" -> { -> HypixelCommands.auctionSearch(searchTerm) }
                    else -> { -> HypixelCommands.ah(searchTerm) }
                }
            }
        }

        bazaarCommands.matchMatcher(event.message) {
            val product = groupOrNull("product")
            action = when (product) {
                null -> HypixelCommands::bz
                else -> { -> HypixelCommands.bazaar(product) }
            }
        }

        if (action != null) event.cancel()
    }

}
