package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableEnums.Experiment
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils.openInventoryName
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ExperimentationTableAPI {

    fun getCurrentExperiment(): Experiment? {
        val inventory = openInventoryName()
        return if (inventory.startsWith("Superpairs (")) Experiment.entries.find {
            it.nameString == inventory.substringAfter("(").substringBefore(")")
        } else null
    }

    private val patternGroup = RepoPattern.group("enchanting.experiments")

    /**
     * REGEX-TEST: Gained +3 Clicks
     */
    val powerUpPattern by patternGroup.pattern(
        "powerups",
        "Gained \\+\\d Clicks?|Instant Find|\\+\\S* XP",
    )

    /**
     * REGEX-TEST: 123k Enchanting Exp
     * REGEX-TEST: Titanic Experience Bottle
     */
    val rewardPattern by patternGroup.pattern(
        "rewards",
        "\\d{1,3}k Enchanting Exp|Enchanted Book|(?:Titanic |Grand |\\b)Experience Bottle|Metaphysical Serum|Experiment The Fish",
    )

    /**
     * REGEX-TEST: Superpairs (Metaphysical)
     * REGEX-TEST: Chronomatron (Metaphysical)
     */
    val inventoriesPattern by patternGroup.pattern(
        "inventories",
        "(?:Superpairs|Chronomatron|Ultrasequencer) (?:\\(.+\\)|➜ Stakes|Rewards)|Experimentation Table",
    )

    /**
     * REGEX-TEST:  +42,000 Enchanting Exp
     */
    val enchantingExpChatPattern by patternGroup.pattern(
        "chatexp",
        "^ \\+(?<amount>\\d+|\\d+,\\d+)k? Enchanting Exp$",
    )

    /**
     * REGEX-TEST:  +Smite VII
     * REGEX-TEST:  +42,000 Enchanting Exp
     */
    val experimentsDropPattern by patternGroup.pattern(
        "drop",
        "^ \\+(?<reward>.*)\$",
    )

    /**
     * REGEX-TEST: 131k Enchanting Exp
     * REGEX-TEST: 42,000 Enchanting Exp
     */
    val enchantingExpPattern by patternGroup.pattern(
        "exp",
        "(?<amount>\\d+|\\d+,\\d+)k? Enchanting Exp",
    )

    /**
     * REGEX-TEST: Titanic Experience Bottle
     */
    val experienceBottlePattern by patternGroup.pattern(
        "xpbottle",
        "(?:Titanic |Grand |\\b)Experience Bottle",
    )

    /**
     * REGEX-TEST: ☕ You renewed the experiment table! (1/3)
     */
    val experimentRenewPattern by patternGroup.pattern(
        "renew",
        "^☕ You renewed the experiment table! \\((?<current>\\d)/3\\)$",
    )

    /**
     * REGEX-TEST: §d§kXX§5 ULTRA-RARE BOOK! §d§kXX
     */
    val ultraRarePattern by patternGroup.pattern(
        "ultrarare",
        "§d§kXX§5 ULTRA-RARE BOOK! §d§kXX",
    )

    /**
     * REGEX-TEST: §9Smite VII
     */
    val bookPattern by patternGroup.pattern(
        "book",
        "§9(?<enchant>.*)",
    )

    /**
     * REGEX-TEST: §dGuardian
     * REGEX-TEST: §9Guardian§e
     */
    val petNamePattern by patternGroup.pattern(
        "guardianpet",
        "§[956d]Guardian.*",
    )
}
