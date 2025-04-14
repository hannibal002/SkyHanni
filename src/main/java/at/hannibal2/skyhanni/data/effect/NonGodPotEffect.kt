package at.hannibal2.skyhanni.data.effect

// TODO move the whole list into the repo
enum class NonGodPotEffect(
    val tabListName: String,
    val isMixin: Boolean = false,
    val inventoryItemName: String = tabListName,
) {
    SMOLDERING("§aSmoldering Polarization I"),
    GLOWY("§2Mushed Glowy Tonic I"),
    WISP("§bWisp's Ice-Flavored Water I"),
    GOBLIN("§2King's Scent I"),

    INVISIBILITY("§8Invisibility I"), // when wearing sorrow armor

    REV("§cZombie Brain Mixin", true),
    TARA("§6Spider Egg Mixin", true),
    SVEN("§bWolf Fur Mixin", true),
    VOID("§6End Portal Fumes", true),
    BLAZE("§fGabagoey", true),
    GLOWING_MUSH("§2Glowing Mush Mixin", true),
    HOT_CHOCOLATE("§6Hot Chocolate Mixin I", true),

    DEEP_TERROR("§4Deepterror", true),

    GREAT_SPOOK("§fGreat Spook I", inventoryItemName = "§fGreat Spook Potion"),

    DOUCE_PLUIE_DE_STINKY_CHEESE("§eDouce Pluie de Stinky Cheese I"),

    HARVEST_HARBINGER("§6Harvest Harbinger V"),

    PEST_REPELLENT("§6Pest Repellent I§r"),
    PEST_REPELLENT_MAX("§6Pest Repellent II"),

    CURSE_OF_GREED("§4Curse of Greed I"),

    COLD_RESISTANCE_4("§bCold Resistance IV"),

    POWDER_PUMPKIN("§fPowder Pumpkin I"),
    FILET_O_FORTUNE("§fFilet O' Fortune I"),
    CHILLED_PRISTINE_POTATO("§fChilled Pristine Potato I"),
}
