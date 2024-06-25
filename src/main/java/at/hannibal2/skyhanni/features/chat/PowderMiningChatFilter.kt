package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.chat.PowderMiningGemstoneFilterConfig.GemstoneFilterEntry
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object PowderMiningChatFilter {

    private val config get() = SkyHanniMod.feature.chat.filterType.powderMiningFilter
    private val gemstoneConfig get() = config.gemstoneFilterConfig

    val patternGroup = RepoPattern.group("filter.powdermining")

    /**
     * REGEX-TEST: §cYou need a tool with a §r§aBreaking Power §r§cof §r§66§r§c to mine Ruby Gemstone Block§r§c! Speak to §r§dFragilis §r§cby the entrance to the Crystal Hollows to learn more!
     */
    private val breakingPowerPattern by patternGroup.pattern(
        "warning.breaking_powder",
        "§cYou need a tool with a §r§aBreaking Power §r§cof (?:§.)*\\d+§r§c to mine (Ruby|Amethyst|Jade|Amber|Sapphire|Topaz) Gemstone Block§r§c! Speak to §r§dFragilis §r§cby the entrance to the Crystal Hollows to learn more!"
    )

    /**
     * REGEX-TEST: §aYou received §r§b+286 §r§aMithril Powder.
     * REGEX-TEST: §aYou received §r§b+1,264 §r§aGemstone Powder.
     */
    private val powderRewardPattern by patternGroup.pattern(
        "reward.powder",
        "§aYou received §r§b\\+(?<amount>[\\d|,]+) §r§a(?:Mithril|Gemstone) Powder."
    )

    /**
     * REGEX-TEST: §aYou received §r§b+4 Diamond Essence§r§a.
     * REGEX-TEST: §aYou received §r§6+3 Gold Essence§r§a.
     */
    private val essenceRewardPattern by patternGroup.pattern(
        "reward.essence",
        "§aYou received §r(?:§6|§b)\\+(?<amount>\\d+) (?:Gold|Diamond) Essence§r§a."
    )

    /**
     * REGEX-TEST: §aYou received §r§f2 §r§9Ascension Rope§r§a.
     */
    private val ascensionRopeRewardPattern by patternGroup.pattern(
        "reward.ascensionrope",
        "§aYou received §r§f\\d+ §r§9Ascension Rope§r§a"
    )

    /**
     * REGEX-TEST: §aYou received §r§f3 §r§aWishing Compass§r§a.
     */
    private val wishingCompassRewardPattern by patternGroup.pattern(
        "reward.wishingcompass",
        "§aYou received §r§f\\d+ §r§aWishing Compass§r§a."
    )

    /**
     * REGEX-TEST: §aYou received §r§f2 §r§aOil Barrel§r§a.
     */
    private val oilBarrelRewardPattern by patternGroup.pattern(
        "reward.oilbarrel",
        "§aYou received §r§f\\d+ §r§aOil Barrel§r§a."
    )

    /**
     * REGEX-TEST: §aYou received §r§f1 §r§fPrehistoric Egg§§a.
     */
    private val prehistoricEggPattern by patternGroup.pattern(
        "reward.prehistoricegg",
        "§aYou received §r§f\\d+ §r§fPrehistoric Egg§r§a."
    )

    /**
     * REGEX-TEST: §aYou received §r§f1 §r§5Pickonimbus 2000§r§a.
     */
    private val pickonimbusPattern by patternGroup.pattern(
        "reward.pickonimbus",
        "§aYou received §r§f\\d+ §r§5Pickonimbus 2000§r§a"
    )

    /**
     * REGEX-TEST: §aYou received §r§f1 §r§6Jungle Heart§r§a.
     */
    private val jungleHeartPattern by patternGroup.pattern(
        "reward.jungleheart",
        "§aYou received §r§f\\d+ §r§6Jungle Heart§r§a."
    )

    /**
     * REGEX-TEST: §aYou received §r§f10 §r§aSludge Juice§r§a.
     */
    private val sludgeJuicePattern by patternGroup.pattern(
        "reward.sludgejuice",
        "§aYou received §r§f\\d+ §r§aSludge Juice§r§a."
    )

    // Why is the Yoggie message missing an §r before the §a.?
    // The world may never know.
    /**
     * REGEX-TEST: §aYou received §r§f1 §r§bYoggie§a.
     */
    private val yoggiePattern by patternGroup.pattern(
        "reward.yoggie",
        "§aYou received §r§f\\d+ §r§bYoggie§a."
    )

    /**
     * REGEX-TEST: §aYou received §r§f1 §r§9FTX 3070§r§a.
     * REGEX-TEST: §aYou received §r§f1 §r§9Synthetic Heart§r§a.
     * REGEX-TEST: §aYou received §r§f1 §r§9Control Switch§r§a.
     * REGEX-TEST: §aYou received §r§f1 §r§9Robotron Reflector§r§a.
     * REGEX-TEST: §aYou received §r§f1 §r§9Electron Transmitter§r§a.
     * REGEX-TEST: §aYou received §r§f1 §r§9Superlite Motor§r§a.
     */
    private val robotPartsPattern by patternGroup.pattern(
        "reward.robotparts",
        "§aYou received §r§f\\d+ §r§9(?:FTX 3070|Synthetic Heart|Control Switch|Robotron Reflector|Electron Transmitter|Superlite Motor)§r§a."
    )

    /**
     * REGEX-TEST: §aYou received §r§f1 §r§5Treasurite§r§a.
     */
    private val treasuritePattern by patternGroup.pattern(
        "reward.treasurite",
        "§aYou received §r§f\\d+ §r§5Treasurite§r§a."
    )

    /**
     * REGEX-TEST: §aYou received §r§f1 §r§9§r§cRed Goblin Egg§r§a.
     * REGEX-TEST: §aYou received §r§f1 §r§9§r§3Blue Goblin Egg§r§a.
     * REGEX-TEST: §aYou received §r§f1 §r§9Goblin Egg§r§a.
     * REGEX-TEST: §aYou received §r§f1 §r§a§r§aGreen Goblin Egg§r§a.
     * REGEX-TEST: §aYou received §r§f1 §r§9§r§eYellow Goblin Egg§r§a.
     */
    private val goblinEggPattern by patternGroup.pattern(
        "reward.goblineggs",
        "§aYou received §r§f\\d+ (?:§.)*(?<color>[a-zA-Z]+)? ?Goblin Egg§r§a."
    )

    /**
     * REGEX-TEST: §aYou received §r§f24 §r§f❈ Rough Amethyst Gemstone§r§a.
     * REGEX-TEST: §aYou received §r§f4 §r§a❈ Flawed Amethyst Gemstone§r§a.
     * REGEX-TEST: §aYou received §r§f1 §r§9⸕ Fine Amber Gemstone§r§a.
     */
    private val gemstonePattern by patternGroup.pattern(
        "reward.gemstone",
        "§aYou received §r§f(?<amount>[\\d,]+) §r§[fa9][❤❈☘⸕✎✧] (?<tier>Rough|Flawed|Fine) (?<gem>Ruby|Amethyst|Jade|Amber|Sapphire|Topaz) Gemstone§r§a\\."
    )

    fun block(message: String): String {

        //Breaking power warning
        breakingPowerPattern.matchMatcher(message) {
            if (gemstoneConfig.strongerToolMessages) return "stronger_tool"
        }
        
        //Powder
        powderRewardPattern.matchMatcher(message) {
            if (config.powderFilterThreshold == 20000) return "powder_mining_powder"
            val amountStr = groupOrNull("amount") ?: ""
            if (amountStr.isNotEmpty() && config.powderFilterThreshold > 0) {
                val amountParsed = amountStr.replace(",", "").toInt()
                if (amountParsed < config.powderFilterThreshold) return "powder_mining_powder"
            }
        }

        //Essence
        essenceRewardPattern.matchMatcher(message) {
            if (config.essenceFilterThreshold == 10) return "powder_mining_essence"
            val amountStr = groupOrNull("amount") ?: ""
            if (amountStr.isNotEmpty() && config.essenceFilterThreshold > 0) {
                val amountParsed = amountStr.toInt()
                if (amountParsed < config.essenceFilterThreshold) return "powder_mining_essence"
            }
        }

        //Ascension Rope
        ascensionRopeRewardPattern.matchMatcher(message) {
            if (config.ascensionRope) return "powder_mining_ascension_rope"
        }

        //Wishing Compass
        wishingCompassRewardPattern.matchMatcher(message) {
            if (config.wishingCompass) return "powder_mining_wishing_compass"
        }

        //Oil Barrel
        oilBarrelRewardPattern.matchMatcher(message) {
            if (config.oilBarrel) return "powder_mining_oil_barrel"
        }

        //Prehistoric Egg
        prehistoricEggPattern.matchMatcher(message) {
            if (config.prehistoricEgg) return "powder_mining_prehistoric_egg"
        }

        //Pickonimbus 2000
        pickonimbusPattern.matchMatcher(message) {
            if (config.pickonimbus) return "powder_mining_pickonimbus"
        }

        //Jungle Heart
        jungleHeartPattern.matchMatcher(message) {
            if (config.jungleHeart) return "powder_mining_jungle_heart"
        }

        //Sludge Juice
        sludgeJuicePattern.matchMatcher(message) {
            if (config.sludgeJuice) return "powder_mining_sludge_juice"
        }

        //Yoggie
        yoggiePattern.matchMatcher(message) {
            if (config.yoggie) return "powder_mining_yoggie"
        }

        //Robot Parts
        robotPartsPattern.matchMatcher(message) {
            if (config.robotParts) return "powder_mining_robot_parts"
        }

        //Treasurite
        treasuritePattern.matchMatcher(message) {
            if (config.treasurite) return "powder_mining_treasurite"
        }

        //Goblin Eggs
        goblinEggPattern.matchMatcher(message) {
            if (config.goblinEggs != PowderMiningFilterConfig.GoblinEggFilterEntry.SHOW_ALL) {
                if (config.goblinEggs == PowderMiningFilterConfig.GoblinEggFilterEntry.HIDE_ALL) return "powder_mining_goblin_eggs"

                val colorStr = groupOrNull("color")?.lowercase() ?: ""
                when(colorStr) {
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
                        ChatUtils.chat("§cUnknown Goblin Egg color detected in Powder Mining Filter: '${colorStr}' - please report this in the Discord!")
                        return ""
                    }
                }
            }
        }

        //Gemstones
        gemstonePattern.matchMatcher(message) {
            val gemStr = groupOrNull("gem")?.lowercase() ?: ""
            val tierStr = groupOrNull("tier")?.lowercase() ?: ""

            //Theoretically impossible but ?
            if (gemStr.isEmpty() || tierStr.isEmpty()) return ""

            val gemSpecificConfig = when(gemStr) {
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

            when(tierStr) {
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
