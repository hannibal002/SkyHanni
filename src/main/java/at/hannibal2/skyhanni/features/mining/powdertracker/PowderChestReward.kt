package at.hannibal2.skyhanni.features.mining.powdertracker

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

enum class PowderChestReward(val displayName: String, pattern: String) {

    GEMSTONE_POWDER(
        "§dGemstone Powder",
        " {4}Gemstone Powder(?: x(?<amount>.*))?",
    ),

    ROUGH_RUBY_GEMSTONE(
        "§fRough Ruby Gemstone",
        " {4}. Rough Ruby Gemstone(?: x(?<amount>.*))?",
    ),
    FLAWED_RUBY_GEMSTONE(
        "§aFlawed Sapphire Gemstone",
        " {4}. Flawed Ruby Gemstone(?: x(?<amount>.*))?",
    ),
    FINE_RUBY_GEMSTONE(
        "§9Fine Ruby Gemstone",
        " {4}. Fine Ruby Gemstone(?: x(?<amount>.*))?",
    ),
    FLAWLESS_RUBY_GEMSTONE(
        "§5Flawless Ruby Gemstone",
        " {4}. Flawless Ruby Gemstone(?: x(?<amount>.*))?",
    ),

    ROUGH_SAPPHIRE_GEMSTONE(
        "§fRough Sapphire Gemstone",
        " {4}. Rough Sapphire Gemstone(?: x(?<amount>.*))?",
    ),
    FLAWED_SAPPHIRE_GEMSTONE(
        "§aFlawed Sapphire Gemstone",
        " {4}. Flawed Sapphire Gemstone(?: x(?<amount>.*))?",
    ),
    FINE_SAPPHIRE_GEMSTONE(
        "§9Fine Sapphire Gemstone",
        " {4}. Fine Sapphire Gemstone(?: x(?<amount>.*))?",
    ),
    FLAWLESS_SAPPHIRE_GEMSTONE(
        "§5Flawless Sapphire Gemstone",
        " {4}. Flawless Sapphire Gemstone(?: x(?<amount>.*))?",
    ),

    ROUGH_AMBER_GEMSTONE(
        "§fRough Amber Gemstone",
        " {4}. Rough Amber Gemstone(?: x(?<amount>.*))?",
    ),
    FLAWED_AMBER_GEMSTONE(
        "§aFlawed Amber Gemstone",
        " {4}. Flawed Amber Gemstone(?: x(?<amount>.*))?",
    ),
    FINE_AMBER_GEMSTONE(
        "§9Fine Amber Gemstone",
        " {4}. Fine Amber Gemstone(?: x(?<amount>.*))?",
    ),
    FLAWLESS_AMBER_GEMSTONE(
        "§5Flawless Amber Gemstone",
        " {4}. Flawless Amber Gemstone(?: x(?<amount>.*))?",
    ),

    ROUGH_AMETHYST_GEMSTONE(
        "§fRough Amethyst Gemstone",
        " {4}. Rough Amethyst Gemstone(?: x(?<amount>.*))?",
    ),
    FLAWED_AMETHYST_GEMSTONE(
        "§aFlawed Amethyst Gemstone",
        " {4}. Flawed Amethyst Gemstone(?: x(?<amount>.*))?",
    ),
    FINE_AMETHYST_GEMSTONE(
        "§9Fine Amethyst Gemstone",
        " {4}. Fine Amethyst Gemstone(?: x(?<amount>.*))?",
    ),
    FLAWLESS_AMETHYST_GEMSTONE(
        "§5Flawless Amethyst Gemstone",
        " {4}. Flawless Amethyst Gemstone(?: x(?<amount>.*))?",
    ),

    ROUGH_JADE_GEMSTONE(
        "§fRough Jade Gemstone",
        " {4}. Rough Jade Gemstone(?: x(?<amount>.*))?",
    ),
    FLAWED_JADE_GEMSTONE(
        "§aFlawed Jade Gemstone",
        " {4}. Flawed Jade Gemstone(?: x(?<amount>.*))?",
    ),
    FINE_JADE_GEMSTONE(
        "§9Fine Jade Gemstone",
        " {4}. Fine Jade Gemstone(?: x(?<amount>.*))?",
    ),
    FLAWLESS_JADE_GEMSTONE(
        "§5Flawless Jade Gemstone",
        " {4}. Flawless Jade Gemstone(?: x(?<amount>.*))?",
    ),

    ROUGH_TOPAZ_GEMSTONE(
        "§fRough Topaz Gemstone",
        " {4}. Rough Topaz Gemstone(?: x(?<amount>.*))?",
    ),
    FLAWED_TOPAZ_GEMSTONE(
        "§aFlawed Topaz Gemstone",
        " {4}. Flawed Topaz Gemstone(?: x(?<amount>.*))?",
    ),
    FINE_TOPAZ_GEMSTONE(
        "§9Fine Topaz Gemstone",
        " {4}. Fine Topaz Gemstone(?: x(?<amount>.*))?",
    ),
    FLAWLESS_TOPAZ_GEMSTONE(
        "§5Flawless Topaz Gemstone",
        " {4}. Flawless Topaz Gemstone(?: x(?<amount>.*))?",
    ),

    FTX_3070(
        "§9FTX 3070",
        " {4}FTX 3070(?: x(?<amount>.*))?",
    ),

    // TODO: Fix typo
    ELECTRON_TRANSIMTTER(
        "§9Electron Transmitter",
        " {4}Electron Transmitter(?: x(?<amount>.*))?",
    ),
    ROBOTRON_REFLECTOR(
        "§9Robotron Reflector",
        " {4}Robotron Reflector(?: x(?<amount>.*))?",
    ),
    SUPERLITE_MOTOR(
        "§9Superlite Motor",
        " {4}Superlite Motor(?: x(?<amount>.*))?",
    ),
    CONTROL_SWITCH(
        "§9Control Switch",
        " {4}Control Switch(?: x(?<amount>.*))?",
    ),
    SYNTHETIC_HEART(
        "§9Synthetic Heart",
        " {4}Synthetic Heart(?: x(?<amount>.*))?",
    ),

    GOBLIN_EGG(
        "§9Goblin Egg",
        " {4}Goblin Egg(?: x(?<amount>.*))?",
    ),
    GREEN_GOBLIN_EGG(
        "§aGreen Goblin Egg",
        " {4}Green Goblin Egg(?: x(?<amount>.*))?",
    ),
    RED_GOBLIN_EGG(
        "§cRed Goblin Egg",
        " {4}Red Goblin Egg(?: x(?<amount>.*))?",
    ),
    YELLOW_GOBLIN_EGG(
        "§eYellow Goblin Egg",
        " {4}Yellow Goblin Egg(?: x(?<amount>.*))?",
    ),
    BLUE_GOBLIN_EGG(
        "§3Blue Goblin Egg",
        " {4}Blue Goblin Egg(?: x(?<amount>.*))?",
    ),

    WISHING_COMPASS(
        "§aWishing Compass",
        " {4}Wishing Compass(?: x(?<amount>.*))?",
    ),
    SLUDGE_JUICE(
        "§aSludge Juice", " {4}Sludge Juice(?: x(?<amount>.*))?",
    ),
    ASCENSION_ROPE(
        "§9Ascension Rope",
        " {4}Ascension Rope(?: x(?<amount>.*))?",
    ),
    TREASURITE(
        "§5Treasurite",
        " {4}Treasurite(?: x(?<amount>.*))?",
    ),
    JUNGLE_HEART(
        "§6Jungle Heart",
        " {4}Jungle Heart(?: x(?<amount>.*))?",
    ),
    PICKONIMBUS_2000(
        "§5Pickonimbus 2000",
        " {4}Pickonimbus 2000(?: x(?<amount>.*))?",
    ),
    YOGGIE(
        "§aYoggie",
        " {4}Yoggie(?: x(?<amount>.*))?",
    ),
    PREHISTORIC_EGG(
        "§fPrehistoric Egg",
        " {4}Prehistoric Egg(?: x(?<amount>.*))?",
    ),
    OIL_BARREL(
        "§aOil Barrel",
        " {4}Oil Barrel(?: x(?<amount>.*))?",
    ),

    DIAMOND_ESSENCE(
        "§bDiamond Essence",
        " {4}Diamond Essence(?: x(?<amount>.*))?",
    ),
    GOLD_ESSENCE(
        "§6Gold Essence",
        " {4}Gold Essence(?: x(?<amount>.*))?",
    ),
    ;

    val chatPattern by RepoPattern.pattern(
        "mining.powder.tracker.reward.${this.patternName()}.new",
        pattern,
    )

    private fun patternName() = name.lowercase().replace("_", "")
}
