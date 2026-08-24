package at.hannibal2.skyhanni.data.effect

import at.hannibal2.skyhanni.events.effects.EffectDurationChangeType
import at.hannibal2.skyhanni.utils.repopatterns.NullableRepoPatternDelegate
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import org.intellij.lang.annotations.Language
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

enum class NonGodPotEffect(
    @param:Language("RegExp")
    private val tabListName: String,
    @param:Language("RegExp")
    private val inventoryItemName: String = ".*$tabListName.*",
    @param:Language("RegExp")
    private val effectGainedMessage: String? = null,
    @param:Language("RegExp")
    private val effectRemovedMessage: String? = null,
    val displayName: String,
    val isMixin: Boolean = false,
    val effectDuration: Duration? = null,
    val effectChangeType: EffectDurationChangeType? = null,
) {
    SMOLDERING(
        "Smoldering Polarization I",
        displayName = "§aSmoldering Polarization I",
        effectGainedMessage = "You ate a Re-heated Gummy Polar Bear!",
        effectDuration = 1.hours,
        effectChangeType = EffectDurationChangeType.ADD,
    ),
    GLOWY(
        "Mushed Glowy Tonic I",
        displayName = "§2Mushed Glowy Tonic I",
        effectGainedMessage = "BUFF! You have gained Mushed Glowy Tonic I!",
        effectDuration = 1.hours,
        effectChangeType = EffectDurationChangeType.SET,
    ),
    WISP(
        "Wisp's Ice-Flavored Water I",
        displayName = "§bWisp's Ice-Flavored Water I",
        effectGainedMessage = "BUFF! You splashed yourself with Wisp's Ice-Flavored Water I!",
        effectDuration = 5.minutes,
        effectChangeType = EffectDurationChangeType.SET,
    ),
    GOBLIN(
        "King's Scent I",
        displayName = "§2King's Scent I",
        effectGainedMessage = "[NPC] King Yolkar: This egg will help me stomach my pain.",
        effectRemovedMessage = "The Goblin King's foul stench has dissipated!",
        effectDuration = 20.minutes,
        effectChangeType = EffectDurationChangeType.SET,
    ),

    INVISIBILITY(
        "Invisibility I",
        displayName = "§8Invisibility I",
    ), // when wearing sorrow armor

    REV(
        "Zombie Brain Mixin",
        isMixin = true,
        displayName = "§cZombie Brain Mixin",
    ),
    TARA(
        "Spider Egg Mixin",
        isMixin = true,
        displayName = "§6Spider Egg Mixin",
    ),
    SVEN(
        "Wolf Fur Mixin",
        isMixin = true,
        displayName = "§bWolf Fur Mixin",
    ),
    VOID(
        "End Portal Fumes",
        isMixin = true,
        displayName = "§6End Portal Fumes",
    ),
    BLAZE(
        "Gabagoey",
        isMixin = true,
        displayName = "§fGabagoey",
    ),
    GLOWING_MUSH(
        "Glowing Mush Mixin",
        isMixin = true,
        displayName = "§2Glowing Mush Mixin",
    ),
    HOT_CHOCOLATE(
        "Hot Chocolate Mixin I",
        isMixin = true,
        displayName = "§6Hot Chocolate Mixin I",
    ),
    MASON_JAR(
        "Celestial Mason Jar I",
        isMixin = true,
        displayName = "§dCelestial Mason Jar Mixin",
    ),
    HOTSPOT_TONIC(
        "Hotspot Tonic",
        isMixin = true,
        displayName = "§2Hotspot Tonic Mixin",
    ),
    MELON_JUICE(
        "Melon Juice Mixin I",
        isMixin = true,
        displayName = "§fMelon Juice Mixin",
    ),

    DEEP_TERROR(
        "Deepterror",
        isMixin = true,
        displayName = "§4Deepterror",
    ),

    GREAT_SPOOK(
        "Great Spook I",
        inventoryItemName = ".*Great Spook Potion.*",
        displayName = "Great Spook I",
        effectGainedMessage = "You consumed a Great Spook Potion!",
        effectDuration = 24.hours,
        effectChangeType = EffectDurationChangeType.SET,
    ),

    DOUCE_PLUIE_DE_STINKY_CHEESE(
        "Douce Pluie de Stinky Cheese I",
        displayName = "§eDouce Pluie de Stinky Cheese I",
        effectGainedMessage = "BUFF! You have gained Douce Pluie de Stinky Cheese I!",
        effectDuration = 1.hours,
        effectChangeType = EffectDurationChangeType.SET,
    ),

    HARVEST_HARBINGER(
        "Harvest Harbinger V",
        displayName = "§6Harvest Harbinger V",
        effectGainedMessage = "BUFF! You have gained Harvest Harbinger V!",
        effectDuration = 25.minutes,
        effectChangeType = EffectDurationChangeType.SET,
    ),

    PEST_REPELLENT(
        "Pest Repellent I",
        displayName = "§6Pest Repellent I§r",
        effectGainedMessage = "YUM! [\uE07F\uE018] Pests will now spawn 2x less while you break crops for the next 60m!",
        effectDuration = 1.hours,
        effectChangeType = EffectDurationChangeType.SET,
    ),
    PEST_REPELLENT_MAX(
        "Pest Repellent II",
        displayName = "§6Pest Repellent II",
        effectGainedMessage = "YUM! [\uE07F\uE018] Pests will now spawn 4x less while you break crops for the next 60m!",
        effectDuration = 1.hours,
        effectChangeType = EffectDurationChangeType.SET,
    ),

    CURSE_OF_GREED(
        "Curse of Greed I",
        displayName = "§4Curse of Greed I",
    ),

    COLD_RESISTANCE_4(
        "Cold Resistance IV",
        displayName = "§bCold Resistance IV",
    ),

    POWDER_PUMPKIN(
        "Powder Pumpkin I",
        displayName = "§fPowder Pumpkin I",
    ),
    FILET_O_FORTUNE(
        "Filet O' Fortune I",
        displayName = "§fFilet O' Fortune I",
    ),
    CHILLED_PRISTINE_POTATO(
        "Chilled Pristine Potato I",
        displayName = "§fChilled Pristine Potato I",
    ),

    LUSHLILAC_BONBON(
        "Lushlilac Bonbon",
        displayName = "§r§5Lushlilac Bonbon§r§f",
    ),
    PRIME_LUSHLILAC_BONBON(
        "Prime Lushlilac Bonbon",
        displayName = "§r§5Prime Lushlilac Bonbon§r§f",
    ),
    EXALTED_LUSHLILAC_BONBON(
        "Exalted Lushlilac Bonbon",
        displayName = "§r§5Exalted Lushlilac Bonbon§r§f",
    ),
    OCEANDY(
        "Oceandy",
        displayName = "§r§5Oceandy§r§f",
    ),
    CANDYCOMB(
        "Candycomb",
        displayName = "§r§5Candycomb§r§f",
    ),
    ;

    private val patternName = name.lowercase().replace("_", "-")

    val tablistNamePattern by RepoPattern.pattern(
        "misc.nongodpot.effects.tabname-$patternName",
        tabListName,
    )
    val effectGainedPattern by NullableRepoPatternDelegate(
        effectGainedMessage?.let {
            RepoPattern.pattern(
                "misc.nongodpot.effects.gained.$patternName.colorless",
                it
            )
        },
    )

    val effectRemovedPattern by NullableRepoPatternDelegate(
        effectRemovedMessage?.let {
            RepoPattern.pattern(
                "misc.nongodpot.effects.removed.$patternName.colorless",
                it
            )
        },
    )

    val inventoryItemNamePattern by RepoPattern.pattern(
        "misc.nongodpot.effects.inventoryitemname.$patternName",
        inventoryItemName,
    )
}
