package com.thatgravyboat.skyblockhud.playerstats;

import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.Utils;
import com.thatgravyboat.skyblockhud.overlay.RPGHud;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionBarParsing {

    private static String lastActionBar = "";
    private static String lastLowActionBar = "";
    private static IChatComponent lastLowEditedActionBar = null;

    private static final Pattern HealthRegex = Pattern.compile("([0-9]+)/([0-9]+)\u2764");
    private static final Pattern HealingRegex = Pattern.compile("\\+([0-9]+)[\u2586\u2585\u2584\u2583\u2582\u2581]");
    private static final Pattern DefenseRegex = Pattern.compile("([0-9]+)\u2748 Defense");
    private static final Pattern ManaRegex = Pattern.compile("([0-9]+)/([0-9]+)\u270E Mana");
    private static final Pattern ManaOverflowRegex = Pattern.compile("([0-9]+)/([0-9]+)\u270E ([0-9]+)\u02AC");
    private static final Pattern ManaDecreaseRegex = Pattern.compile("-([0-9]+) Mana \\(");
    private static final Pattern XpGainRegex = Pattern.compile("\\+(\\d*\\.?\\d*) (Farming|Mining|Combat|Foraging|Fishing|Enchanting|Alchemy|Carpentry|Runecrafting) \\((\\d*\\.?\\d*)%\\)");

    private static final Pattern HealthReplaceRegex = Pattern.compile("\u00A7c([0-9]+)/([0-9]+)\u2764");
    private static final Pattern HealingReplaceRegex = Pattern.compile("\\+\u00A7c([0-9]+)[\u2586\u2585\u2584\u2583\u2582\u2581]");
    private static final Pattern HealthAbsorptionReplaceRegex = Pattern.compile("\u00A76([0-9]+)/([0-9]+)\u2764");
    private static final Pattern DefenseReplaceRegex = Pattern.compile("\u00A7a([0-9]+)\u00A7a\u2748 Defense");
    private static final Pattern ManaReplaceRegex = Pattern.compile("\u00A7b([0-9]+)/([0-9]+)\u270E Mana");
    private static final Pattern ManaOverflowReplaceRegex = Pattern.compile("\u00A7b([0-9]+)/([0-9]+)\u270E \u00A73([0-9]+)\u02AC");

    private static int ticksSinceLastPrediction = 0;
    private static boolean predict = false;

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event){
        if (predict) {
            ticksSinceLastPrediction++;
            if (ticksSinceLastPrediction == 20 && SkyblockHud.config.rpg.showRpgHud) {
                ticksSinceLastPrediction = 0;
                RPGHud.manaPredictionUpdate(true, 0);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onStatusBarHigh(ClientChatReceivedEvent event){
        if (event.type == 2 && SkyblockHud.hasSkyblockScoreboard() && SkyblockHud.config.rpg.showRpgHud){
            parseActionBar(event.message.getUnformattedText());
        }
    }


    @SubscribeEvent(priority = EventPriority.LOW)
    public void onStatusBarLow(ClientChatReceivedEvent event){
        if (event.type == 2 && SkyblockHud.hasSkyblockScoreboard() && SkyblockHud.config.rpg.showRpgHud){
            String message = event.message.getUnformattedText();
            if (lastLowEditedActionBar == null || !lastLowActionBar.equals(message)){
                lastLowActionBar = message;
                message = HealthReplaceRegex.matcher(message).replaceAll("");
                message = HealthAbsorptionReplaceRegex.matcher(message).replaceAll("");
                message = DefenseReplaceRegex.matcher(message).replaceAll("");
                message = ManaReplaceRegex.matcher(message).replaceAll("");
                message = ManaOverflowReplaceRegex.matcher(message).replaceAll("");

                lastLowEditedActionBar = new ChatComponentText(message.trim());
            }
            event.message = lastLowEditedActionBar;
        }
    }

    public static void parseActionBar(String input){
        if (!lastActionBar.equals(input)) {
            lastActionBar = input;
            String bar = Utils.removeColor(input);

            Matcher HealthMatcher = HealthRegex.matcher(bar);
            Matcher DefenseMatcher = DefenseRegex.matcher(bar);
            Matcher ManaMatcher = ManaRegex.matcher(bar);
            Matcher ManaUseMatcher = ManaDecreaseRegex.matcher(bar);
            Matcher ManaOverflowMatcher = ManaOverflowRegex.matcher(bar);
            Matcher XpGainMatcher = XpGainRegex.matcher(bar);

            boolean healthFound = HealthMatcher.find();
            boolean defenseFound = DefenseMatcher.find();
            boolean manaFound = ManaMatcher.find();
            boolean manaUseFound = ManaUseMatcher.find();
            boolean manaOverflowFound = ManaOverflowMatcher.find();
            boolean xpFound = XpGainMatcher.find();


            if (healthFound) {
                try {
                    RPGHud.updateHealth(Integer.parseInt(HealthMatcher.group(1)), Integer.parseInt(HealthMatcher.group(2)));
                }catch (Exception ignored){}
            }
            if (defenseFound) {
                try {
                    RPGHud.updateDefense(Integer.parseInt(DefenseMatcher.group(1)));
                }catch (Exception ignored){}
            }else if (!xpFound && !manaUseFound){
                    RPGHud.updateDefense(0);
            }
            if (manaFound) {
                try {
                    RPGHud.updateMana(Integer.parseInt(ManaMatcher.group(1)), Integer.parseInt(ManaMatcher.group(2)));
                }catch (Exception ignored){}
            }
            if (!manaFound && manaOverflowFound){
                try {
                    RPGHud.updateMana(Integer.parseInt(ManaOverflowMatcher.group(1)), Integer.parseInt(ManaOverflowMatcher.group(2)));
                    RPGHud.updateOverflow(Integer.parseInt(ManaOverflowMatcher.group(3)));
                }catch (Exception ignored){}
            }
            if (!manaFound){
                if (manaUseFound) {
                    try {
                        RPGHud.manaPredictionUpdate(false, Integer.parseInt(ManaUseMatcher.group(1)));
                    } catch (Exception ignored) {}
                }
                RPGHud.manaPredictionUpdate(true, 0);
            }
            predict = !manaFound;
        }
    }
}
