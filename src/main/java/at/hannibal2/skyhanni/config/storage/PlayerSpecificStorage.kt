package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.features.bingo.card.goals.BingoGoal
import at.hannibal2.skyhanni.features.chat.CurrentChatDisplay
import at.hannibal2.skyhanni.features.combat.damageindicator.BossType
import at.hannibal2.skyhanni.features.fame.UpgradeReminder.CommunityShopUpgrade
import at.hannibal2.skyhanni.features.misc.UserLuckBreakdown
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.farPast
import at.hannibal2.skyhanni.utils.SkyblockCurrency
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import com.google.gson.annotations.Expose
import java.time.LocalDate
import kotlin.time.Duration

class PlayerSpecificStorage {
    @Expose
    val profiles: MutableMap<String, ProfileSpecificStorage> = mutableMapOf() // profile name

    @Expose
    var multipleProfiles: Boolean = false

    @Expose
    var useRomanNumerals: Boolean = true

    @Expose
    var gardenCommunityUpgrade: Int = -1

    @Expose
    var fameRank: String = "New Player"

    @Expose
    var currentChat: CurrentChatDisplay.ChatType? = null

    @Expose
    var nextCityProjectParticipationTime: SimpleTimeMark = farPast()

    @Expose
    var communityShopAccountUpgrade: CommunityShopUpgrade? = null

    @Expose
    val guildMembers: MutableList<String> = mutableListOf()

    /** Written and read by [at.hannibal2.skyhanni.data.CurrencyApi], for currencies shared by all profiles. */
    @Expose
    val currencies: MutableMap<SkyblockCurrency, Long> = enumMapOf()

    @Expose
    var bazaar: BazaarStorage = BazaarStorage()

    class BazaarStorage {
        @Expose
        var taxRate: Double = 1.25

        @Expose
        var coinsTowardsLimit: Double = 0.0

        @Expose
        var lastAccessedDay: LocalDate? = null
    }

    @Expose
    var winter: WinterStorage = WinterStorage()

    class WinterStorage {
        @Expose
        val playersThatHaveBeenGifted: MutableSet<String> = mutableSetOf()

        @Expose
        var amountGifted: Int = 0

        @Expose
        var cakeCollectedYear: Int = 0
    }

    @Expose
    val bingoSessions: MutableMap<Long, BingoSession> = mutableMapOf()

    class BingoSession {
        @Expose
        val tierOneMinionsDone: MutableSet<NeuInternalName> = mutableSetOf()

        @Expose
        val goals: MutableMap<Int, BingoGoal> = mutableMapOf()
    }

    @Expose
    var limbo: LimboStats = LimboStats()

    class LimboStats {
        @Expose
        var playtime: Int = 0

        @Expose
        var personalBest: Int = 0

        /**
         * Do NOT use if you are trying to get the players total user luck
         *
         * @see UserLuckBreakdown.getTotalUserLuck
         */
        @Expose
        var userLuck: Float = 0f
    }

    @Expose
    val slayerPersonalBests: MutableMap<BossType, Duration> = mutableMapOf()
}
