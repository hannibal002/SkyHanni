package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.chat.PowderMiningFilterConfig.SimplePowderMiningRewardTypes.ASCENSION_ROPE
import at.hannibal2.skyhanni.features.chat.PowderMiningFilterConfig.SimplePowderMiningRewardTypes.JUNGLE_HEART
import at.hannibal2.skyhanni.features.chat.PowderMiningFilterConfig.SimplePowderMiningRewardTypes.OIL_BARREL
import at.hannibal2.skyhanni.features.chat.PowderMiningFilterConfig.SimplePowderMiningRewardTypes.PICKONIMBUS
import at.hannibal2.skyhanni.features.chat.PowderMiningFilterConfig.SimplePowderMiningRewardTypes.PREHISTORIC_EGG
import at.hannibal2.skyhanni.features.chat.PowderMiningFilterConfig.SimplePowderMiningRewardTypes.ROBOT_PARTS
import at.hannibal2.skyhanni.features.chat.PowderMiningFilterConfig.SimplePowderMiningRewardTypes.SLUDGE_JUICE
import at.hannibal2.skyhanni.features.chat.PowderMiningFilterConfig.SimplePowderMiningRewardTypes.TREASURITE
import at.hannibal2.skyhanni.features.chat.PowderMiningFilterConfig.SimplePowderMiningRewardTypes.WISHING_COMPASS
import at.hannibal2.skyhanni.features.chat.PowderMiningFilterConfig.SimplePowderMiningRewardTypes.YOGGIE
import at.hannibal2.skyhanni.features.chat.PowderMiningGemstoneFilterConfig.GemstoneFilterEntry
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object PowderMiningChatFilter {

    private val config get() = SkyHanniMod.feature.chat.filterType.powderMiningFilter
    private val gemstoneConfig get() = config.gemstoneFilterConfig

    val patternGroup = RepoPattern.group("filter.powdermining")

    private var unclosedRewards = false;

    /**
     * REGEX-TEST: §aYou uncovered a treasure chest!
     */
    private val uncoverChestPattern by patternGroup.pattern(
        "warning.chestuncover",
        "§aYou uncovered a treasure chest!"
    )

    /**
     * REGEX-TEST: §cYou need a tool with a §r§aBreaking Power §r§cof §r§66§r§c to mine Ruby Gemstone Block§r§c! Speak to §r§dFragilis §r§cby the entrance to the Crystal Hollows to learn more!
     */
    private val breakingPowerPattern by patternGroup.pattern(
        "warning.breakingpower",
        "§cYou need a tool with a §r§aBreaking Power §r§cof (?:§.)*\\d+§r§c to mine (Ruby|Amethyst|Jade|Amber|Sapphire|Topaz) Gemstone Block§r§c!.+",
    )

    /**
     * REGEX-TEST: §e§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     * REGEX-TEST: §d§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    private val chestWrapperPattern by patternGroup.pattern(
        "powder.chestwrapper",
        "^§[ed]§l▬{64}\$"
    )

    /**
     * REGEX-TEST:   §r§6§lCHEST LOCKPICKED
     */
    private val lockPickedPattern by patternGroup.pattern(
        "powder.picked",
        ".*§r§6§lCHEST LOCKPICKED.*"
    )

    /**
     * REGEX-TEST:   §r§5§lLOOT CHEST COLLECTED
     */
    private val lootChestCollectedPattern by patternGroup.pattern(
        "lootchest.collected",
        ".*§r§5§lLOOT CHEST COLLECTED.*"
    )

    /**
     * REGEX-TEST:   §r§a§lREWARDS
     */
    private val rewardHeaderPattern by patternGroup.pattern(
        "reward.header",
        ".*§r§[af]§lREWARDS.*"
    )

    /**
     * REGEX-TEST: §r§2Mithril Powder §r§8x153
     * REGEX-TEST: §r§dGemstone Powder §r§8x537
     */
    private val powderRewardPattern by patternGroup.pattern(
        "reward.powder",
        "§r§[d2](?:Gemstone|Mithril) Powder( §r§8x(?<amount>[\\d,]+))?",
    )

    /**
     * REGEX-TEST: §r§dGold Essence
     * REGEX-TEST: §r§dGold Essence §r§8x3
     * REGEX-TEST: §r§dDiamond Essence §r§8x2
     * REGEX-TEST: §r§dDiamond Essence
     */
    private val essenceRewardPattern by patternGroup.pattern(
        "reward.essence",
        "§r§d(?:Gold|Diamond) Essence( §r§8x(?<amount>[\\d,]+))?",
    )

    /**
     * REGEX-TEST: §r§9Ascension Rope
     */
    private val ascensionRopeRewardPattern by patternGroup.pattern(
        "reward.ascensionrope",
        "§r§9Ascension Rope( §r§8x(?<amount>[\\d,]+))?",
    )

    /**
     * REGEX-TEST: §r§aWishing Compass
     */
    private val wishingCompassRewardPattern by patternGroup.pattern(
        "reward.wishingcompass",
        "§r§aWishing Compass( §r§8x(?<amount>[\\d,]+))?",
    )

    /**
     * REGEX-TEST: §r§aOil Barrel
     */
    private val oilBarrelRewardPattern by patternGroup.pattern(
        "reward.oilbarrel",
        "§r§aOil Barrel( §r§8x(?<amount>[\\d,]+))?",
    )

    /**
     * REGEX-TEST: §r§fPrehistoric Egg
     */
    private val prehistoricEggPattern by patternGroup.pattern(
        "reward.prehistoricegg",
        "§r§fPrehistoric Egg( §r§8x(?<amount>[\\d,]+))?",
    )

    /**
     * REGEX-TEST: §r§5Pickonimbus 2000
     */
    private val pickonimbusPattern by patternGroup.pattern(
        "reward.pickonimbus",
        "§r§5Pickonimbus 2000( §r§8x(?<amount>[\\d,]+))?",
    )

    /**
     * REGEX-TEST: §r§6Jungle Heart
     */
    private val jungleHeartPattern by patternGroup.pattern(
        "reward.jungleheart",
        "§r§6Jungle Heart( §r§8x(?<amount>[\\d,]+))?",
    )

    /**
     * REGEX-TEST: §r§aSludge Juice
     */
    private val sludgeJuicePattern by patternGroup.pattern(
        "reward.sludgejuice",
        "§r§aSludge Juice( §r§8x(?<amount>[\\d,]+))?",
    )

    /**
     * REGEX-TEST: §r§aYoggie
     */
    private val yoggiePattern by patternGroup.pattern(
        "reward.yoggie",
        "§r§aYoggie( §r§8x(?<amount>[\\d,]+))?",
    )

    /**
     * REGEX-TEST: §r§9FTX 3070
     * REGEX-TEST: §r§9Synthetic Heart
     * REGEX-TEST: §r§9Control Switch
     * REGEX-TEST: §r§9Robotron Reflector
     * REGEX-TEST: §r§9Electron Transmitter
     * REGEX-TEST: §r§9Superlite Motor
     */
    private val robotPartsPattern by patternGroup.pattern(
        "reward.robotparts",
        "§r§9(?:FTX 3070|Synthetic Heart|Control Switch|Robotron Reflector|Electron Transmitter|Superlite Motor)( §r§8x(?<amount>[\\d,]+))?",
    )

    /**
     * REGEX-TEST: §r§5Treasurite
     */
    private val treasuritePattern by patternGroup.pattern(
        "reward.treasurite",
        "§r§5Treasurite( §r§8x(?<amount>[\\d,]+))?",
    )

    /**
     * REGEX-TEST: §r§9§r§cRed Goblin Egg
     * REGEX-TEST: §r§9§r§3Blue Goblin Egg
     * REGEX-TEST: §r§9Goblin Egg
     * REGEX-TEST: §r§9Goblin Egg §r§8x2
     * REGEX-TEST: §r§a§r§aGreen Goblin Egg
     * REGEX-TEST: §r§9§r§eYellow Goblin Egg
     */
    private val goblinEggPattern by patternGroup.pattern(
        "reward.goblineggs",
        "(?:§.)*(?<color>[a-zA-Z]+)? ?Goblin Egg( §r§8x(?<amount>[\\d,]+))?",
    )

    /**
     * REGEX-TEST: §r§f❈ Rough Amethyst Gemstone §r§8x24
     * REGEX-TEST: §r§a❈ Flawed Amethyst Gemstone §r§8x4
     * REGEX-TEST: §r§9⸕ Fine Amber Gemstone
     */
    private val gemstonePattern by patternGroup.pattern(
        "reward.gemstone",
        "§r§[fa9][❤❈☘⸕✎✧] (?<tier>Rough|Flawed|Fine) (?<gem>Ruby|Amethyst|Jade|Amber|Sapphire|Topaz) Gemstone( §r§8x(?<amount>[\\d,]+))?",
    )

    fun block(message: String): String {

        // Generic "you uncovered a chest" message
        if (uncoverChestPattern.matches(message)) return "powder_mining_chest"
        // Breaking power warning
        if (breakingPowerPattern.matches(message) && gemstoneConfig.strongerToolMessages) return "stronger_tool"
        // Closing or opening a reward 'loop' with the spam of ▬
        if (chestWrapperPattern.matches(message)) {
            unclosedRewards = !unclosedRewards
            return "reward_wrapper"
        }

        if (!unclosedRewards) return "no_match"
        if (lockPickedPattern.matches(message)) return "powder_chest_lockpicked"
        if (lootChestCollectedPattern.matches(message)) return "loot_chest_opened"
        if (rewardHeaderPattern.matches((message))) return "powder_reward_header"

        // All powder and loot chest rewards start with 4 spaces
        // To simplify regex statements, this check is done outside
        if (!message.startsWith("    ")) return "no_match"
        val ssMessage = message.substring(4);

        //Powder
        powderRewardPattern.matchMatcher(ssMessage) {
            if (config.powderFilterThreshold == 20000) return "powder_mining_powder"
            val amountStr = groupOrNull("amount") ?: "1"
            if (amountStr.isNotEmpty() && config.powderFilterThreshold > 0) {
                val amountParsed = amountStr.replace(",", "").toInt()
                if (amountParsed < config.powderFilterThreshold) return "powder_mining_powder"
            }
        }

        //Essence
        essenceRewardPattern.matchMatcher(ssMessage) {
            if (config.essenceFilterThreshold == 10) return "powder_mining_essence"
            val amountStr = groupOrNull("amount") ?: "1"
            if (amountStr.isNotEmpty() && config.essenceFilterThreshold > 0) {
                val amountParsed = amountStr.toInt()
                if (amountParsed < config.essenceFilterThreshold) return "powder_mining_essence"
            }
        }

        //All the 'simple' boolean rewards
        val rewardPatterns = mapOf(
            ascensionRopeRewardPattern to ASCENSION_ROPE to "powder_mining_ascension_rope",
            wishingCompassRewardPattern to WISHING_COMPASS to "powder_mining_wishing_compass",
            oilBarrelRewardPattern to OIL_BARREL to "powder_mining_oil_barrel",
            prehistoricEggPattern to PREHISTORIC_EGG to "powder_mining_prehistoric_egg",
            pickonimbusPattern to PICKONIMBUS to "powder_mining_pickonimbus",
            jungleHeartPattern to JUNGLE_HEART to "powder_mining_jungle_heart",
            sludgeJuicePattern to SLUDGE_JUICE to "powder_mining_sludge_juice",
            yoggiePattern to YOGGIE to "powder_mining_yoggie",
            robotPartsPattern to ROBOT_PARTS to "powder_mining_robot_parts",
            treasuritePattern to TREASURITE to "powder_mining_treasurite"
        )
        for ((patternToReward, returnReason) in rewardPatterns) {
            if (patternToReward.first.matches(ssMessage) && config.simplePowderMiningTypes.contains(patternToReward.second)) {
                return returnReason
            }
        }

        //Goblin Eggs
        goblinEggPattern.matchMatcher(ssMessage) {
            if (config.goblinEggs != PowderMiningFilterConfig.GoblinEggFilterEntry.SHOW_ALL) {
                if (config.goblinEggs == PowderMiningFilterConfig.GoblinEggFilterEntry.HIDE_ALL) return "powder_mining_goblin_eggs"

                val colorStr = groupOrNull("color")?.lowercase() ?: ""
                when (colorStr) {
                    //'Colorless', base goblin eggs will never be shown in this code path
                    "" -> return "powder_mining_goblin_eggs"
                    "green" -> return if (config.goblinEggs > PowderMiningFilterConfig.GoblinEggFilterEntry.GREEN_UP) {
                        return "powder_mining_goblin_eggs"
                    } else ""
                    "yellow" -> return if (config.goblinEggs > PowderMiningFilterConfig.GoblinEggFilterEntry.YELLOW_UP) {
                        "powder_mining_goblin_eggs"
                    } else ""
                    "red" -> return if (config.goblinEggs > PowderMiningFilterConfig.GoblinEggFilterEntry.RED_UP) {
                        "powder_mining_goblin_eggs"
                    } else ""
                    // BLUE_ONLY enum not explicitly used in comparison, as the only
                    // case that will block it is HIDE_ALL, which is covered above
                    "blue" -> return ""
                    else -> {
                        ChatUtils.chat(
                            "§cUnknown Goblin Egg color detected in Powder Mining Filter: " +
                                "'${colorStr}' - please report this in the Discord!",
                        )
                        return ""
                    }
                }
            }
        }

        //Gemstones
        gemstonePattern.matchMatcher(ssMessage) {
            val gemStr = groupOrNull("gem")?.lowercase() ?: ""
            val tierStr = groupOrNull("tier")?.lowercase() ?: ""

            //Theoretically impossible but ?
            if (gemStr.isEmpty() || tierStr.isEmpty()) return ""

            val gemSpecificConfig = when (gemStr) {
                "ruby" -> gemstoneConfig.rubyGemstones
                "sapphire" -> gemstoneConfig.sapphireGemstones
                "amber" -> gemstoneConfig.amberGemstones
                "amethyst" -> gemstoneConfig.amethystGemstones
                "jade" -> gemstoneConfig.jadeGemstones
                "topaz" -> gemstoneConfig.topazGemstones
                //Failure case that should never be reached
                else -> return ""
            }

            if (gemSpecificConfig == GemstoneFilterEntry.HIDE_ALL) return "powder_mining_gemstones"

            when (tierStr) {
                // Never allowed through, except for in SHOW_ALL,
                // which is handled above
                "rough" -> return "powder_mining_gemstones"
                "flawed" -> return if (gemSpecificConfig > GemstoneFilterEntry.FLAWED_UP) {
                    "powder_mining_gemstones"
                } else ""
                // FINE_ONLY enum not explicitly used in comparison, as the only
                // case that will block it is HIDE_ALL, which is covered above
                "fine" -> return ""
                // This should not be reachable
                else -> return ""
            }
        }

        //Fallback default
        return "no_match"
    }
}
