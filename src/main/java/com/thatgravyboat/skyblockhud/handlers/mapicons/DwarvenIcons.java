package com.thatgravyboat.skyblockhud.handlers.mapicons;

import com.thatgravyboat.skyblockhud.ComponentBuilder;
import com.thatgravyboat.skyblockhud.handlers.MapHandler;
import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Vector2f;
import net.minecraft.util.ResourceLocation;

public class DwarvenIcons {

  public static List<MapHandler.MapIcon> dwarvenIcons = new ArrayList<>();

  static {
    setupNpcIcons();
    setupMiscIcons();
    setupInfoIcons();
    setupShopIcons();
    setupQuestIcons();
  }

  private static void setupNpcIcons() {
    dwarvenIcons.add(
      new MapHandler.MapIcon(
        new Vector2f(129, 187),
        new ResourceLocation("skyblockhud", "maps/icons/puzzle.png"),
        new ComponentBuilder()
          .nl("Puzzler", new char[] { 'a', 'l' })
          .nl("Description", 'l')
          .nl("The Puzzler gives you a small puzzle each day to solve and")
          .nl("gives you 1000 mithril powder.")
          .build(),
        MapHandler.MapIconTypes.NPC
      )
    );
  }

  private static void setupMiscIcons() {}

  private static void setupInfoIcons() {
    dwarvenIcons.add(
      new MapHandler.MapIcon(
        new Vector2f(129, 187),
        new ResourceLocation("skyblockhud", "maps/icons/crown.png"),
        new ComponentBuilder()
          .nl("King", new char[] { 'a', 'l' })
          .nl("Description", 'l')
          .nl("The King allows you to first start commissions and if you click")
          .nl("each king which change every skyblock day you will get")
          .nl("the King Talisman.")
          .nl()
          .apd("Click to open HOTM", new char[] { '6', 'l' })
          .build(),
        MapHandler.MapIconTypes.INFO,
        "hotm"
      )
    );
  }

  private static void setupShopIcons() {
    dwarvenIcons.add(
      new MapHandler.MapIcon(
        new Vector2f(4, 8),
        new ResourceLocation("skyblockhud", "maps/icons/blacksmith.png"),
        new ComponentBuilder()
          .nl("Forge", new char[] { 'a', 'l' })
          .nl("Description", 'l')
          .nl("The Forge is where you can go craft special items")
          .nl("and fuel your drill.")
          .nl("NPCS", new char[] { 'c', 'l' })
          .nl(" Forger - Allows you to forge special items")
          .nl(" Jotraeline Greatforge - Allows you to refuel your drill.")
          .nl()
          .apd("Click to warp", new char[] { '6', 'l' })
          .build(),
        MapHandler.MapIconTypes.SHOPS,
        "warpforge"
      )
    );
  }

  private static void setupQuestIcons() {
    dwarvenIcons.add(
      new MapHandler.MapIcon(
        new Vector2f(67, 204),
        new ResourceLocation("skyblockhud", "maps/icons/special.png"),
        new ComponentBuilder()
          .nl("Royal Resident", new char[] { 'a', 'l' })
          .nl("The Royal Resident is a quest where you right")
          .nl("click them for a bit to obtain and if you continue")
          .nl("to right click them for about 7 hours it will give")
          .apd("the achievement Royal Conversation.")
          .build(),
        MapHandler.MapIconTypes.QUEST
      )
    );
  }
}
