package at.hannibal2.skyhanni.features.mining.powdertracker

import java.util.regex.Pattern

enum class PowderChestReward(val displayName: String, val pattern: Pattern) {


    MITHRIL_POWDER("§aMithril Powder", "§aYou received §r§b[+](?<amount>.*) §r§aMithril Powder.".toPattern()),
    GEMSTONE_POWDER("§dGemstone Powder", "§aYou received §r§b[+](?<amount>.*) §r§aGemstone Powder.".toPattern()),

    ROUGH_RUBY_GEMSTONE(
        "§fRough Ruby Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§f❤ §r§fRough Ruby Gemstone§r§a.".toPattern()
    ),
    FLAWED_RUBY_GEMSTONE(
        "§aFlawed Sapphire Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§a❤ §r§aFlawed RubyGemstone§r§a.".toPattern()
    ),
    FINE_RUBY_GEMSTONE(
        "§9Fine Ruby Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9❤ §r§9Fine Ruby Gemstone§r§a.".toPattern()
    ),
    FLAWLESS_RUBY_GEMSTONE(
        "§5Flawless Ruby Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9❤ §r§5Flawless Ruby Gemstone§r§a.".toPattern()
    ),

    ROUGH_SAPPHIRE_GEMSTONE(
        "§fRough Sapphire Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§f✎ §r§fRough Sapphire Gemstone§r§a.".toPattern()
    ),
    FLAWED_SAPPHIRE_GEMSTONE(
        "§aFlawed Sapphire Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§a✎ §r§aFlawed Sapphire Gemstone§r§a.".toPattern()
    ),
    FINE_SAPPHIRE_GEMSTONE(
        "§9Fine Sapphire Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9✎ §r§9Fine Sapphire Gemstone§r§a.".toPattern()
    ),
    FLAWLESS_SAPPHIRE_GEMSTONE(
        "§5Flawless Sapphire Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9✎ §r§5Flawless Sapphire Gemstone§r§a.".toPattern()
    ),

    ROUGH_AMBER_GEMSTONE(
        "§fRough Amber Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§f⸕ §r§fRough Amber Gemstone§r§a.".toPattern()
    ),
    FLAWED_AMBER_GEMSTONE(
        "§aFlawed Amber Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§a⸕ §r§aFlawed Amber Gemstone§r§a.".toPattern()
    ),
    FINE_AMBER_GEMSTONE(
        "§9Fine Amber Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9⸕ §r§9Fine Amber Gemstone§r§a.".toPattern()
    ),
    FLAWLESS_AMBER_GEMSTONE(
        "§5Flawless Amber Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9⸕ §r§5Flawless Amber Gemstone§r§a.".toPattern()
    ),

    ROUGH_AMETHYST_GEMSTONE(
        "§fRough Amethyst Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§f❈ §r§fRough Amethyst Gemstone§r§a.".toPattern()
    ),
    FLAWED_AMETHYST_GEMSTONE(
        "§aFlawed Amethyst Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§a❈ §r§aFlawed Amethyst Gemstone§r§a.".toPattern()
    ),
    FINE_AMETHYST_GEMSTONE(
        "§9Fine Amethyst Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9❈ §r§9Fine Amethyst Gemstone§r§a.".toPattern()
    ),
    FLAWLESS_AMETHYST_GEMSTONE(
        "§5Flawless Amethyst Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9❈ §r§5Flawless Amethyst Gemstone§r§a.".toPattern()
    ),

    ROUGH_JADE_GEMSTONE(
        "§fRough Jade Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§f☘ §r§fRough Jade Gemstone§r§a.".toPattern()
    ),
    FLAWED_JADE_GEMSTONE(
        "§aFlawed Jade Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§a☘ §r§aFlawed Jade Gemstone§r§a.".toPattern()
    ),
    FINE_JADE_GEMSTONE(
        "§9Fine Jade Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9☘ §r§9Fine Jade Gemstone§r§a.".toPattern()
    ),
    FLAWLESS_JADE_GEMSTONE(
        "§5Flawless Jade Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9☘ §r§5Flawless Jade Gemstone§r§a.".toPattern()
    ),

    ROUGH_TOPAZ_GEMSTONE(
        "§fRough Topaz Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§f✧ §r§fRough Topaz Gemstone§r§a.".toPattern()
    ),
    FLAWED_TOPAZ_GEMSTONE(
        "§aFlawed Topaz Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§a✧ §r§aFlawed Topaz Gemstone§r§a.".toPattern()
    ),
    FINE_TOPAZ_GEMSTONE(
        "§9Fine Topaz Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9✧ §r§9Fine Topaz Gemstone§r§a.".toPattern()
    ),
    FLAWLESS_TOPAZ_GEMSTONE(
        "§5Flawless Topaz Gemstone",
        "§aYou received §r§f(?<amount>.*) §r§9✧ §r§5Flawless Topaz Gemstone§r§a.".toPattern()
    ),

    FTX_3070("§9FTX 3070", "§aYou received §r§f(?<amount>.*) §r§9FTX 3070§r§a.".toPattern()),
    ELECTRON_TRANSIMTTER(
        "§9Electron Transmitter",
        "§aYou received §r§f(?<amount>.*) §r§9Electron Transmitter§r§a.".toPattern()
    ),
    ROBOTRON_REFLECTOR(
        "§9Robotron Reflector",
        "§aYou received §r§f(?<amount>.*) §r§9Robotron Reflector§r§a.".toPattern()
    ),
    SUPERLITE_MOTOR("§9Superlite Motor", "§aYou received §r§f(?<amount>.*) §r§9Superlite Motor§r§a.".toPattern()),
    CONTROL_SWITCH("§9Control Switch", "§aYou received §r§f(?<amount>.*) §r§9Control Switch§r§a.".toPattern()),
    SYNTHETIC_HEART("§9Synthetic Heart", "§aYou received §r§f(?<amount>.*) §r§9Synthetic Heart§r§a.".toPattern()),

    GOBLIN_EGG("§9Goblin Egg", "§aYou received §r§f(?<amount>.*) §r§9Goblin Egg§r§a.".toPattern()),
    GREEN_GOBLIN_EGG(
        "§aGreen Goblin Egg",
        "§aYou received §r§f(?<amount>.*) §r§a§r§aGreen Goblin Egg§r§a.".toPattern()
    ),
    RED_GOBLIN_EGG("§cRed Goblin Egg", "§aYou received §r§f(?<amount>.*) §r§9§r§cRed Goblin Egg§r§a.".toPattern()),
    YELLOW_GOBLIN_EGG(
        "§eYellow Goblin Egg",
        "§aYou received §r§f(?<amount>.*) §r§9§r§eYellow Goblin Egg§r§a.".toPattern()
    ),
    BLUE_GOBLIN_EGG("§3Blue Goblin Egg", "§aYou received §r§f(?<amount>.*) §r§9§r§3Blue Goblin Egg§r§a.".toPattern()),

    WISHING_COMPASS("§aWishing Compass", "§aYou received §r§f(?<amount>.*) §r§aWishing Compass§r§a.".toPattern()),

    SLUDGE_JUICE("§aSludge Juice", "§aYou received §r§f(?<amount>.*) §r§aSludge Juice§r§a.".toPattern()),
    ASCENSION_ROPE("§9Ascension Rope", "§aYou received §r§f(?<amount>.*) §r§9Ascension Rope§r§a.".toPattern()),
    TREASURITE("§5Treasurite", "§aYou received §r§f(?<amount>.*) §r§5Treasurite§r§a.".toPattern()),
    JUNGLE_HEART("§6Jungle Heart", "§aYou received §r§f(?<amount>.*) §r§6Jungle Heart§r§a.".toPattern()),
    PICKONIMBUS_2000("§5Pickonimbus 2000", "§aYou received §r§f(?<amount>.*) §r§5Pickonimbus 2000§r§a.".toPattern()),
    YOGGIE("§aYoggie", "§aYou received §r§f(?<amount>.*) §r§aYoggie§r§a.".toPattern()),
    PREHISTORIC_EGG("§fPrehistoric Egg", "§aYou received §r§f(?<amount>.*) §r§fPrehistoric Egg§r§a.".toPattern()),
    OIL_BARREL("§aOil Barrel", "§aYou received §r§f(?<amount>.*) §r§aOil Barrel§r§a.".toPattern()),

    DIAMOND_ESSENCE("§bDiamond Essence", "§aYou received §r§b[+](?<amount>.*) Diamond Essence".toPattern()),
    GOLD_ESSENCE("§6Gold Essence", "§aYou received §r§6[+](?<amount>.*) Gold Essence".toPattern()),
}
