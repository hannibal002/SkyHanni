package com.thatgravyboat.skyblockhud.location;

public enum Locations {

    //ERROR LOCATIONS
        DEFAULT("unknown", "Error", LocationCategory.ERROR),
        NONE("none", "Unknown", LocationCategory.ERROR),
    //ISLAND
        YOURISLAND("yourisland", "Your Island", LocationCategory.ISLAND),
        GUESTISLAND("guestisland", "Guest Island", LocationCategory.ISLAND),
        MOULBERRYSISLAND("moulberryisland", "Cool Dude Hub", LocationCategory.ISLAND),
    //HUB
        VILLAGE("village", "Village", LocationCategory.HUB),
        AUCTIONHOUSE("auctionhouse", "Auction House", LocationCategory.HUB),
        BAZAARALLEY("bazaaralley", "Bazaar Alley", LocationCategory.HUB),
        BANK("bank", "Bank", LocationCategory.HUB),
        FASHIONSHOP("fashionshop", "Fashion Shop", LocationCategory.HUB),
        COLOSSEUM("colosseum", "Colosseum", LocationCategory.HUB),
        COLOSSEUMARENA("colosseumarena", "Colosseum Arena", LocationCategory.HUB),
        MOUNTAIN("mountain", "Mountain", LocationCategory.HUB),
        HIGHLEVEL("highlevel", "High Level", LocationCategory.HUB),
        WILDERNESS("wilderness", "Wilderness", LocationCategory.HUB),
        FISHERMANSHUT("fishermanshut", "Fisherman's Hut", LocationCategory.HUB),
        FLOWERHOUSE("flowerhouse", "Flower House", LocationCategory.HUB),
        CANVASROOM("canvasroom", "Canvas Room", LocationCategory.HUB),
        TAVERN("tavern", "Tavern", LocationCategory.HUB),
        FOREST("forest", "Forest", LocationCategory.HUB),
        RUINS("ruins", "Ruins", LocationCategory.HUB),
        GRAVEYARD("graveyard", "Graveyard", LocationCategory.HUB),
        COALMINE("coalmine", "Coal Mine", LocationCategory.HUB),
        FARM("farm", "Farm", LocationCategory.HUB),
        LIBRARY("library", "Library", LocationCategory.HUB),
        COMMUNITYCENTER("communitycenter", "Community Center", LocationCategory.HUB),
        ELECTIONROOM("electionroom", "Election Room", LocationCategory.HUB),
        BUILDERSHOUSE("buildershouse", "Builder's House", LocationCategory.HUB),
        BLACKSMITH("blacksmith", "Blacksmith", LocationCategory.HUB),
        FARMHOUSE("farmhouse", "Farmhouse", LocationCategory.HUB),
        WIZARDTOWER("wizardtower", "Wizard Tower", LocationCategory.HUB),
    //THE BARN
        THEBARN("thebarn", "The Barn", LocationCategory.BARN),
        WINDMILL("windmill", "Windmill", LocationCategory.BARN),
    //MUSHROOM DESERT
        MUSHROOMDESERT("mushroomdesert", "Mushroom Desert", LocationCategory.MUSHROOMDESERT),
        DESERTSETTLEMENT("desertsettlement", "Desert Settlement", LocationCategory.MUSHROOMDESERT),
        OASIS("oasis", "Oasis", LocationCategory.MUSHROOMDESERT),
        MUSHROOMGORGE("mushroomgorge", "Mushroom Gorge", LocationCategory.MUSHROOMDESERT),
        SHEPHERDSKEEP("shepherdskeep", "Shepherds Keep", LocationCategory.MUSHROOMDESERT),
        JAKESHOUSE("jakeshouse", "Jake's House", LocationCategory.MUSHROOMDESERT),
        TREASUREHUNTERCAMP("treasurehuntercamp", "Treasure Hunter Camp", LocationCategory.MUSHROOMDESERT),
        GLOWINGMUSHROOMCAVE("glowingmushroomcave", "Glowing Mushroom Cave", LocationCategory.MUSHROOMDESERT),
        TRAPPERSDEN("trappersden", "Trappers Den", LocationCategory.MUSHROOMDESERT),
        OVERGROWNMUSHROOMCAVE("overgrownmushroomcave", "Overgrown Mushroom Cave", LocationCategory.MUSHROOMDESERT),
    //GOLD MINE
        GOLDMINE("goldmine", "Gold Mine", LocationCategory.GOLDMINE),
    //DEEP CAVERNS
        DEEPCAVERNS("deepcaverns", "Deep Caverns", LocationCategory.DEEPCAVERNS),
        GUNPOWDERMINES("gunpowdermines", "Gunpowder Mines", LocationCategory.DEEPCAVERNS),
        LAPISQUARRY("lapisquarry", "Lapis Quarry", LocationCategory.DEEPCAVERNS),
        PIGMANSDEN("pigmansden", "Pigman's Den", LocationCategory.DEEPCAVERNS),
        SLIMEHILL("slimehill", "Slimehill", LocationCategory.DEEPCAVERNS),
        DIAMONDRESERVE("diamondreserve", "Diamond Reserve", LocationCategory.DEEPCAVERNS),
        OBSIDIANSANCTUARY("obsidiansanctuary", "Obsidian Sanctuary", LocationCategory.DEEPCAVERNS),
    //SPIDERS DEN
        SPIDERSDEN("spidersden", "Spider's Den", LocationCategory.SPIDERSDEN),

    //THE END
        THEEND("theend", "The End", LocationCategory.THEEND),
        DRAGONSNEST("dragonsnest", "Dragon's Nest", LocationCategory.THEEND),
        VOIDSEPULTURE("voidsepulture", "Void Sepulture", LocationCategory.THEEND),
    //PARK
        HOWLINGCAVE("howlingcave", "Howling Cave", LocationCategory.PARK),
        BIRCHPARK("birchpark", "Birch Park", LocationCategory.PARK),
        SPRUCEWOODS("sprucewoods", "Spruce Woods", LocationCategory.PARK),
        DARKTHICKET("darkthicket", "Dark Thicket", LocationCategory.PARK),
        SAVANNAWOODLAND("savannawoodland", "Savanna Woodland", LocationCategory.PARK),
        JUNGLEISLAND("jungleisland", "Jungle Island", LocationCategory.PARK),
    //BLAZING FORTRESS
        BLAZINGFORTRESS("blazingfortress", "Blazing Fortress", LocationCategory.FORTRESS),
    //DUNGEONS
        DUNGEONHUB("dungeonhub", "Dungeon Hub", LocationCategory.DUNGEONHUB),
        CATACOMBS("catacombs", "The Catacombs", LocationCategory.DUNGEONHUB),
        CATACOMBSENTRANCE("catacombsentrance", "Catacombs Entrance", LocationCategory.DUNGEONHUB),
    //JERRYISLAND
        JERRYSWORKSHOP("jerrysworkshop", "Jerry's Workshop", LocationCategory.JERRY),
        JERRYPOND("jerrypond", "Jerry Pond", LocationCategory.JERRY),
    //DWARVENMINES
        THELIFT("thelift", "The Lift", LocationCategory.DWARVENMINES),
        DWARVENVILLAGE("dwarvenvillage", "Dwarven Village", LocationCategory.DWARVENMINES),
        DWARVENMINES("dwarvenmines", "Dwarven Mines", LocationCategory.DWARVENMINES),
        LAVASPRINGS("lavasprings", "Lava Springs", LocationCategory.DWARVENMINES),
        PALACEBRIDGE("palacebridge", "Palace Bridge", LocationCategory.DWARVENMINES),
        ROYALPALACE("royalpalace", "Royal Palace", LocationCategory.DWARVENMINES),
        GRANDLIBRARY("grandlibrary", "Grand Library", LocationCategory.DWARVENMINES),
        ROYALQUARTERS("royalquarters", "Royal Quarters", LocationCategory.DWARVENMINES),
        BARRACKSOFHEROES("barracksofheroes", "Barracks of Heroes", LocationCategory.DWARVENMINES),
        HANGINGCOURT("hangingcourt", "Hanging Court", LocationCategory.DWARVENMINES),
        GREATICEWALL("greaticewall", "Great Ice Wall", LocationCategory.DWARVENMINES),
        GOBLINBURROWS("goblinburrows", "Goblin Burrows", LocationCategory.DWARVENMINES),
        FARRESERVE("farreserve", "Far Reserve", LocationCategory.DWARVENMINES),
        CCMINECARTSCO("ccminecartco", "Minecart Co.", LocationCategory.DWARVENMINES),
        UPPERMINES("uppermines", "Upper Mines", LocationCategory.DWARVENMINES),
        RAMPARTSQUARRY("rampartsquarry", "Ramparts Quarry", LocationCategory.DWARVENMINES),
        GATESTOTHEMINES("gatestothemines", "Gates to The Mines", LocationCategory.DWARVENMINES),
        FORGEBASIN("forgebasin", "Forge Basin", LocationCategory.DWARVENMINES),
        THEFORGE("theforge", "The Forge", LocationCategory.DWARVENMINES),
        CLIFFSIDEVEINS("cliffsideveins", "Cliffside Veins", LocationCategory.DWARVENMINES),
        DIVANSGATEWAY("divansgateway", "Divan's Gateway", LocationCategory.DWARVENMINES),
        THEMIST("themist", "The Mist", LocationCategory.DWARVENMINES),
        ROYALMINES("royalmines", "Royal Mines", LocationCategory.DWARVENMINES),
        ARISTOCRATPASSAGE("aristocratpassage", "Aristocrat Passage", LocationCategory.DWARVENMINES),
        MINERSGUILD("minersguild", "Miner's Guild", LocationCategory.DWARVENMINES),
    //CRYSTALHOLLOWS
        JUNGLE("jungle", "Jungle", LocationCategory.CRYSTALHOLLOWS),
        MAMGAFIELDS("magmafields", "Magma Fields", LocationCategory.CRYSTALHOLLOWS),
        GOBLINHOLDOUT("goblinholdout", "Goblin Holdout", LocationCategory.CRYSTALHOLLOWS),
        CRYSTALNUCLEUS("crystalnucleus", "Crystal Nucleus", LocationCategory.CRYSTALHOLLOWS),
        PERCURSORREMNANTS("precursorremnants", "Precursor Remnants", LocationCategory.CRYSTALHOLLOWS),
        MITHRILDEPOSITS("mithrildeposits", "Mithril Deposits", LocationCategory.CRYSTALHOLLOWS);


    private final String name;
    private final String displayName;
    private final LocationCategory category;

    Locations(String name, String displayName, LocationCategory category){
        this.name = name;
        this.displayName = displayName;
        this.category = category;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public LocationCategory getCategory() {
        return this.category;
    }

    static public Locations get(String id) {
        try {
            return Locations.valueOf(id.replace(" ", "").toUpperCase());
        } catch (IllegalArgumentException ex) {
            LocationHandler.reportUndocumentedLocation(id);
            return DEFAULT;
        }
    }


    @Override
    public String toString() {
        return this.name;
    }
}
