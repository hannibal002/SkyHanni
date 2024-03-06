package at.hannibal2.skyhanni.data.jsonobjects.repo;

import com.google.gson.annotations.Expose;

public class CrystalHollowsLobbyAgeWarningJson {
    @Expose
    public int minPlayers;
    @Expose
    public int minLobbyAgeMCDays;
    @Expose
    public int maxLobbyAgeMCDays;
}
