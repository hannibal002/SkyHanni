package at.hannibal2.skyhanni.features.mining

enum class OreType(
    val oreName: String,
    val shaftMultiplier: Int = 0,
) {
    MITHRIL("Mithril", 2),
    TITANIUM("Titanium", 8),
    COBBLESTONE("Cobblestone"),
    COAL("Coal"),
    IRON("Coal"),
    GOLD("Gold"),
    LAPIS("Lapis Lazuli"),
    REDSTONE("Redstone"),
    EMERALD("Emerald"),
    DIAMOND("Diamond"),
    NETHERRACK("Netherrack"),
    QUARTZ("Nether Quartz"),
    GLOWSTONE("Glowstone"),
    MYCELIUM("Mycelium"),
    RED_SAND("Red Sand"),
    SULPHUR("Sulphur"),
    GRAVEL("Gravel"),
    END_STONE("End Stone"),
    OBSIDIAN("Obsidian"),
    HARD_STONE("Hard Stone"),
    GEMSTONE("Gemstone", 2),
    UMBER("Umber", 2),
    TUNGSTEN("Tungsten", 2),
    GLACITE("Glacite", 2),
    ;
}
