package com.thatgravyboat.skyblockhud_2.api.events;

import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class SidebarPreGetEvent extends Event {

    public Scoreboard scoreboard;
    public ScoreObjective objective;

    public SidebarPreGetEvent(Scoreboard scoreboard, ScoreObjective objective) {
        this.scoreboard = scoreboard;
        this.objective = objective;
    }
}
