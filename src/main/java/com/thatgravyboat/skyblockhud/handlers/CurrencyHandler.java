package com.thatgravyboat.skyblockhud.handlers;

import com.thatgravyboat.skyblockhud.api.events.SidebarLineUpdateEvent;
import com.thatgravyboat.skyblockhud.api.events.SidebarPostEvent;
import com.thatgravyboat.skyblockhud.utils.Utils;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CurrencyHandler {

    private static int bits = 0;
    private static double coins = 0;

    public static void setBits(int amount) {
        bits = amount;
    }

    public static void setCoins(double amount) {
        coins = amount;
    }

    public static int getBits() {
        return bits;
    }

    public static double getCoins() {
        return coins;
    }

    @SubscribeEvent
    public void onSidebarLineUpdate(SidebarLineUpdateEvent event) {
        if (Utils.removeColor(event.formattedLine.toLowerCase().trim()).contains("purse:") || Utils.removeColor(event.formattedLine.toLowerCase().trim()).contains("piggy:")) {
            CurrencyHandler.checkCoins(event.formattedLine);
        }
        if (Utils.removeColor(event.formattedLine.toLowerCase().trim()).contains("bits:") && !event.formattedLine.toLowerCase().contains("(")) {
            CurrencyHandler.checkBits(event.formattedLine);
        }
    }

    @SubscribeEvent
    public void onSidebarPost(SidebarPostEvent event) {
        if (!Arrays.toString(event.arrayScores).toLowerCase().contains("bits:")) {
            CurrencyHandler.setBits(0);
        }
    }

    public static String getCoinsFormatted() {
        DecimalFormat formatter = new DecimalFormat("#,###.0", DecimalFormatSymbols.getInstance(Locale.CANADA));
        String output = formatter.format(coins);
        if (output.equals(".0")) output = "0.0"; else if (output.equals(",0")) output = "0,0";
        return output;
    }

    public static String getBitsFormatted() {
        DecimalFormat formatter = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.CANADA));
        formatter.setRoundingMode(RoundingMode.FLOOR);
        return bits > 999 ? formatter.format((double) bits / 1000) + "k" : String.valueOf(bits);
    }

    public static void checkCoins(String formatedScoreboardLine) {
        String purse = Utils.removeWhiteSpaceAndRemoveWord(Utils.removeColor(formatedScoreboardLine.toLowerCase().trim()), Utils.removeColor(formatedScoreboardLine.toLowerCase().trim()).contains("purse:") ? "purse:" : "piggy:").replace(",", "");
        if (!purse.contains("(") && !purse.contains("+")) {
            try {
                double coins = Double.parseDouble(Pattern.compile("[^0-9.]").matcher(purse).replaceAll(""));
                CurrencyHandler.setCoins(coins);
            } catch (IllegalArgumentException ex) {
                System.out.println("Failed to parse purse, please report to ThatGravyBoat. Purse Text: " + purse);
            }
        }
    }

    public static void checkBits(String formatedScoreboardLine) {
        String bits = Utils.removeWhiteSpaceAndRemoveWord(Utils.removeColor(formatedScoreboardLine.toLowerCase().trim()), "bits:").replace(",", "");
        try {
            int bit = Integer.parseInt(Pattern.compile("[^0-9]").matcher(bits).replaceAll(""));
            CurrencyHandler.setBits(bit);
        } catch (IllegalArgumentException ex) {
            System.out.println("Failed to parse bits, please report to ThatGravyBoat. Bits Text: " + bits);
        }
    }
}
