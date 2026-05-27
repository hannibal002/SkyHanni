package at.hannibal2.skyhanni.data.effect

// TODO move the whole list into the repo
enum class NonGodPotEffect(
    val tabListName: String,
    val isMixin: Boolean = false,
    val inventoryItemName: String = tabListName,
    val displayName: String
) {
    SMOLDERING("Smoldering Polarization I", displayName = "§aSmoldering Polarization I"),
    GLOWY("Mushed Glowy Tonic I", displayName = "§2Mushed Glowy Tonic I"),
    WISP("Wisp's Ice-Flavored Water I", displayName = "§bWisp's Ice-Flavored Water I"),
    GOBLIN("King's Scent I", displayName = "§2King's Scent I"),

    INVISIBILITY("Invisibility I", displayName = "§8Invisibility I"), // when wearing sorrow armor

    REV("Zombie Brain Mixin", true, displayName = "§cZombie Brain Mixin"),
    TARA("Spider Egg Mixin", true, displayName = "§6Spider Egg Mixin"),
    SVEN("Wolf Fur Mixin", true, displayName = "§bWolf Fur Mixin"),
    VOID("End Portal Fumes", true, displayName = "§6End Portal Fumes"),
    BLAZE("Gabagoey", true, displayName = "§fGabagoey"),
    GLOWING_MUSH("Glowing Mush Mixin", true, displayName = "§2Glowing Mush Mixin"),
    HOT_CHOCOLATE("Hot Chocolate Mixin I", true, displayName = "§6Hot Chocolate Mixin I"),

    DEEP_TERROR("Deepterror", true, displayName = "§4Deepterror"),

    GREAT_SPOOK("Great Spook I", inventoryItemName = "§fGreat Spook Potion", displayName = "Great Spook I"),

    DOUCE_PLUIE_DE_STINKY_CHEESE("Douce Pluie de Stinky Cheese I", displayName = "§eDouce Pluie de Stinky Cheese I"),

    HARVEST_HARBINGER("Harvest Harbinger V", displayName = "§6Harvest Harbinger V"),

    PEST_REPELLENT("Pest Repellent I", displayName = "§6Pest Repellent I§r"),
    PEST_REPELLENT_MAX("Pest Repellent II", displayName = "§6Pest Repellent II"),

    CURSE_OF_GREED("Curse of Greed I", displayName = "§4Curse of Greed I"),

    COLD_RESISTANCE_4("Cold Resistance IV", displayName = "§bCold Resistance IV"),

    POWDER_PUMPKIN("Powder Pumpkin I", displayName = "§fPowder Pumpkin I"),
    FILET_O_FORTUNE("Filet O' Fortune I", displayName = "§fFilet O' Fortune I"),
    CHILLED_PRISTINE_POTATO("Chilled Pristine Potato I", displayName = "§fChilled Pristine Potato I"),

    LUSHLILAC_BONBON("Lushlilac Bonbon", displayName = "§r§5Lushlilac Bonbon§r§f"),
    PRIME_LUSHLILAC_BONBON("Prime Lushlilac Bonbon", displayName = "§r§5Prime Lushlilac Bonbon§r§f"),
    EXALTED_LUSHLILAC_BONBON("Exalted Lushlilac Bonbon", displayName = "§r§5Exalted Lushlilac Bonbon§r§f"),
    OCEANDY("Oceandy", displayName = "§r§5Oceandy§r§f"),
    CANDYCOMB("Candycomb", displayName = "§r§5Candycomb§r§f"),
}
