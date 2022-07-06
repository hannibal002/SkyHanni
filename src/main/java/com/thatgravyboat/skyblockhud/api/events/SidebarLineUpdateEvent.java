package com.thatgravyboat.skyblockhud.api.events;

import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.fml.common.eventhandler.Event;

public class SidebarLineUpdateEvent extends Event {

    public String rawLine;
    public String formattedLine;
    public int position;
    public Scoreboard scoreboard;
    public ScoreObjective objective;

    public SidebarLineUpdateEvent(String rawLine, String formattedLine, int score, int max, Scoreboard scoreboard, ScoreObjective objective) {
        this.rawLine = rawLine;
        this.formattedLine = formattedLine;
        this.position = max - score;
        this.scoreboard = scoreboard;
        this.objective = objective;
    }
}
