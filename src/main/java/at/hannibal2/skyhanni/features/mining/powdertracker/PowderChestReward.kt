package at.hannibal2.skyhanni.features.mining.powdertracker

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

enum class PowderChestReward(val displayName: String, pattern: String) {

    MITHRIL_POWDER("§aMithril Powder", "§aYou received §r§b[+](?<amount>.*) §r§aMithril Powder."),
    GEMSTONE_POWDER("§dGemstone Powder", "§aYou received §r§b[+](?<amount>.*) §r§aGemstone Powder."),

    ROUGH_RUBY_GEMSTONE(
        "§fRough Ruby Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§f❤ Rough Ruby Gemstone§r§a."
    ),
    FLAWED_RUBY_GEMSTONE(
        "§aFlawed Sapphire Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§a❤ Flawed Ruby Gemstone§r§a."
    ),
    FINE_RUBY_GEMSTONE(
        "§9Fine Ruby Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9❤ Fine Ruby Gemstone§r§a."
    ),
    FLAWLESS_RUBY_GEMSTONE(
        "§5Flawless Ruby Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9❤ Flawless Ruby Gemstone§r§a."
    ),

    ROUGH_SAPPHIRE_GEMSTONE(
        "§fRough Sapphire Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§f✎ Rough Sapphire Gemstone§r§a."
    ),
    FLAWED_SAPPHIRE_GEMSTONE(
        "§aFlawed Sapphire Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§a✎ Flawed Sapphire Gemstone§r§a."
    ),
    FINE_SAPPHIRE_GEMSTONE(
        "§9Fine Sapphire Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9✎ Fine Sapphire Gemstone§r§a."
    ),
    FLAWLESS_SAPPHIRE_GEMSTONE(
        "§5Flawless Sapphire Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9✎ Flawless Sapphire Gemstone§r§a."
    ),

    ROUGH_AMBER_GEMSTONE(
        "§fRough Amber Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§f⸕ Rough Amber Gemstone§r§a."
    ),
    FLAWED_AMBER_GEMSTONE(
        "§aFlawed Amber Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§a⸕ Flawed Amber Gemstone§r§a."
    ),
    FINE_AMBER_GEMSTONE(
        "§9Fine Amber Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9⸕ Fine Amber Gemstone§r§a."
    ),
    FLAWLESS_AMBER_GEMSTONE(
        "§5Flawless Amber Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9⸕ Flawless Amber Gemstone§r§a."
    ),

    ROUGH_AMETHYST_GEMSTONE(
        "§fRough Amethyst Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§f❈ Rough Amethyst Gemstone§r§a."
    ),
    FLAWED_AMETHYST_GEMSTONE(
        "§aFlawed Amethyst Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§a❈ Flawed Amethyst Gemstone§r§a."
    ),
    FINE_AMETHYST_GEMSTONE(
        "§9Fine Amethyst Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9❈ Fine Amethyst Gemstone§r§a."
    ),
    FLAWLESS_AMETHYST_GEMSTONE(
        "§5Flawless Amethyst Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9❈ Flawless Amethyst Gemstone§r§a."
    ),

    ROUGH_JADE_GEMSTONE(
        "§fRough Jade Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§f☘ Rough Jade Gemstone§r§a."
    ),
    FLAWED_JADE_GEMSTONE(
        "§aFlawed Jade Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§a☘ Flawed Jade Gemstone§r§a."
    ),
    FINE_JADE_GEMSTONE(
        "§9Fine Jade Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9☘ Fine Jade Gemstone§r§a."
    ),
    FLAWLESS_JADE_GEMSTONE(
        "§5Flawless Jade Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9☘ §r§5Flawless Jade Gemstone§r§a."
    ),

    ROUGH_TOPAZ_GEMSTONE(
        "§fRough Topaz Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§f✧ Rough Topaz Gemstone§r§a."
    ),
    FLAWED_TOPAZ_GEMSTONE(
        "§aFlawed Topaz Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§a✧ Flawed Topaz Gemstone§r§a."
    ),
    FINE_TOPAZ_GEMSTONE(
        "§9Fine Topaz Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9✧ Fine Topaz Gemstone§r§a."
    ),
    FLAWLESS_TOPAZ_GEMSTONE(
        "§5Flawless Topaz Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9✧ Flawless Topaz Gemstone§r§a."
    ),

    FTX_3070("§9FTX 3070", "§aYou received §r§f(?<amount>.*) §r§9FTX 3070§r§a."),
    ELECTRON_TRANSIMTTER(
        "§9Electron Transmitter",
        "§aYou received §r§f(?<amount>.*) §r§9Electron Transmitter§r§a."
    ),
    ROBOTRON_REFLECTOR(
        "§9Robotron Reflector",
        "§aYou received §r§f(?<amount>.*) §r§9Robotron Reflector§r§a."
    ),
    SUPERLITE_MOTOR("§9Superlite Motor", "§aYou received §r§f(?<amount>.*) §r§9Superlite Motor§r§a."),
    CONTROL_SWITCH("§9Control Switch", "§aYou received §r§f(?<amount>.*) §r§9Control Switch§r§a."),
    SYNTHETIC_HEART("§9Synthetic Heart", "§aYou received §r§f(?<amount>.*) §r§9Synthetic Heart§r§a."),

    GOBLIN_EGG("§9Goblin Egg", "§aYou received §r§f(?<amount>.*) §r§9Goblin Egg§r§a."),
    GREEN_GOBLIN_EGG(
        "§aGreen Goblin Egg",
        "§aYou received §r§f(?<amount>.*) §r§a§r§aGreen Goblin Egg§r§a."
    ),
    RED_GOBLIN_EGG("§cRed Goblin Egg", "§aYou received §r§f(?<amount>.*) §r§9§r§cRed Goblin Egg§r§a."),
    YELLOW_GOBLIN_EGG(
        "§eYellow Goblin Egg",
        "§aYou received §r§f(?<amount>.*) §r§9§r§eYellow Goblin Egg§r§a."
    ),
    BLUE_GOBLIN_EGG("§3Blue Goblin Egg", "§aYou received §r§f(?<amount>.*) §r§9§r§3Blue Goblin Egg§r§a."),

    WISHING_COMPASS("§aWishing Compass", "§aYou received §r§f(?<amount>.*) §r§aWishing Compass§r§a."),

    SLUDGE_JUICE("§aSludge Juice", "§aYou received §r§f(?<amount>.*) §r§aSludge Juice§r§a."),
    ASCENSION_ROPE("§9Ascension Rope", "§aYou received §r§f(?<amount>.*) §r§9Ascension Rope§r§a."),
    TREASURITE("§5Treasurite", "§aYou received §r§f(?<amount>.*) §r§5Treasurite§r§a."),
    JUNGLE_HEART("§6Jungle Heart", "§aYou received §r§f(?<amount>.*) §r§6Jungle Heart§r§a."),
    PICKONIMBUS_2000("§5Pickonimbus 2000", "§aYou received §r§f(?<amount>.*) §r§5Pickonimbus 2000§r§a."),
    YOGGIE("§aYoggie", "§aYou received §r§f(?<amount>.*) §r§aYoggie§r§a."),
    PREHISTORIC_EGG("§fPrehistoric Egg", "§aYou received §r§f(?<amount>.*) §r§fPrehistoric Egg§r§a."),
    OIL_BARREL("§aOil Barrel", "§aYou received §r§f(?<amount>.*) §r§aOil Barrel§r§a."),

    DIAMOND_ESSENCE("§bDiamond Essence", "§aYou received §r§b[+](?<amount>.*) Diamond Essence§r§a."),
    GOLD_ESSENCE("§6Gold Essence", "§aYou received §r§6[+](?<amount>.*) Gold Essence§r§a."),
    ;

    val chatPattern by RepoPattern.pattern(
        "mining.powder.tracker.reward." + this.patternName(),
        pattern
    )

    private fun patternName() = name.lowercase().replace("_", "")
}
