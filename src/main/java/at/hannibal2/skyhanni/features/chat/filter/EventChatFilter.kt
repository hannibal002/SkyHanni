package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gifting.GiftProfitTracker

object EventChatFilter : ChatFilterGroup {
    private val patternGroup = ChatFilterManager.chatFilterGroup.group("event")
    private val config get() = ChatFilterManager.config

    val winterDetector = IslandDetector(IslandType.WINTER,)

    override val filters: Set<ChatFilter> = setOf(
        GuildEventExpFilter,
        WinterIslandFilter,
        FireSaleFilter,
        EventLevelUpFilter,
        ChocolateFactoryUpgradeFilter,
        HoppityBeginFilter,
        HoppityEggAppearFilter,
        WinterGiftFilter,
    )

    object GuildEventExpFilter : RegexChatFilter("guild_event_exp", config.guildEventExp) {
        /**
         * REGEX-TEST: You earned 2 GEXP from playing SkyBlock!
         * REGEX-TEST: You earned 2 GEXP + 210 Event EXP from playing SkyBlock!
         * REGEX-TEST: You earned 10 Event EXP from playing SkyBlock!
         */
        override val patterns by patternGroup.list(
            "guild-event",
            "You earned (?:[\\d,]+ GEXP(?: \\+ [\\d,]+ Event EXP)?|[\\d,]+ Event EXP) from playing SkyBlock!",
        )
    }


    object WinterIslandFilter : RegexIslandChatFilter("winter_island", config.others, winterDetector) {
        /**
         * REGEX-TEST: ☃ [VIP+] liron150 mounted a Snow Cannon!
         */
        override val patterns by patternGroup.list(
            "winter-island",
            "☃ .* mounted a Snow Cannon!",
        )
    }

    object FireSaleFilter : RegexChatFilter("firesale", config.fireSale) {
        override val patterns by patternGroup.list(
            "firesale",
            "A FIRE SALE A[\\n.]*",
            "♨ Fire Sales for .* are starting soon!",
            "\\s*♨ .* (?:Skin|Rune|Dye) (?:for a limited time )?\\(.* left\\)(?:|!)",
            "♨ Visit the Community Shop in the next .* to grab yours! \\[WARP]",
            "♨ A Fire Sale for .* is starting soon!",
            "♨ Fire Sales? for .* ended!",
            " {3}♨ And \\d+ more!",
            "A FIRE SALE A",
            "♨ Selling multiple items for a limited time!",
        )
    }

    object EventLevelUpFilter : RegexChatFilter("event_levelup", config.eventLevelUp) {
        // TODO need proper solution to hide empty messages in event text
        override val patterns by patternGroup.list(
            "event-levelup",
            " +You are now Event Level *!",
            " +You earned * Event Silver!",
            " +# LEVEL UP! #",
            "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
        )
    }

    object ChocolateFactoryUpgradeFilter : RegexChatFilter("factory_upgrade", config.factoryUpgrade) {
        override val patterns by patternGroup.list(
            "chocolate-factory-upgrade",
            ".* has been promoted to \\[.*] *!",
            "Your Rabbit Barn capacity has been increased to .* Rabbits!",
            "You will now produce .* Chocolate per click!",
            "You upgraded to .*?!",
        )
    }

    object HoppityBeginFilter : RegexChatFilter("hoppity_begin", config.hoppityBegun) {
        override val patterns by patternGroup.list(
            "hoppity-begin",
            "Hoppity's Hunt has begun! Help Hoppity find his Chocolate Rabbit Eggs across SkyBlock each day during the Spring!",
        )
    }

    object HoppityEggAppearFilter : RegexChatFilter("hoppity_appear", config.hoppityEggs) {
        /**
         * REGEX-TEST: HOPPITY'S HUNT A Chocolate Rabbit Egg has appeared!
         */
        override val patterns by patternGroup.list(
            "hoppity-egg-appear",
            "HOPPITY'S HUNT A .* has appeared!",
        )
    }

    object WinterGiftFilter : RegexChatFilter("winter_gift", config.winterGift) {
        override val patterns = buildList {
            GiftProfitTracker.run {
                listOf(
                    xpGainedPattern,
                    coinsGainedPattern,
                    northStarsPattern,
                    boostPotionPattern,
                    enchantmentBookPattern,
                    genericRewardPattern,
                ).forEach { add(it) }
            }
            addAll(GiftProfitTracker.spamPatterns)
        }
    }
}
