//package com.thatgravyboat.skyblockhud.handlers.mapicons;
//
//import com.thatgravyboat.skyblockhud.handlers.MapHandler;
//import com.thatgravyboat.skyblockhud.utils.ComponentBuilder;
//import java.util.ArrayList;
//import java.util.List;
//import javax.vecmath.Vector2f;
//import net.minecraft.util.ResourceLocation;
//
//public class HubIcons {
//
//    public static List<MapHandler.MapIcon> hubIcons = new ArrayList<>();
//
//    static {
//        setupNpcIcons();
//        setupMiscIcons();
//        setupInfoIcons();
//        setupShopIcons();
//        setupQuestIcons();
//    }
//
//    private static void setupNpcIcons() {
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(-2, -34), new ResourceLocation("skyblockhud", "maps/icons/special.png"), new ComponentBuilder().nl("Event Hut", 'a', 'l').nl("Description", 'l').nl("The Event Hut is where special event npcs").nl("are during some events.").nl("NPC'S", 'c', 'l').nl(" Baker - During New Years").nl(" Jerry - While Winter Island is opened").nl(" Fear Mongerer - During Spooky Festival").apd(" Oringo - During Traveling Zoo").build(), MapHandler.MapIconTypes.NPC));
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(135, 142), new ResourceLocation("skyblockhud", "maps/icons/fairy.png"), new ComponentBuilder().nl("Fairy", 'a', 'l').nl("Description", 'l').nl("The Fairy is where you go when you find fairy souls").apd("to trade them in to get permanent stat upgrades.").build(), MapHandler.MapIconTypes.NPC));
//    }
//
//    private static void setupShopIcons() {
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(-50, -22), new ResourceLocation("skyblockhud", "maps/icons/building.png"), new ComponentBuilder().nl("Builder's House", 'a', 'l').nl("NPCS", 'c', 'l').nl(" Wool Weaver").nl(" Builder").apd(" Mad Redstone Engineer").build(), MapHandler.MapIconTypes.SHOPS));
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(-78, -46), new ResourceLocation("skyblockhud", "maps/icons/bar.png"), new ComponentBuilder().nl("Tavern", 'a', 'l').nl("NPCS", 'c', 'l').nl(" Bartender").nl(" Maddox the slayer").nl("Description", 'l').nl("The Tavern is where maddox the slayer is located you can").nl("start slayer quests with them to unlock").apd("new items the more slayer bosses you kill.").build(), MapHandler.MapIconTypes.SHOPS));
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(36, -82), new ResourceLocation("skyblockhud", "maps/icons/vet.png"), new ComponentBuilder().nl("Vet", 'a', 'l').nl("NPCS", 'c', 'l').nl(" Bea").nl(" Zog").nl(" Kat").nl(" George").nl("Description", 'l').nl("The Vet is where you go to upgrade your pet").nl("at Kat or to buy pet upgrade items from Zog").nl("or trade in your pet at George and if you're").apd("a new player you can buy a bee pet from Bea.").build(), MapHandler.MapIconTypes.SHOPS));
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(58, -73), new ResourceLocation("skyblockhud", "maps/icons/fishing_merchant.png"), new ComponentBuilder().nl("Fishing Merchant", 'a', 'l').nl("Description", 'l').nl("The Fishing Merchant allows you to buy").nl("fishing related items and he has his friend").nl("joe whose in the house hes setup").apd("in front of who sells sponges.").build(), MapHandler.MapIconTypes.SHOPS));
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(46, -53), new ResourceLocation("skyblockhud", "maps/icons/witch.png"), new ComponentBuilder().nl("Alchemist", 'a', 'l').nl("Description", 'l').nl("The Alchemist allows you to buy").apd("potion making related items").build(), MapHandler.MapIconTypes.SHOPS));
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(-4, -128), new ResourceLocation("skyblockhud", "maps/icons/metal_merchants.png"), new ComponentBuilder().nl("Blacksmith Merchants", 'a', 'l').nl("Merchants", 'c', 'l').nl(" Weaponsmith - Weapon Related Items").nl(" Armorsmith - Armor Related Items").apd(" Mine Merchant - Mining Related Items").build(), MapHandler.MapIconTypes.SHOPS));
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(-30, -120), new ResourceLocation("skyblockhud", "maps/icons/blacksmith.png"), new ComponentBuilder().nl("Blacksmith", 'a', 'l').nl("NPCS", 'c', 'l').nl(" Blacksmith").nl(" Dusk").nl(" Smithmonger").nl("Description", 'l').nl("The Blacksmith lets you reforge your items").nl("while the Smithmonger allows you to buy reforge stones").apd("and Dusk allows you to combine and apply runes.").build(), MapHandler.MapIconTypes.SHOPS));
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(124, 180), new ResourceLocation("skyblockhud", "maps/icons/dark_bar.png"), new ComponentBuilder().nl("Dark Bar", 'a', 'l').nl("NPCS", 'c', 'l').nl(" Shifty").nl(" Lucius").nl("Description", 'l').nl("The Dark Bar is where you can buy special").nl("brews from Shifty and you can buy special").nl("items from Lucius after buying a certain").apd("amount of items from the Dark Auction.").build(), MapHandler.MapIconTypes.SHOPS));
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(92, 185), new ResourceLocation("skyblockhud", "maps/icons/dark_ah.png"), new ComponentBuilder().nl("Dark Auction", 'a', 'l').nl("Description", 'l').nl("The Dark Auction allows you to buy").nl("super special items from Sirius the").apd("auctioneer in a special auction.").build(), MapHandler.MapIconTypes.SHOPS));
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(-245, 52), new ResourceLocation("skyblockhud", "maps/icons/scroll.png"), new ComponentBuilder().nl("Lonely Philosopher", 'a', 'l').nl("Shop", '6', 'l').nl(" Travel Scroll to Hub Castle").nl().nl(" Cost").nl(" 150,000 Coins", '6').nl().apd(" Requires ").apd("MVP", 'b').apd("+", 'c').build(), MapHandler.MapIconTypes.SHOPS));
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(24, -38), new ResourceLocation("skyblockhud", "maps/icons/tux.png"), new ComponentBuilder().nl("Fashion Shop", 'a', 'l').nl("NPCS", 'c', 'l').nl(" Wool Weaver").nl(" Builder").apd(" Mad Redstone Engineer").build(), MapHandler.MapIconTypes.SHOPS));
//    }
//
//    private static void setupMiscIcons() {
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(-24, -53), new ResourceLocation("skyblockhud", "maps/icons/bank.png"), new ComponentBuilder().nl("Bank", 'a', 'l').nl("Description", 'l').nl("The Bank is where you can store your money on skyblock").apd("you can also store some items in the vault.").build(), MapHandler.MapIconTypes.MISC));
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(-26, -80), new ResourceLocation("skyblockhud", "maps/icons/ah.png"), new ComponentBuilder().nl("Auction House", 'a', 'l').nl("Description", 'l').nl("The Auction House is where you can auction off your").apd("precious items in skyblock to make a profit.").build(), MapHandler.MapIconTypes.MISC));
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(-38, -66), new ResourceLocation("skyblockhud", "maps/icons/bazaar.png"), new ComponentBuilder().nl("Bazaar", 'a', 'l').nl("Description", 'l').nl("The Bazaar is where you can sell specific items").nl("on a sort of stock market and request and").apd("sell items at a specific price.").build(), MapHandler.MapIconTypes.MISC));
//    }
//
//    private static void setupInfoIcons() {
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(8, -95), new ResourceLocation("skyblockhud", "maps/icons/community.png"), new ComponentBuilder().nl("Community Center", 'a', 'l').nl("Description", 'l').nl("The Community Center is where you can vote").nl("for your favorite election candidate,").nl("access the community shop, upgrade your").apd("account, and help with city projects.").build(), MapHandler.MapIconTypes.INFO));
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(150, 45), new ResourceLocation("skyblockhud", "maps/icons/fishing.png"), new ComponentBuilder().nl("Fisherman's Hut", 'a', 'l').nl("Description", 'l').nl("This is a spot where people regularly").nl("do their fishing, this is one").apd("of many spots.").build(), MapHandler.MapIconTypes.INFO));
//    }
//
//    private static void setupQuestIcons() {
//        hubIcons.add(new MapHandler.MapIcon(new Vector2f(-8, -10), new ResourceLocation("skyblockhud", "maps/icons/painter.png"), new ComponentBuilder().nl("Marco", 'a', 'l').nl("Description", 'l').nl("Marco is an NPC that has no other uses").nl("besides giving you a spray can for").apd("completing a quest.").build(), MapHandler.MapIconTypes.QUEST));
//    }
//}
