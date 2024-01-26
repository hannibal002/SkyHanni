package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.LorenzUtils.enumJoinToPattern
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object UtilsPatterns {

    private val patternGroup = RepoPattern.group("utils")

    /** Examples:
    §d§l§ka§r §d§l§d§lMYTHIC ACCESSORY §d§l§ka
    §d§l§ka§r §d§l§d§lSHINY MYTHIC DUNGEON CHESTPLATE §d§l§ka
    §c§l§ka§r §c§l§c§lVERY SPECIAL HATCESSORY §c§l§ka
    §6§lSHINY LEGENDARY DUNGEON BOOTS
    §6§lLEGENDARY DUNGEON BOOTS
    §5§lEPIC BOOTS
    §f§lCOMMON
     **/
    val rarityLoreLinePattern by patternGroup.pattern(
        "item.lore.rarity.line",
        "^(?:§.){2,3}(?:.§. (?:§.){4})?(?:SHINY )?(?<rarity>${enumJoinToPattern<LorenzRarity> { it.name.replace("_", " ") }}) ?(?:DUNGEON )?(?<itemCategory>[^§]*)(?: (?:§.){3}.)?$"
    )

    val abiPhonePattern by patternGroup.pattern(
        "item.name.abiphone",
        ".{2}Abiphone .*"
    )

    val enchantedBookPattern by patternGroup.pattern(
        "item.name.enchanted.book",
        ".{2}?Enchanted Book"
    )

    val potionPattern by patternGroup.pattern(
        "item.name.potion",
        ".*Potion"
    )
    val petLevelPattern by patternGroup.pattern(
        "item.petlevel",
        "(?:§f§f)?§7\\[Lvl (?<level>\\d+)] .*"
    )

    val seasonPattern by RepoPattern.pattern(
        "skyblocktime.season",
        "(?:Early |Late )?(?<season>Spring|Summer|Autumn|Winter)"
    )
}
