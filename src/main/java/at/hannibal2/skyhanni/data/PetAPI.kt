package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object PetAPI {
    private val patternGroup = RepoPattern.group("misc.pet")
    private val petMenuPattern by patternGroup.pattern(
        "menu.title",
        "Pets(?: \\(\\d+/\\d+\\) )?",
    )

    private var pet: PetData? = null

    /**
     * REGEX-TEST: §e⭐ §7[Lvl 200] §6Golden Dragon§d ✦
     * REGEX-TEST: ⭐ [Lvl 100] Black Cat ✦
     */
    private val petItemName by patternGroup.pattern(
        "item.name",
        "(?<favorite>(?:§.)*⭐ )?(?:§.)*\\[Lvl (?<level>\\d+)] (?<name>.*)",
    )
    private val neuRepoPetItemName by patternGroup.pattern(
        "item.name.neu.format",
        "(?:§f§f)?§7\\[Lvl (?:1➡(?:100|200)|\\{LVL})] (?<name>.*)",
    )

    /**
     * REGEX-TEST:  §r§7[Lvl 100] §r§dEndermite
     * REGEX-TEST:  §r§7[Lvl 200] §r§8[§r§6108§r§8§r§4✦§r§8] §r§6Golden Dragon
     * REGEX-TEST:  §r§7[Lvl 100] §r§dBlack Cat§r§d ✦
     */
    private val petWidget by patternGroup.pattern(
        "widget.pet",
        "^ §r§7\\[Lvl (?<level>\\d+)](?: (?:§.)+\\[(?:§.)+(?<overflow>\\d+)(?:§.)+✦(?:§.)+])? §r§(?<rarity>.)(?<name>[\\w ]+)(?:§r(?<skin>§. ✦))?\$",
    )

    /**
     * REGEX-TEST:  §r§7No pet selected
     * REGEX-TEST:  §r§6Washed-up Souvenir
     * REGEX-TEST:  §r§9Dwarf Turtle Shelmet
     */
    private val widgetString by patternGroup.pattern(
        "widget.string",
        "^ §r§.(?<string>[\\w -]+)\$",
    )

    /**
     * REGEX-TEST:  §r§b§lMAX LEVEL
     * REGEX-TEST:  §r§6+§r§e21,248,020.7 XP
     * REGEX-TEST:  §r§e15,986.6§r§6/§r§e29k XP §r§6(53.6%)
     */
    private val xpWidget by patternGroup.pattern(
        "widget.xp",
        "^ §r§.(?:§l(?<max>MAX LEVEL)|\\+§r§e(?<overflow>[\\d,.]+) XP|(?<currentXP>[\\d,.]+)§r§6/§r§e(?<maxXP>[\\d.km]+) XP §r§6\\((?<percentage>[\\d.%]+)\\))$",
    )

    private val ignoredPetStrings = listOf(
        "Archer",
        "Berserk",
        "Mage",
        "Tank",
        "Healer",
        "➡",
    )

    fun isPetMenu(inventoryTitle: String): Boolean = petMenuPattern.matches(inventoryTitle)

    // Contains color code + name and for older SkyHanni users maybe also the pet level
    var currentPet: String?
        get() = ProfileStorageData.profileSpecific?.currentPet?.takeIf { it.isNotEmpty() }
        set(value) {
            ProfileStorageData.profileSpecific?.currentPet = value
        }

    fun isCurrentPet(petName: String): Boolean = currentPet?.contains(petName) ?: false

    fun getCleanName(nameWithLevel: String): String? {
        petItemName.matchMatcher(nameWithLevel) {
            return group("name")
        }
        neuRepoPetItemName.matchMatcher(nameWithLevel) {
            return group("name")
        }

        return null
    }

    fun getPetLevel(nameWithLevel: String): Int? = petItemName.matchMatcher(nameWithLevel) {
        group("level").toInt()
    }

    fun hasPetName(name: String): Boolean = petItemName.matches(name) && !ignoredPetStrings.any { name.contains(it) }

    @SubscribeEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.PET)) return

        val newPetLine = event.lines.getOrNull(1) ?: return
        if (newPetLine == pet?.rawPetName) return

        petWidget.matchMatcher(newPetLine) {
            pet = PetData(
                group("name"),
                LorenzRarity.getByColorCode(group("rarity")[0]) ?: LorenzRarity.ULTIMATE,
                NEUInternalName.NONE,
                group("skin") != null,
                group("level").toInt(),
                0.0,
                newPetLine,
            )
        }

        widgetString.matchMatcher(newPetLine) {
            val string = group("string")
            if (string == "No pet selected") {
                pet = null
                return
            }
            pet = pet?.let {
                PetData(
                    it.name,
                    it.rarity,
                    NEUInternalName.fromItemNameOrNull(string) ?: NEUInternalName.NONE,
                    it.hasSkin,
                    it.level,
                    it.xp,
                    it.rawPetName,
                )
            }
        }


        xpWidget.matchMatcher(newPetLine) {
            //i don't feel like doing this right now
        }
    }

    @SubscribeEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("PetAPI")
        if (pet != null) {
            event.addIrrelevant {
                add("petName: '${pet?.name}'")
                add("petRarity: '${pet?.rarity}'")
                add("petItem: '${pet?.petItem}'")
                add("petHasSkin: '${pet?.hasSkin}'")
                add("petLevel: '${pet?.level}'")
                add("petXP: '${pet?.xp}'")
            }
        } else {
            event.addData("no pet equipped")
        }
    }

    //taken from NEU
    //move to repo
    private val pet_levels = arrayOf(
        100,
        110,
        120,
        130,
        145,
        160,
        175,
        190,
        210,
        230,
        250,
        275,
        300,
        330,
        360,
        400,
        440,
        490,
        540,
        600,
        660,
        730,
        800,
        880,
        960,
        1050,
        1150,
        1260,
        1380,
        1510,
        1650,
        1800,
        1960,
        2130,
        2310,
        2500,
        2700,
        2920,
        3160,
        3420,
        3700,
        4000,
        4350,
        4750,
        5200,
        5700,
        6300,
        7000,
        7800,
        8700,
        9700,
        10800,
        12000,
        13300,
        14700,
        16200,
        17800,
        19500,
        21300,
        23200,
        25200,
        27400,
        29800,
        32400,
        35200,
        38200,
        41400,
        44800,
        48400,
        52200,
        56200,
        60400,
        64800,
        69400,
        74200,
        79200,
        84700,
        90700,
        97200,
        104200,
        111700,
        119700,
        128200,
        137200,
        146700,
        156700,
        167700,
        179700,
        192700,
        206700,
        221700,
        237700,
        254700,
        272700,
        291700,
        311700,
        333700,
        357700,
        383700,
        411700,
        441700,
        476700,
        516700,
        561700,
        611700,
        666700,
        726700,
        791700,
        861700,
        936700,
        1016700,
        1101700,
        1191700,
        1286700,
        1386700,
        1496700,
        1616700,
        1746700,
        1886700
    )

    fun testLeveltoXP(input: Array<String>) {
        if (input.size == 3) {
            val level = input[0].toIntOrNull()
            val rarity = LorenzRarity.getByName(input[1])
            val isGoldenDragon = input[2].toBooleanStrictOrNull()
            if (level != null && rarity != null && isGoldenDragon != null) {
                val xp: Int = levelToXP(level, rarity, isGoldenDragon) ?: run {
                    ChatUtils.chat("bad input. invalid rarity or level")
                    return
                }
                ChatUtils.chat(xp.addSeparators())
                return
            }
        }
        ChatUtils.chat("bad usage. /shcalcpetxp <level> <rarity> <isGdrag>")
    }

    private fun levelToXP(level: Int, rarity: LorenzRarity, isGoldenDragon: Boolean = false): Int? {
        val rarityOffset = getRarityOffset(rarity) ?: return null
        if (!isValidLevel(level, isGoldenDragon)) return null

        return if (isGoldenDragon && level > 100) {
            pet_levels.slice(0 + rarityOffset..<100 + rarityOffset - 1).sum() + getGdragXP(level - 100)
        } else {
            pet_levels.slice(0 + rarityOffset..<level + rarityOffset - 1).sum()
        }
    }

    private fun isValidLevel(level: Int, isGoldenDragon: Boolean): Boolean {
        return if (isGoldenDragon) level in 1..200
        else level in 1..100
    }

    private fun getGdragXP(levelAbove100: Int): Int {
        return when (levelAbove100) {
            1 -> 0
            2 -> 5555
            else -> 5555 + (levelAbove100 - 2) * 1886700
        }
    }

    private fun getRarityOffset(rarity: LorenzRarity): Int? = when (rarity) {
        LorenzRarity.COMMON -> 0
        LorenzRarity.UNCOMMON -> 6
        LorenzRarity.RARE -> 11
        LorenzRarity.EPIC -> 16
        LorenzRarity.LEGENDARY -> 20
        LorenzRarity.MYTHIC -> 20
        else -> {
            ChatUtils.chat("bad rarity. ${rarity.name}")
            null
        }
    }
}
