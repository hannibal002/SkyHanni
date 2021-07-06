package com.thatgravyboat.skyblockhud.location;

public class EndIslandHandler {

  public enum dragonTypes {
    PROTECTOR("Protector Dragon", 9000000),
    OLD("Old Dragon", 15000000),
    WISE("Wise Dragon", 9000000),
    UNSTABLE("Unstable Dragon", 9000000),
    YOUNG("Young Dragon", 7500000),
    STRONG("Strong Dragon", 9000000),
    SUPERIOR("Superior Dragon", 12000000),
    NODRAGON("", 0);

    private final String displayName;
    private final int maxHealth;

    dragonTypes(String displayName, int maxHealth) {
      this.displayName = displayName;
      this.maxHealth = maxHealth;
    }

    public String getDisplayName() {
      return this.displayName;
    }

    public int getMaxHealth() {
      return this.maxHealth;
    }

    public static dragonTypes findDragon(String input) {
      if (input.contains(" ")) {
        try {
          return dragonTypes.valueOf(
            input
              .toLowerCase()
              .replace("dragon", "")
              .replace(" ", "")
              .toUpperCase()
          );
        } catch (IllegalArgumentException ignored) {
          return NODRAGON;
        }
      } else {
        try {
          return dragonTypes.valueOf(input);
        } catch (IllegalArgumentException ignored) {
          return NODRAGON;
        }
      }
    }
  }

  private static dragonTypes currentDragon = dragonTypes.NODRAGON;

  public static void setCurrentDragon(dragonTypes dragon) {
    currentDragon = dragon;
  }
}
