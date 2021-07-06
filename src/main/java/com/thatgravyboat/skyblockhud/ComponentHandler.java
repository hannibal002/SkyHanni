package com.thatgravyboat.skyblockhud;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.thatgravyboat.skyblockhud.dungeons.DungeonHandler;
import com.thatgravyboat.skyblockhud.location.*;
import com.thatgravyboat.skyblockhud.seasons.SeasonDateHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class ComponentHandler {
    public static final Pattern SCOREBOARD_CHARACTERS = Pattern.compile("[^]\\[a-z A-Z:0-9/'.()+\\d-§?]");
	private static final Ordering<NetworkPlayerInfo> sortingList = Ordering.from(new PlayerComparator());
    private static int ticksExisted = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event){
        Minecraft mc = Minecraft.getMinecraft();
		ticksExisted++;
        boolean eventPass = false;
        if  (mc.theWorld != null) {
			List<NetworkPlayerInfo> players = sortingList.sortedCopy(mc.thePlayer.sendQueue.getPlayerInfoMap());
			GuiIngameForge.renderObjective = !SkyblockHud.hasSkyblockScoreboard() || !SkyblockHud.config.misc.hideScoreboard;
            if (players != null && SkyblockHud.hasSkyblockScoreboard()){

            	if (ticksExisted % 60 == 0) {
					for (NetworkPlayerInfo player : players) {
						if (player.getDisplayName() != null) {
							String formattedTabListPlayer = SCOREBOARD_CHARACTERS.matcher(Utils.removeColor(player.getDisplayName().getFormattedText())).replaceAll("");
							if (LocationHandler.getCurrentLocation().equals(Locations.CATACOMBS)) {
								if (formattedTabListPlayer.toLowerCase().contains("secrets found:"))
									DungeonHandler.parseTotalSecrets(formattedTabListPlayer);
								if (formattedTabListPlayer.toLowerCase().contains("deaths:"))
									DungeonHandler.parseDeaths(formattedTabListPlayer);
								if (formattedTabListPlayer.toLowerCase().contains("crypts:"))
									DungeonHandler.parseCrypts(formattedTabListPlayer);
							}else if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.DWARVENMINES)){
								if (formattedTabListPlayer.toLowerCase().contains("mithril powder:")){
									DwarvenMineHandler.parseMithril(formattedTabListPlayer);
								}
							}else if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.MUSHROOMDESERT)){
								if (formattedTabListPlayer.toLowerCase().contains("pelts:")){
									try {
										FarmingIslandHandler.pelts = Integer.parseInt(formattedTabListPlayer.toLowerCase().replace("pelts:","").trim());
									}catch (Exception ignored){}
								}
							}
						}
					}
					if (players.size() > 80) {
						for (int i = 61; i <= 80; i++) {
							if (players.get(i).getDisplayName() != null) {
								String formattedTabListPlayer = SCOREBOARD_CHARACTERS.matcher(Utils.removeColor(players.get(i).getDisplayName().getFormattedText())).replaceAll("");
								if (formattedTabListPlayer.toLowerCase().contains("event:")) {
									if (i < 80) {
										if (players.get(i + 1).getDisplayName() != null) {
											String secondLine = SCOREBOARD_CHARACTERS.matcher(Utils.removeColor(players.get(i + 1).getDisplayName().getFormattedText())).replaceAll("");
											SeasonDateHandler.setCurrentEvent(formattedTabListPlayer.replace("Event:", ""), secondLine);
											eventPass = true;
										}
									}
								}
							}
							if (i == 80 && !eventPass) {
								SeasonDateHandler.setCurrentEvent("", "");
							}
						}
					}
				}
            	if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.PARK)) {
					if (players.size() >= 80) {
						for (int i = 41; i <= 60; i++) {
							if (players.get(i).getDisplayName() != null) {
								String formattedTabListPlayer = SCOREBOARD_CHARACTERS.matcher(Utils.removeColor(players.get(i).getDisplayName().getFormattedText())).replaceAll("");
								if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.PARK)) {
									if (formattedTabListPlayer.toLowerCase().contains("rain:")) {
										ParkIslandHandler.parseRain(formattedTabListPlayer.toLowerCase());
									}
								}
							}
						}
					}
				}else if (ParkIslandHandler.isRaining()) {
					ParkIslandHandler.parseRain(null);
				}
			}
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onStatusBar(ClientChatReceivedEvent event){
    	if (event.type == 2){
			if (LocationHandler.getCurrentLocation().equals(Locations.CATACOMBS)) DungeonHandler.parseSecrets(event.message.getFormattedText());
		}
	}

	@SideOnly(Side.CLIENT)
	static class PlayerComparator implements Comparator<NetworkPlayerInfo>
	{
		private PlayerComparator()
		{
		}

		public int compare(NetworkPlayerInfo p_compare_1_, NetworkPlayerInfo p_compare_2_)
		{
			ScorePlayerTeam scoreplayerteam = p_compare_1_.getPlayerTeam();
			ScorePlayerTeam scoreplayerteam1 = p_compare_2_.getPlayerTeam();
			return ComparisonChain.start().compareTrueFirst(p_compare_1_.getGameType() != WorldSettings.GameType.SPECTATOR, p_compare_2_.getGameType() != WorldSettings.GameType.SPECTATOR).compare(scoreplayerteam != null ? scoreplayerteam.getRegisteredName() : "", scoreplayerteam1 != null ? scoreplayerteam1.getRegisteredName() : "").compare(p_compare_1_.getGameProfile().getName(), p_compare_2_.getGameProfile().getName()).result();
		}
	}
}
