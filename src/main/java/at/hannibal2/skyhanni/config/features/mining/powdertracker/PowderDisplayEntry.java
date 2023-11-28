package at.hannibal2.skyhanni.config.features.mining.powdertracker;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum PowderDisplayEntry implements HasLegacyId {
    TITLE("§b§lPowder Tracker", 0),
    DISPLAY_MODE("§7Display Mode: §a[Total] §e[This Session]", 1),
    TOTAL_CHESTS("§d852 Total chests Picked §7(950/h)", 2),
    DOUBLE_POWDER("§bx2 Powder: §aActive!", 3),
    MITHRIL_POWDER("§b250,420 §aMithril Powder §7(350,000/h)", 4),
    GEMSTONE_POWDER("§b250,420 §dGemstone Powder §7(350,000/h)", 5),
    SPACER_1("", 6),
    DIAMOND_ESSENCE("§b129 §bDiamond Essence §7(600/h)", 7),
    GOLD_ESSENCE("§b234 §6Gold Essence §7(700/h)", 8),
    SPACER_2("", 9),
    RUBY("§50§7-§90§7-§a0§f-0 §cRuby Gemstone", 10),
    SAPPHIRE("§50§7-§90§7-§a0§f-0 §bSapphire Gemstone", 11),
    AMBER("§50§7-§90§7-§a0§f-0 §6Amber Gemstone", 12),
    AMETHYST("§50§7-§90§7-§a0§f-0 §5Amethyst Gemstone", 13),
    JADE("§50§7-§90§7-§a0§f-0 §aJade Gemstone", 14),
    TOPAZ("§50§7-§90§7-§a0§f-0 §eTopaz Gemstone", 15),
    FTX("§b14 §9FTX 3070", 16),
    ELECTRON("§b14 §9Electron Transmitter", 17),
    ROBOTRON("§b14 §9Robotron Reflector", 18),
    SUPERLITE("§b14 §9Superlite Motor", 19),
    CONTROL_SWITCH("§b14 §9Control Switch", 20),
    SYNTHETIC_HEART("§b14 §9Synthetic Heart", 21),
    TOTAL_ROBOT_PARTS("§b14 §9Total Robot Parts", 22),
    GOBLIN_EGGS("§90§7-§a0§7-§c0§f-§e0§f-§30 §fGoblin Egg", 23),
    WISHING_COMPASS("§b12 §aWishing Compass", 24),
    SLUDGE_JUICE("§b320 §aSludge Juice", 25),
    ASCENSION_ROPE("§b2 §9Ascension Rope", 26),
    TREASURITE("§b6 §5Treasurite", 27),
    JUNGLE_HEART("§b4 §6Jungle Heart", 28),
    PICKONIMBUS("§b1 §5Pickonimbus 2000", 29),
    YOGGIE("§b14 §aYoggie", 30),
    PREHISTORIC_EGG("§b9 §fPrehistoric Egg", 31),
    OIL_BARREL("§b25 §aOil Barrel", 32),
    ;

    private final String str;
    private final int legacyId;

    PowderDisplayEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    PowderDisplayEntry(String str) {
        this(str, -1);
    }

    @Override
    public int getLegacyId() {
        return legacyId;
    }

    @Override
    public String toString() {
        return str;
    }
}
