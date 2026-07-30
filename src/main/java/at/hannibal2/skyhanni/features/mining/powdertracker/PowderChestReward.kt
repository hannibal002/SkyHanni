package at.hannibal2.skyhanni.features.mining.powdertracker

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

enum class PowderChestReward(val displayName: String, pattern: String) {

    GEMSTONE_POWDER(
        "§dGemstone Powder",
        " {4}Gemstone Powder(?: x(?<amount>[\\d,]+))?",
    ),

    ROUGH_RUBY_GEMSTONE(
        "§fRough Ruby Gemstone",
        " {4}. Rough Ruby Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FLAWED_RUBY_GEMSTONE(
        "§aFlawed Ruby Gemstone",
        " {4}. Flawed Ruby Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FINE_RUBY_GEMSTONE(
        "§9Fine Ruby Gemstone",
        " {4}. Fine Ruby Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FLAWLESS_RUBY_GEMSTONE(
        "§5Flawless Ruby Gemstone",
        " {4}. Flawless Ruby Gemstone(?: x(?<amount>[\\d,]+))?",
    ),

    ROUGH_SAPPHIRE_GEMSTONE(
        "§fRough Sapphire Gemstone",
        " {4}. Rough Sapphire Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FLAWED_SAPPHIRE_GEMSTONE(
        "§aFlawed Sapphire Gemstone",
        " {4}. Flawed Sapphire Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FINE_SAPPHIRE_GEMSTONE(
        "§9Fine Sapphire Gemstone",
        " {4}. Fine Sapphire Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FLAWLESS_SAPPHIRE_GEMSTONE(
        "§5Flawless Sapphire Gemstone",
        " {4}. Flawless Sapphire Gemstone(?: x(?<amount>[\\d,]+))?",
    ),

    ROUGH_AMBER_GEMSTONE(
        "§fRough Amber Gemstone",
        " {4}. Rough Amber Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FLAWED_AMBER_GEMSTONE(
        "§aFlawed Amber Gemstone",
        " {4}. Flawed Amber Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FINE_AMBER_GEMSTONE(
        "§9Fine Amber Gemstone",
        " {4}. Fine Amber Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FLAWLESS_AMBER_GEMSTONE(
        "§5Flawless Amber Gemstone",
        " {4}. Flawless Amber Gemstone(?: x(?<amount>[\\d,]+))?",
    ),

    ROUGH_AMETHYST_GEMSTONE(
        "§fRough Amethyst Gemstone",
        " {4}. Rough Amethyst Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FLAWED_AMETHYST_GEMSTONE(
        "§aFlawed Amethyst Gemstone",
        " {4}. Flawed Amethyst Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FINE_AMETHYST_GEMSTONE(
        "§9Fine Amethyst Gemstone",
        " {4}. Fine Amethyst Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FLAWLESS_AMETHYST_GEMSTONE(
        "§5Flawless Amethyst Gemstone",
        " {4}. Flawless Amethyst Gemstone(?: x(?<amount>[\\d,]+))?",
    ),

    ROUGH_JADE_GEMSTONE(
        "§fRough Jade Gemstone",
        " {4}. Rough Jade Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FLAWED_JADE_GEMSTONE(
        "§aFlawed Jade Gemstone",
        " {4}. Flawed Jade Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FINE_JADE_GEMSTONE(
        "§9Fine Jade Gemstone",
        " {4}. Fine Jade Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FLAWLESS_JADE_GEMSTONE(
        "§5Flawless Jade Gemstone",
        " {4}. Flawless Jade Gemstone(?: x(?<amount>[\\d,]+))?",
    ),

    ROUGH_TOPAZ_GEMSTONE(
        "§fRough Topaz Gemstone",
        " {4}. Rough Topaz Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FLAWED_TOPAZ_GEMSTONE(
        "§aFlawed Topaz Gemstone",
        " {4}. Flawed Topaz Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FINE_TOPAZ_GEMSTONE(
        "§9Fine Topaz Gemstone",
        " {4}. Fine Topaz Gemstone(?: x(?<amount>[\\d,]+))?",
    ),
    FLAWLESS_TOPAZ_GEMSTONE(
        "§5Flawless Topaz Gemstone",
        " {4}. Flawless Topaz Gemstone(?: x(?<amount>[\\d,]+))?",
    ),

    FTX_3070(
        "§9FTX 3070",
        " {4}FTX 3070(?: x(?<amount>[\\d,]+))?",
    ),

    // TODO: Fix typo
    ELECTRON_TRANSIMTTER(
        "§9Electron Transmitter",
        " {4}Electron Transmitter(?: x(?<amount>[\\d,]+))?",
    ),
    ROBOTRON_REFLECTOR(
        "§9Robotron Reflector",
        " {4}Robotron Reflector(?: x(?<amount>[\\d,]+))?",
    ),
    SUPERLITE_MOTOR(
        "§9Superlite Motor",
        " {4}Superlite Motor(?: x(?<amount>[\\d,]+))?",
    ),
    CONTROL_SWITCH(
        "§9Control Switch",
        " {4}Control Switch(?: x(?<amount>[\\d,]+))?",
    ),
    SYNTHETIC_HEART(
        "§9Synthetic Heart",
        " {4}Synthetic Heart(?: x(?<amount>[\\d,]+))?",
    ),

    GOBLIN_EGG(
        "§9Goblin Egg",
        " {4}Goblin Egg(?: x(?<amount>[\\d,]+))?",
    ),
    GREEN_GOBLIN_EGG(
        "§aGreen Goblin Egg",
        " {4}Green Goblin Egg(?: x(?<amount>[\\d,]+))?",
    ),
    RED_GOBLIN_EGG(
        "§cRed Goblin Egg",
        " {4}Red Goblin Egg(?: x(?<amount>[\\d,]+))?",
    ),
    YELLOW_GOBLIN_EGG(
        "§eYellow Goblin Egg",
        " {4}Yellow Goblin Egg(?: x(?<amount>[\\d,]+))?",
    ),
    BLUE_GOBLIN_EGG(
        "§3Blue Goblin Egg",
        " {4}Blue Goblin Egg(?: x(?<amount>[\\d,]+))?",
    ),

    WISHING_COMPASS(
        "§aWishing Compass",
        " {4}Wishing Compass(?: x(?<amount>[\\d,]+))?",
    ),
    SLUDGE_JUICE(
        "§aSludge Juice", " {4}Sludge Juice(?: x(?<amount>[\\d,]+))?",
    ),
    ASCENSION_ROPE(
        "§9Ascension Rope",
        " {4}Ascension Rope(?: x(?<amount>[\\d,]+))?",
    ),
    TREASURITE(
        "§5Treasurite",
        " {4}Treasurite(?: x(?<amount>[\\d,]+))?",
    ),
    JUNGLE_HEART(
        "§6Jungle Heart",
        " {4}Jungle Heart(?: x(?<amount>[\\d,]+))?",
    ),
    PICKONIMBUS_2000(
        "§5Pickonimbus 2000",
        " {4}Pickonimbus 2000(?: x(?<amount>[\\d,]+))?",
    ),
    YOGGIE(
        "§aYoggie",
        " {4}Yoggie(?: x(?<amount>[\\d,]+))?",
    ),
    PREHISTORIC_EGG(
        "§fPrehistoric Egg",
        " {4}Prehistoric Egg(?: x(?<amount>[\\d,]+))?",
    ),
    OIL_BARREL(
        "§aOil Barrel",
        " {4}Oil Barrel(?: x(?<amount>[\\d,]+))?",
    ),

    DIAMOND_ESSENCE(
        "§bDiamond Essence",
        " {4}Diamond Essence(?: x(?<amount>[\\d,]+))?",
    ),
    GOLD_ESSENCE(
        "§6Gold Essence",
        " {4}Gold Essence(?: x(?<amount>[\\d,]+))?",
    ),
    ;

    val chatPattern by RepoPattern.pattern(
        "mining.powder.tracker.reward.${this.patternName()}.colorless",
        pattern,
    )

    private fun patternName() = name.lowercase().replace("_", "")
}
