package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
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
     * REGEX-TEST: [Lvl 8] Squid
     * REGEX-TEST: [Lvl 100] Hermit Crab
     * REGEX-TEST: [Lvl 200] [122✦] Golden Dragon
     * REGEX-TEST: [Lvl 67] T-Rex
     */
    @Suppress("MaxLineLength")
    val petMenuPetStackNamePattern by patternGroup.pattern(
        "menu.petstack.name",
        "\\[Lvl (?<level>[\\d,]+)] (?:\\[\\d+(?<altskin>✦)] )?(?<pet>[\\w -]+?)(?<skin> ✦)?",
    )

    /**
     * WRAPPED-REGEX-TEST: " [Lvl 100] Hedgehog"
     * WRAPPED-REGEX-TEST: " [Lvl 68] Blaze"
     * WRAPPED-REGEX-TEST: " [Lvl 51] Kuudra"
     * WRAPPED-REGEX-TEST: " [Lvl 100] Flying Fish"
     * WRAPPED-REGEX-TEST: " [Lvl 100] Chicken ✦"
     * WRAPPED-REGEX-TEST: " [Lvl 200] [122✦] Golden Dragon"
     * WRAPPED-REGEX-TEST: " [Lvl 67] T-Rex"
     * WRAPPED-REGEX-FAIL: " No pet selected"
     */
    @Suppress("MaxLineLength")
    val petTabWidgetNamePattern by patternGroup.pattern(
        "tab.name",
        " \\[Lvl (?<level>[\\d,]+)] (?:\\[\\d+(?<altskin>✦)\\] )?(?<pet>[\\w -]+?)(?:(?<skin> ✦))?$",
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
     * REGEX-TEST: Selected pet: Chicken ✦
     * REGEX-TEST: Selected pet: Rift Ferret
     * REGEX-TEST: Selected pet: Endermite
     * REGEX-TEST: Selected pet: T-Rex
     * REGEX-FAIL: Selected pet: None
     */
    val petMenuSelectedPetNamePattern by patternGroup.pattern(
        "menu.selected.name.colorless",
        "Selected pet: (?!None$)(?<pet>[\\w -]+)(?<skin> ✦)?",
    )

    /**
     * REGEX-TEST: Progress to Level 52: 29.7%
     * REGEX-TEST: Progress to Level 2: 0%
     * REGEX-TEST: Progress to Level 69: 18.9%
     * REGEX-TEST: MAX LEVEL
     */
    val petMenuSelectedPetProgressPattern by patternGroup.pattern(
        "menu.selected.progress.colorless",
        "(?:MAX LEVEL|Progress to Level (?<next>\\d+): (?<percentage>[\\d.]+)%)",
    )

    /**
     * WRAPPED-REGEX-TEST: "                          2,877.5/9.7k"
     * WRAPPED-REGEX-TEST: "                          33,915/179.7k"
     * WRAPPED-REGEX-TEST: "                          931,886.2/1.4M"
     * WRAPPED-REGEX-TEST: "                          0/660"
     * REGEX-TEST: ▸ 25,353,248 XP
     */
    val petMenuSelectedPetXpPattern by patternGroup.pattern(
        "menu.selected.xp.colorless",
        "(?:▸| )*(?<current>[\\d,.kM]+)(?: XP|(?:\\/)*(?<next>[\\d,.kM]+))",
    )

    /**
     * REGEX-TEST: Autopet equipped your [Lvl 100] Enderman! VIEW RULE
     * REGEX-TEST: Autopet equipped your [Lvl 200] Golden Dragon! VIEW RULE
     * REGEX-TEST: Autopet equipped your [Lvl 100] Rabbit ✦! VIEW RULE
     * REGEX-TEST: Autopet equipped your [Lvl 200] [122✦] Golden Dragon! VIEW RULE
     * REGEX-TEST: Autopet equipped your [Lvl 200] [34✦] Golden Dragon! VIEW RULE
     * REGEX-TEST: Autopet equipped your [Lvl 67] T-Rex! VIEW RULE
     */
    @Suppress("MaxLineLength")
    val autoPetMessageColorlessPattern by patternGroup.pattern(
        "autopet.message.colorless",
        "Autopet equipped your \\[Lvl (?<level>\\d+)] (?:\\[\\d+(?<altskin>✦)\\] )?(?<pet>[\\w -]+)(?<skin> ✦)?! VIEW RULE",
    )

    /**
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 100] §6Mosquito§e! §a§lVIEW RULE
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 100] §5Rabbit§9 ✦§e! §a§lVIEW RULE
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 200] §6[122✦] Golden Dragon§e! §a§lVIEW RULE
     */
    @Suppress("MaxLineLength")
    val autoPetMessagePattern by patternGroup.pattern(
        "autopet.message.formatted",
        "§cAutopet §eequipped your §7\\[Lvl (?<level>\\d+)] §(?<rarity>.)(?:\\[\\d+(?<altskin>✦)] )?(?<pet>[^§!]+?)(?<skin>§. ✦)?§e! §a§lVIEW RULE(?: \\(\\d+\\))?",
    )

    /**
     * REGEX-TEST: Held Item: Poignant Lucky Clover
     * REGEX-TEST: Equip: [Lvl 200] Rose Dragon Held Item: Poignant Lucky Clover
     */
    val autoPetHoverHeldItemPattern by patternGroup.pattern(
        "autopet.hover.helditem.colorless",
        ".*Held Item: (?<item>.*)",
    )

    /**
     * REGEX-TEST: Your pet is now holding Burnt Texts.
     * REGEX-TEST: Your pet is now holding Combat Exp Boost.
     * REGEX-TEST: Your pet is now holding Burnt Texts.
     */
    val petItemHeldMessagePattern by patternGroup.pattern(
        "chat.helditem",
        "Your pet is now holding (?<item>.+?)\\.",
    )

    /**
     * REGEX-TEST: You removed Quick Claw from your pet!
     */
    val petItemRemovedMessagePattern by patternGroup.pattern(
        "chat.removedhelditem",
        "You removed (?<item>.+?) from your pet!",
    )
}
