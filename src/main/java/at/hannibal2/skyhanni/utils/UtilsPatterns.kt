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
        "^(?:§.){2,3}(?:.§. (?:§.){4})?(?:SHINY )?(?<rarity>" +
            enumJoinToPattern<LorenzRarity> { it.name.replace("_", " ") } +
            ") ?(?:DUNGEON )?(?<itemCategory>[^§]*)(?: (?:§.){3}.)?$"
    )

    val abiPhonePattern by patternGroup.pattern(
        "item.name.abiphone",
        ".{2}Abiphone .*"
    )

    val enchantedBookPattern by patternGroup.pattern(
        "item.name.enchanted.book",
        ".{2}?Enchanted Book"
    )
    val enchantmentNamePattern by patternGroup.pattern(
        "item.neuitems.enchantmentname",
        "^(?<format>(?:§.)+)(?<name>[^§]+) (?<level>[IVXL]+)$"
    )

    val potionPattern by patternGroup.pattern(
        "item.name.potion",
        ".*Potion"
    )
    val readAmountBeforePattern by patternGroup.pattern(
        "item.amount.front",
        "(?: *§8(\\+§\\w)?(?<amount>[\\d.km,]+)(x )?)?(?<name>.*)"
    )
    val readAmountAfterPattern by patternGroup.pattern(
        "item.amount.behind",
        "(?<name>(?:['\\w-]+ ?)+)(?:§8x(?<amount>[\\d,]+))?"
    )

    val timeAmountPattern by patternGroup.pattern(
        "time.amount",
        "(?:(?<y>\\d+) ?y(?:\\w* ?)?)?(?:(?<d>\\d+) ?d(?:\\w* ?)?)?(?:(?<h>\\d+) ?h(?:\\w* ?)?)?(?:(?<m>\\d+) ?m(?:\\w* ?)?)?(?:(?<s>\\d+) ?s(?:\\w* ?)?)?"
    )

    val playerChatPattern by patternGroup.pattern(
        "string.playerchat",
        "(?<important>.*?)(?:§[f7r])*: .*"
    )
    val chatUsernamePattern by patternGroup.pattern(
        "string.chatusername",
        "^(?:§\\w\\[§\\w\\d+§\\w] )?(?:(?:§\\w)+\\S )?(?<rankedName>(?:§\\w\\[\\w.+] )?(?:§\\w)?(?<username>\\w+))(?: (?:§\\w)?\\[.+?])?"
    )
    val isRomanPattern by RepoPattern.pattern(
        "string.isroman",
        "^M{0,3}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})"
    )

    val sackPattern by patternGroup.pattern(
        "item.sack",
        ".*Sack"
    )

    val seasonPattern by patternGroup.pattern(
        "skyblocktime.season",
        "(?:Early |Late )?(?<season>Spring|Summer|Autumn|Winter)"
    )

    val tabListProfilePattern by patternGroup.pattern(
        "tablist.profile",
        "§.§lProfile: §r§a(?<profile>.*)"
    )
}
