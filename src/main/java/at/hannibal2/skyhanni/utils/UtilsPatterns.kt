package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EnumUtils.enumJoinToPattern
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object UtilsPatterns {

    private val patternGroup = RepoPattern.group("utils")

    /**
     * @regexTest §d§l§ka§r §d§lMYTHIC ACCESSORY §d§l§ka
     * @regexTest §d§l§ka§r §d§lSHINY MYTHIC DUNGEON CHESTPLATE §d§l§ka
     * @regexTest §c§l§ka§r §c§lVERY SPECIAL HATCESSORY §c§l§ka
     * @regexTest §6§lSHINY LEGENDARY DUNGEON BOOTS
     * @regexTest §6§lLEGENDARY DUNGEON BOOTS
     * @regexTest §5§lEPIC BOOTS
     * @regexTest §f§lCOMMON
     * @regexTest §f§lCOMMON COMBAT SHARD §8(ID C9)
     * @regexTest §7Rarity: §6§lLEGENDARY
     * @regexTest §7Rarity: §9§lRARE
     */
    val rarityLoreLinePattern by patternGroup.pattern(
        "item.lore.rarity.line",
        "^(?:§7Rarity: )?(?:§.){2,3}(?:.§. (?:§.){2})?(?:SHINY )?(?<rarity>" +
            enumJoinToPattern<LorenzRarity> { it.name.replace("_", " ") } +
            ") ?(?:DUNGEON )?(?<itemCategory>[^§]*)(?: (?:§.){3}.)?(?: §8\\(ID \\w\\d+\\))?$",
    )

    /**
     * @regexTest §5Abiphone XIII Pro Giga
     */
    val abiPhonePattern by patternGroup.pattern(
        "item.name.abiphone",
        ".{2}Abiphone .*",
    )

    /**
     * @regexTest §fEnchanted Book
     * @regexTest §f§f§fEnchanted Book
     */
    val enchantedBookPattern by patternGroup.pattern(
        "item.name.enchanted.book",
        "(?:§.)+Enchanted Book",
    )

    /**
     * @regexTest Obfuscated
     * @regexTest Hot Bait
     */
    val baitPattern by patternGroup.pattern(
        "item.name.bait",
        "^Obfuscated.*|.* Bait$",
    )

    val enchantmentNamePattern by patternGroup.pattern(
        "item.neuitems.enchantmentname",
        "^(?<format>(?:§.)*)(?<name>[^§]+) (?<level>[IVXL]+)(?: Book)?$",
    )

    /**
     * @regexTest duplex i
     * @regexTest ultimate wise v
     * @regexTest chimera 2
     */
    val cleanEnchantedNamePattern by patternGroup.pattern(
        "item.enchantment.clean.name",
        "(?i)(?<name>.*) (?<level>[IVXL]+|[0-9]+)",
    )

    val potionPattern by patternGroup.pattern(
        "item.name.potion",
        ".*Potion",
    )

    /**
     * @regexTest 8x Enchanted Pork
     * @regexTestWrapped "  §810x §r§bGlacite Jewel"
     */
    val readAmountBeforePattern by patternGroup.pattern(
        "item.amount.front",
        "(?:(?: +§8)?(?:\\+§.)?(?<amount>[\\d.,]+[km]?)x? )?(?<name>.*)",
    )
    val readAmountAfterPattern by patternGroup.pattern(
        "item.amount.behind",
        "(?<name>(?:§.)*(?:[^§] ?)+)(?:§8x(?<amount>[\\d,]+))?",
    )

    /**
     * @regexTest §7Cost
     */
    val costLinePattern by patternGroup.pattern(
        "item.cost.line",
        "(?:§5§o)?§7Cost.*",
    )

    @Suppress("MaxLineLength")
    val timeAmountPattern by patternGroup.pattern(
        "time.amount",
        "(?:(?<y>\\d+) ?y(?:\\w* ?)?)?(?:(?<d>\\d+) ?d(?:\\w* ?)?)?(?:(?<h>\\d+) ?h(?:\\w* ?)?)?(?:(?<m>\\d+) ?m(?:\\w* ?)?)?(?:(?<s>\\d+) ?s(?:\\w* ?)?)?",
    )

    val playerChatPattern by patternGroup.pattern(
        "string.playerchat",
        "(?<important>.*?)(?:§[f7r])*: .*",
    )

    /**
     * @regexTest 8[§r§2164§r§8] §r§7❤ §r§a[VIP§6+§a] Heaven_Reaper§f§r§f: stop
     */
    val chatUsernamePattern by patternGroup.pattern(
        "string.chatusername",
        "^(?:§\\w\\[§\\w\\d+§\\w] )?(?:(?:§\\w)+\\S )?(?<rankedName>(?:§\\w\\[\\w.+] )?(?:§\\w)?(?<username>\\w+))(?: (?:§\\w)?\\[.+?])?",
    )
    val isRomanPattern by RepoPattern.pattern(
        "string.isroman",
        "^M{0,3}(?:CM|CD|D?C{0,3})(?:XC|XL|L?X{0,3})(?:IX|IV|V?I{0,3})",
    )

    /**
     * @regexTest §5Large Enchanted Husbandry Sack
     */
    val sackPattern by patternGroup.pattern(
        "item.sack",
        ".*Sack",
    )

    /**
     * @regexTest §5§kX§5 Rift-Transferable §kX
     */
    val riftTransferablePattern by patternGroup.pattern(
        "item.rift.transferable",
        "§5§kX§5 Rift-Transferable §kX",
    )
    /**
     * @regexTest §5§kX§5 Rift-Exportable §kX
     * @regexTest §5§kX§5 Rift-Exported §kX
     */
    val riftExportablePattern by patternGroup.pattern(
        "item.rift.exportable",
        "§5§kX§5 Rift-Export(?:able|ed) §kX",
    )

    /**
     * @regexTest Late Winter
     * @regexTest Early Spring
     * @regexTest Summer
     */
    val seasonPattern by patternGroup.pattern(
        "skyblocktime.season",
        "(?:Early |Late )?(?<season>Spring|Summer|Autumn|Winter)",
    )

    /**
     * @regexTest §l§r§e§lProfile: §r§aApple §r§7♲
     * @regexTest §l§r§e§lProfile: §r§aNot Allowed To Quit Skyblock Ever Again
     */
    val tabListProfilePattern by patternGroup.pattern(
        "tablist.profile",
        "(?:§.)+Profile: §r§a(?<profile>[\\w\\s]+[^ §]).*",
    )

    /**
     * @regexTest oxsss
     * @regexTest Gillsplash
     */
    val playerNamePattern by patternGroup.pattern(
        "string.playername",
        "[a-zA-Z0-9_]{2,16}",
    )

    val shopOptionsPattern by patternGroup.pattern(
        "inventory.shopoptions",
        "Shop Trading Options",
    )

    val skyblockMenuGuiPattern by patternGroup.pattern(
        "inventory.skyblockmenu",
        "SkyBlock Menu",
    )

    /**
     * @regexTest §7Source: §fVerdant Shard §8(C11)
     * @regexTest §7Source: §9Drowned Shard §8(R18)
     * @regexTest §7Source: §5Barbarian Duke X Shard §8(E27)
     * @regexTest §7Source: §6Galaxy Fish Shard §8(L41)
     * @regexTest §7Source: §6Starborn Shard §8(L44)
     * @regexTest §7Source: §fSea Archer Shard §8(C14)
     */
    val attributeSourcePattern by patternGroup.pattern(
        "attribute.shard.source",
        "§7Source: §.(?<source>.+) Shard §8\\(\\w\\d+\\)"
    )
}
