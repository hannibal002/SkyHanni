package com.thatgravyboat.skyblockhud_2.dungeons;

public class DungeonPlayer {

    private final Classes dungeonClass;
    private final String name;
    private final int health;
    private final boolean dead;

    public DungeonPlayer(Classes playersClass, String playersName, int playersHealth, boolean isDead) {
        this.dungeonClass = playersClass;
        this.name = playersName;
        this.health = isDead ? 0 : playersHealth;
        this.dead = isDead;
    }

    public Classes getDungeonClass() {
        return this.dungeonClass;
    }

    public String getName() {
        return this.name;
    }

    public int getHealth() {
        return this.dead ? 0 : this.health;
    }

    public boolean isDead() {
        return this.dead;
    }
}
