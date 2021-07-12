package com.thatgravyboat.skyblockhud.handlers;

import com.google.common.collect.Lists;
import com.thatgravyboat.skyblockhud.Utils;
import com.thatgravyboat.skyblockhud.api.events.LocationChangeEvent;
import com.thatgravyboat.skyblockhud.commands.SimpleCommand;
import com.thatgravyboat.skyblockhud.location.LocationCategory;
import com.thatgravyboat.skyblockhud.location.LocationHandler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CrystalWaypoints {

    public static final HashMap<String, BlockPos> waypoints = new HashMap<>();

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        waypoints.forEach(
            (text, pos) -> {
                GlStateManager.disableCull();
                GlStateManager.disableDepth();
                Utils.renderWaypointText(text, pos, event.partialTicks);
            }
        );
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
    }

    @SubscribeEvent
    public void onLocationChange(LocationChangeEvent event) {
        if (!event.newLocation.getCategory().equals(LocationCategory.CRYSTALHOLLOWS)) {
            waypoints.clear();
        }
    }

    public static class WaypointCommand extends SimpleCommand {

        public WaypointCommand() {
            super(
                "sbhpoints",
                new ProcessCommandRunnable() {
                    @Override
                    public void processCommand(ICommandSender sender, String[] args) {
                        String subCommand = args[0].toLowerCase();
                        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                        switch (subCommand) {
                            case "add":
                                if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.CRYSTALHOLLOWS)) {
                                    if (!CrystalWaypoints.waypoints.containsKey(name)) {
                                        CrystalWaypoints.waypoints.put(name, sender.getPosition().add(0.5, 0.5, 0.5));
                                    } else {
                                        sender.addChatMessage(new ChatComponentText("[" + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "SkyBlockHud" + EnumChatFormatting.RESET + "] : " + EnumChatFormatting.GRAY + "Waypoint already exists!"));
                                    }
                                }
                                break;
                            case "remove":
                                if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.CRYSTALHOLLOWS)) {
                                    if (CrystalWaypoints.waypoints.containsKey(name)) {
                                        CrystalWaypoints.waypoints.remove(name);
                                    } else {
                                        sender.addChatMessage(new ChatComponentText("[" + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "SkyBlockHud" + EnumChatFormatting.RESET + "] : " + EnumChatFormatting.GRAY + "Waypoint doesnt exist!"));
                                    }
                                }
                                break;
                            case "clear":
                                CrystalWaypoints.waypoints.clear();
                                break;
                        }
                    }
                },
                new TabCompleteRunnable() {
                    @Override
                    public List<String> tabComplete(ICommandSender sender, String[] args, BlockPos pos) {
                        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
                            return getListOfStringsMatchingLastWord(args, waypoints.keySet());
                        }
                        if (args.length == 1) {
                            return getListOfStringsMatchingLastWord(args, Lists.newArrayList("add", "clear", "remove"));
                        }
                        return null;
                    }
                }
            );
        }
    }
}
