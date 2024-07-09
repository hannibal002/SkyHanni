package at.hannibal2.skyhanni.features.mining.powdertracker

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

enum class PowderChestReward(val displayName: String, pattern: String) {

    MITHRIL_POWDER(
        "§aMithril Powder",
        ".*§2Mithril Powder(?: §r§8x(?<amount>.*))?",
    ),
    GEMSTONE_POWDER(
        "§dGemstone Powder",
        ".*§dGemstone Powder(?: §r§8x(?<amount>.*))?",
    ),

    ROUGH_RUBY_GEMSTONE(
        "§fRough Ruby Gemstone",
        ".*§f❤ Rough Ruby Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FLAWED_RUBY_GEMSTONE(
        "§aFlawed Sapphire Gemstone",
        ".*§a❤ Flawed Ruby Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FINE_RUBY_GEMSTONE(
        "§9Fine Ruby Gemstone",
        ".*§9❤ Fine Ruby Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FLAWLESS_RUBY_GEMSTONE(
        "§5Flawless Ruby Gemstone",
        ".*§9❤ Flawless Ruby Gemstone(?: §r§8x(?<amount>.*))?",
    ),

    ROUGH_SAPPHIRE_GEMSTONE(
        "§fRough Sapphire Gemstone",
        ".*§f✎ Rough Sapphire Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FLAWED_SAPPHIRE_GEMSTONE(
        "§aFlawed Sapphire Gemstone",
        ".*§a✎ Flawed Sapphire Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FINE_SAPPHIRE_GEMSTONE(
        "§9Fine Sapphire Gemstone",
        ".*§9✎ Fine Sapphire Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FLAWLESS_SAPPHIRE_GEMSTONE(
        "§5Flawless Sapphire Gemstone",
        ".*§9✎ Flawless Sapphire Gemstone(?: §r§8x(?<amount>.*))?",
    ),

    ROUGH_AMBER_GEMSTONE(
        "§fRough Amber Gemstone",
        ".*§f⸕ Rough Amber Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FLAWED_AMBER_GEMSTONE(
        "§aFlawed Amber Gemstone",
        ".*§a⸕ Flawed Amber Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FINE_AMBER_GEMSTONE(
        "§9Fine Amber Gemstone",
        ".*§9⸕ Fine Amber Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FLAWLESS_AMBER_GEMSTONE(
        "§5Flawless Amber Gemstone",
        ".*§9⸕ Flawless Amber Gemstone(?: §r§8x(?<amount>.*))?",
    ),

    ROUGH_AMETHYST_GEMSTONE(
        "§fRough Amethyst Gemstone",
        ".*§f❈ Rough Amethyst Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FLAWED_AMETHYST_GEMSTONE(
        "§aFlawed Amethyst Gemstone",
        ".*§a❈ Flawed Amethyst Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FINE_AMETHYST_GEMSTONE(
        "§9Fine Amethyst Gemstone",
        ".*§9❈ Fine Amethyst Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FLAWLESS_AMETHYST_GEMSTONE(
        "§5Flawless Amethyst Gemstone",
        ".*§9❈ Flawless Amethyst Gemstone(?: §r§8x(?<amount>.*))?",
    ),

    ROUGH_JADE_GEMSTONE(
        "§fRough Jade Gemstone",
        ".*§f☘ Rough Jade Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FLAWED_JADE_GEMSTONE(
        "§aFlawed Jade Gemstone",
        ".*§a☘ Flawed Jade Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FINE_JADE_GEMSTONE(
        "§9Fine Jade Gemstone",
        ".*§9☘ Fine Jade Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FLAWLESS_JADE_GEMSTONE(
        "§5Flawless Jade Gemstone",
        ".*§5☘ Flawless Jade Gemstone(?: §r§8x(?<amount>.*))?",
    ),

    ROUGH_TOPAZ_GEMSTONE(
        "§fRough Topaz Gemstone",
        ".*§f✧ Rough Topaz Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FLAWED_TOPAZ_GEMSTONE(
        "§aFlawed Topaz Gemstone",
        ".*§a✧ Flawed Topaz Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FINE_TOPAZ_GEMSTONE(
        "§9Fine Topaz Gemstone",
        ".*§9✧ Fine Topaz Gemstone(?: §r§8x(?<amount>.*))?",
    ),
    FLAWLESS_TOPAZ_GEMSTONE(
        "§5Flawless Topaz Gemstone",
        ".*§9✧ Flawless Topaz Gemstone(?: §r§8x(?<amount>.*))?",
    ),

    FTX_3070(
        "§9FTX 3070",
        ".*§9FTX 3070(?: §r§8x(?<amount>.*))?",
    ),
    ELECTRON_TRANSIMTTER(
        "§9Electron Transmitter",
        ".*§9Electron Transmitter(?: §r§8x(?<amount>.*))?",
    ),
    ROBOTRON_REFLECTOR(
        "§9Robotron Reflector",
        ".*§9Robotron Reflector(?: §r§8x(?<amount>.*))?",
    ),
    SUPERLITE_MOTOR(
        "§9Superlite Motor",
        ".*§9Superlite Motor(?: §r§8x(?<amount>.*))?",
    ),
    CONTROL_SWITCH(
        "§9Control Switch",
        ".*§9Control Switch(?: §r§8x(?<amount>.*))?",
    ),
    SYNTHETIC_HEART(
        "§9Synthetic Heart",
        ".*§9Synthetic Heart(?: §r§8x(?<amount>.*))?",
    ),

    GOBLIN_EGG(
        "§9Goblin Egg",
        ".*§9Goblin Egg(?: §r§8x(?<amount>.*))?",
    ),
    GREEN_GOBLIN_EGG(
        "§aGreen Goblin Egg",
        ".*§a§r§aGreen Goblin Egg(?: §r§8x(?<amount>.*))?",
    ),
    RED_GOBLIN_EGG(
        "§cRed Goblin Egg",
        ".*§9§r§cRed Goblin Egg(?: §r§8x(?<amount>.*))?",
    ),
    YELLOW_GOBLIN_EGG(
        "§eYellow Goblin Egg",
        ".*§9§r§eYellow Goblin Egg(?: §r§8x(?<amount>.*))?",
    ),
    BLUE_GOBLIN_EGG(
        "§3Blue Goblin Egg",
        ".*§9§r§3Blue Goblin Egg(?: §r§8x(?<amount>.*))?",
    ),

    WISHING_COMPASS(
        "§aWishing Compass",
        ".*§aWishing Compass(?: §r§8x(?<amount>.*))?",
    ),
    SLUDGE_JUICE(
        "§aSludge Juice", ".*§aSludge Juice(?: §r§8x(?<amount>.*))?",
    ),
    ASCENSION_ROPE(
        "§9Ascension Rope",
        ".*§9Ascension Rope(?: §r§8x(?<amount>.*))?",
    ),
    TREASURITE(
        "§5Treasurite",
        ".*§5Treasurite(?: §r§8x(?<amount>.*))?",
    ),
    JUNGLE_HEART(
        "§6Jungle Heart",
        ".*§6Jungle Heart(?: §r§8x(?<amount>.*))?",
    ),
    PICKONIMBUS_2000(
        "§5Pickonimbus 2000",
        ".*§5Pickonimbus 2000(?: §r§8x(?<amount>.*))?",
    ),
    YOGGIE(
        "§aYoggie",
        ".*§aYoggie(?: §r§8x(?<amount>.*))?",
    ),
    PREHISTORIC_EGG(
        "§fPrehistoric Egg",
        ".*§fPrehistoric Egg(?: §r§8x(?<amount>.*))?",
    ),
    OIL_BARREL(
        "§aOil Barrel",
        ".*§aOil Barrel(?: §r§8x(?<amount>.*))?",
    ),

    DIAMOND_ESSENCE(
        "§bDiamond Essence",
        ".*§dDiamond Essence(?: §r§8x(?<amount>.*))?",
    ),
    GOLD_ESSENCE(
        "§6Gold Essence",
        ".*§dGold Essence(?: §r§8x(?<amount>.*))?",
    ),
    ;

    val chatPattern by RepoPattern.pattern(
        "mining.powder.tracker.reward." + this.patternName(),
        pattern,
    )

    private fun patternName() = name.lowercase().replace("_", "")
}
