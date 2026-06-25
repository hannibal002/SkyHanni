package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

internal object PetStoragePatterns {

    private val patternGroup = RepoPattern.group("misc.pet.storage")

    /**
     * REGEX-TEST: Pets
     * REGEX-TEST: (1/3) Pets
     * REGEX-TEST: Pets (1/3)
     * REGEX-TEST: (6/6) Pets
     * REGEX-TEST: Pets: "a"
     * REGEX-TEST: Pets: "e" (1/2)
     * REGEX-TEST: (6/6) Pets: "e"
     */
    val mainPetMenuNamePattern by patternGroup.pattern(
        "menu.gui.name",
        "(?:\\(\\d+\\/\\d+\\) )?Pets(?:: \"(?<search>.*)\")?(?: \\(\\d+\\/\\d+\\))? ?",
    )

    /**
     * REGEX-TEST: §7[Lvl 8] §6Squid
     * REGEX-TEST: §7[Lvl 100] §dHermit Crab
     * REGEX-TEST: §7[Lvl 200] §8[§6122§4✦§8] §6Golden Dragon
     */
    @Suppress("MaxLineLength")
    val petMenuPetStackNamePattern by patternGroup.pattern(
        "menu.petstack.name",
        "(?:§.)*\\[Lvl (?<level>[\\d,]+)] (?:(?:§.)+\\[(?:§.)*\\d+(?:§.)*(?<altskin>§.✦)(?:§.)*] )?(?:§.)*§(?<rarity>.)(?<pet>[^§]+?)(?<skin>§. ✦)?",
    )

    /**
     * WRAPPED-REGEX-TEST: " [Lvl 100] Hedgehog"
     * WRAPPED-REGEX-TEST: " [Lvl 68] Blaze"
     * WRAPPED-REGEX-TEST: " [Lvl 51] Kuudra"
     * WRAPPED-REGEX-TEST: " [Lvl 100] Flying Fish"
     * WRAPPED-REGEX-TEST: " [Lvl 100] Chicken ✦"
     * WRAPPED-REGEX-TEST: " [Lvl 200] [122✦] Golden Dragon"
     * WRAPPED-REGEX-FAIL: " No pet selected"
     */
    @Suppress("MaxLineLength")
    val petTabWidgetNamePattern by patternGroup.pattern(
        "tab.name",
        " \\[Lvl (?<level>[\\d,]+)] (?:\\[\\d+(?<altskin>✦)\\] )?(?<pet>[\\w ]+?)(?:(?<skin> ✦))?$",
    )

    /**
     * WRAPPED-REGEX-TEST: " +163,119,730.2 XP"
     * WRAPPED-REGEX-TEST: " 33,915/179.7k XP (18.9%)"
     * WRAPPED-REGEX-TEST: " 2,877.5/9.7k XP (29.7%)"
     * WRAPPED-REGEX-TEST: " 931,886.2/1.4M XP (67.2%)"
     * WRAPPED-REGEX-TEST: " 251,016.4/561.7k XP (44.7%)"
     * WRAPPED-REGEX-TEST: " 3,138.4/9.7k XP (32.4%)"
     * WRAPPED-REGEX-TEST: " MAX LEVEL"
     */
    @Suppress("MaxLineLength")
    val petTabWidgetXpPattern by patternGroup.pattern(
        "tab.xp",
        " (?:(?<max>MAX LEVEL)|(?:\\+)?(?<current>[\\d,.kM]+)(?:(?:|\\/)*(?<next>[\\d,.kM]+))? XP(?: \\((?<percentage>[\\d.]+)%\\))?)",
    )

    /**
     * REGEX-TEST: §7§7Selected pet: §6Chicken§5 ✦
     * REGEX-TEST: §7§7Selected pet: §5Rift Ferret
     * REGEX-TEST: §7§7Selected pet: §dEndermite
     * REGEX-FAIL: §7§7Selected pet: §cNone
     */
    val petMenuSelectedPetNamePattern by patternGroup.pattern(
        "menu.selected.name",
        "(?:§.)+Selected pet: §(?<rarity>[^c])(?<pet>[\\w ]+)(?<skin>§. ✦)?",
    )

    /**
     * REGEX-TEST: §7Progress to Level 52: §e29.7%
     * REGEX-TEST: §7Progress to Level 2: §e0%
     * REGEX-TEST: §7Progress to Level 69: §e18.9%
     * REGEX-TEST: §b§lMAX LEVEL
     */
    val petMenuSelectedPetProgressPattern by patternGroup.pattern(
        "menu.selected.progress",
        "(?:§.)+(?:MAX LEVEL|Progress to Level (?<next>\\d+): (?:§.)+(?<percentage>[\\d.]+)%)",
    )

    /**
     * REGEX-TEST: §2§l§m        §f§l§m                 §r §e2,877.5§6/§e9.7k
     * REGEX-TEST: §2§l§m     §f§l§m                    §r §e33,915§6/§e179.7k
     * REGEX-TEST: §2§l§m                 §f§l§m        §r §e931,886.2§6/§e1.4M
     * REGEX-TEST: §f§l§m                         §r §e0§6/§e660
     * REGEX-TEST: §8▸ 25,353,248 XP
     */
    val petMenuSelectedPetXpPattern by patternGroup.pattern(
        "menu.selected.xp",
        "(?:§.|▸| )+(?<current>[\\d,.kM]+)(?: XP|(?:§.|\\/)+(?<next>[\\d,.kM]+))",
    )

    /**
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 100] §dEnderman§e! §a§lVIEW RULE
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 200] §6Golden Dragon§e! §a§lVIEW RULE
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 100] §dRabbit§9 ✦§e! §a§lVIEW RULE
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 200] §r§8[§r§6122§4✦] §r§6Golden Dragon§e! §a§lVIEW RULE
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 200] §8[§634§8§4✦§8] §6Golden Dragon§e! §a§lVIEW RULE
     */
    @Suppress("MaxLineLength")
    val autoPetMessagePattern by patternGroup.pattern(
        "autopet.message",
        "§cAutopet §eequipped your §7\\[Lvl (?<level>\\d+)] (?:(?:§.)+\\[(?:§.)*\\d+(?:§.)*(?<altskin>§.✦)(?:§.)*\\] )?(?:§.)*§(?<rarity>.)(?<pet>[^§]+)(?<skin>§. ✦)?§e! §a§lVIEW RULE",
    )

    /**
     * REGEX-TEST: Held Item: Poignant Lucky Clover
     * REGEX-TEST: Equip: [Lvl 200] Rose Dragon Held Item: Poignant Lucky Clover
     */
    val autoPetHoverHeldItemPattern by patternGroup.pattern(
        "autopet.hover.helditem.clean",
        ".*Held Item: (?<item>.*)",
    )

    /**
     * REGEX-TEST: §aYour pet is now holding §r§6Burnt Texts§r§a.
     * REGEX-TEST: §aYour pet is now holding §r§9Combat Exp Boost§r§a.
     * REGEX-TEST: Your pet is now holding Burnt Texts.
     */
    val petItemHeldMessagePattern by patternGroup.pattern(
        "chat.helditem",
        "(?:§a)?Your pet is now holding (?<item>.+?)(?:§r)?(?:§a)?\\.",
    )
}
