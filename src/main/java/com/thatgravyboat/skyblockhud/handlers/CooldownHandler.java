package com.thatgravyboat.skyblockhud.handlers;

import com.google.common.collect.Sets;
import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.api.item.IAbility;
import com.thatgravyboat.skyblockhud.utils.Utils;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class CooldownHandler {

    private static final Pattern ABILITY_REGEX = Pattern.compile("\u00A76Ability: (.*) \u00A7e\u00A7lRIGHT CLICK .* \u00A78Cooldown: \u00A7a(\\d*)s");

    private static final Map<String, Cooldown> COOLDOWNS = new HashMap<>();

    private static final Set<String> CUSTOM_HANDLED_COOLDOWNS = Sets.newHashSet("Mining Speed Boost");

    public static Matcher getAbility(NBTTagCompound nbt) {
        if (nbt != null && nbt.hasKey("ExtraAttributes") && nbt.getCompoundTag("ExtraAttributes").hasKey("uuid") && nbt.hasKey("display")) {
            NBTTagCompound display = nbt.getCompoundTag("display");
            if (display != null && display.hasKey("Lore")) {
                NBTTagList lore = display.getTagList("Lore", 8);
                List<String> loreList = new ArrayList<>();
                for (int i = 0; i < lore.tagCount(); i++) {
                    String loreLine = lore.getStringTagAt(i).trim();
                    if (!loreLine.isEmpty()) loreList.add(loreLine);
                }
                Matcher abilityMatcher = ABILITY_REGEX.matcher(String.join(" ", loreList));
                if (abilityMatcher.find()) {
                    return abilityMatcher;
                }
            }
        }
        return null;
    }

    private static void addCooldown(String id, int time) {
        COOLDOWNS.putIfAbsent(id, new Cooldown(time * 20));
    }

    private static void addCooldown(IAbility ability, boolean isForced) {
        if (isForced || !CUSTOM_HANDLED_COOLDOWNS.contains(ability.getAbility())) {
            addCooldown(ability.getAbility(), ability.getAbilityTime());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChat(ClientChatReceivedEvent event) {
        String message = Utils.removeColor(event.message.getUnformattedText());
        if (event.type != 2 && message.equals("You used your Mining Speed Boost Pickaxe Ability!")) {
            if (Minecraft.getMinecraft().thePlayer.getHeldItem() != null) {
                IAbility ability = (IAbility) (Object) Minecraft.getMinecraft().thePlayer.getHeldItem();
                if (ability.getAbility().equals("Mining Speed Boost")) {
                    addCooldown("Mining Speed Boost", ability.getAbilityTime());
                }
            }
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (SkyblockHud.config.misc.hideItemCooldowns) return;
        if (event.phase.equals(TickEvent.Phase.END)) {
            COOLDOWNS.values().forEach(Cooldown::tick);
            COOLDOWNS.entrySet().removeIf(entry -> entry.getValue().isOver());
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (SkyblockHud.config.misc.hideItemCooldowns) return;
        if (event.action.equals(PlayerInteractEvent.Action.RIGHT_CLICK_AIR) || event.action.equals(PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)) {
            if (event.entityPlayer.getHeldItem() != null) {
                IAbility ability = (IAbility) ((Object) event.entityPlayer.getHeldItem());
                if (ability.getAbility() != null) {
                    addCooldown(ability, false);
                }
            }
        }
    }

    public static float getAbilityTime(ItemStack stack) {
        IAbility ability = (IAbility) ((Object) stack);
        if (ability.getAbility() != null) {
            return COOLDOWNS.containsKey(ability.getAbility()) ? COOLDOWNS.get(ability.getAbility()).getTime() : -1f;
        }
        return -1f;
    }

    private static class Cooldown {

        public int current;
        public final int end;

        Cooldown(int end) {
            this.end = end;
        }

        public boolean isOver() {
            return current >= end;
        }

        public void tick() {
            current++;
        }

        public float getTime() {
            return current / (float) end;
        }
    }
}
