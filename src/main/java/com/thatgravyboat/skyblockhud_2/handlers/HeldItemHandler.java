package com.thatgravyboat.skyblockhud_2.handlers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;

public class HeldItemHandler extends Gui {

    private static final Pattern MANA_COST_REGEX = Pattern.compile("Mana Cost: \u00A73([0-9]+)");

    public static boolean hasManaCost(ItemStack stack) {
        if (stack == null) return false;
        if (!stack.hasTagCompound()) return false;
        if (!stack.getTagCompound().hasKey("display")) return false;
        if (!stack.getTagCompound().getCompoundTag("display").hasKey("Lore")) return false;
        String lore = stack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8).toString();
        return MANA_COST_REGEX.matcher(lore).find();
    }

    public static int getManaCost(ItemStack stack) {
        String lore = stack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8).toString();
        Matcher matcher = MANA_COST_REGEX.matcher(lore);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (Exception ignored) {}
        }
        return 0;
    }
}
