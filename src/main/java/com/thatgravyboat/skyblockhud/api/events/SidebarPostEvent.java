package com.thatgravyboat.skyblockhud.api.events;

import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;

public class SidebarPostEvent extends Event {

    public Scoreboard scoreboard;
    public ScoreObjective objective;
    public List<String> scores;
    public String[] arrayScores;

    public SidebarPostEvent(Scoreboard scoreboard, ScoreObjective objective, List<String> scores) {
        this.scoreboard = scoreboard;
        this.objective = objective;
        this.scores = scores;
        this.arrayScores = scores.toArray(new String[]{});
    }
}
