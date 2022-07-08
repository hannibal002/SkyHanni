//package com.thatgravyboat.skyblockhud.location;
//
//import static com.thatgravyboat.skyblockhud.handlers.MapHandler.Maps;
//
//import at.lorenz.mod.LorenzMod;
//import com.thatgravyboat.skyblockhud.handlers.MapHandler;
//
//public enum LocationCategory {
//    ERROR("error", 34),
//    ISLAND("island", 43),
//    HUB("hub", 34, Maps.HUB),
//    BARN("barn", 67, Maps.BARN),
//    MUSHROOMDESERT("mushroomdesert", 75, Maps.MUSHROOM),
//    GOLDMINE("gold_mine", 83),
//    DEEPCAVERNS("deepcaverns", 91),
//    SPIDERSDEN("spiders_den", 99, Maps.SPIDERS),
//    PARK("park", 51, Maps.PARK),
//    FORTRESS("fortress", 107, Maps.NETHER),
//    DUNGEONHUB("dungeonhub", 115),
//    JERRY("jerry", 59),
//    THEEND("the_end", 123),
//    DWARVENMINES("dwarven_mines", 131, Maps.DWARVEN),
//    CRYSTALHOLLOWS("crystal_hollows", 139, Maps.CRYSTAL);
//
//    private final String name;
//    private final int texturePos;
//    private final MapHandler.Maps map;
//
//    LocationCategory(String name, int texturePos) {
//        this(name, texturePos, null);
//    }
//
//    LocationCategory(String name, int texturePos, MapHandler.Maps map) {
//        this.name = name;
//        this.texturePos = texturePos;
//        this.map = map;
//    }
//
//    public String getName() {
//        return this.name;
//    }
//
//    public int getTexturePos() {
//        return this.texturePos;
//    }
//
//    public MapHandler.Maps getMap() {
//        if (this.map != null && LorenzMod.config.map.mapLocations.contains(this.ordinal() - 2)) return this.map; else return null;
//    }
//
//    public boolean isMiningCategory() {
//        return this == LocationCategory.DWARVENMINES || this == LocationCategory.CRYSTALHOLLOWS;
//    }
//}
