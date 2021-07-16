package com.thatgravyboat.skyblockhud.handlers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.Utils;
import com.thatgravyboat.skyblockhud.api.events.LocationChangeEvent;
import com.thatgravyboat.skyblockhud.commands.SimpleCommand;
import com.thatgravyboat.skyblockhud.location.LocationCategory;
import com.thatgravyboat.skyblockhud.location.LocationHandler;
import com.thatgravyboat.skyblockhud.location.Locations;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CrystalWaypoints {

    public static final Pattern LOCATION_MESSAGE_REGEX = Pattern.compile("\\{(.*) : \\((-?\\d+)/(-?\\d+)/(-?\\d+)\\)}");

    public static final HashMap<String, BlockPos> waypoints = new HashMap<>();

    private static final Set<Locations> IMPORTANT_WAYPOINTS = Sets.newHashSet(Locations.GOBLINQUEENSDEN, Locations.LOSTPRECURSORCITY, Locations.JUNGLETEMPLE, Locations.MINESOFDIVAN, Locations.KHAZADDM, Locations.FAIRYGROTTO);

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        waypoints.forEach((text, pos) -> Utils.renderWaypointText(text, pos, event.partialTicks));
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
    }

    @SubscribeEvent
    public void onWorldChange(EntityJoinWorldEvent event) {
        if (event.entity == Minecraft.getMinecraft().thePlayer) {
            waypoints.clear();
        }
    }

    @SubscribeEvent
    public void onLocationChange(LocationChangeEvent event) {
        if (!event.newLocation.getCategory().equals(LocationCategory.CRYSTALHOLLOWS)) {
            waypoints.clear();
        } else if (!waypoints.containsKey("Crystal Nucleus") && SkyblockHud.config.mining.autoWaypoint) {
            waypoints.put("Crystal Nucleus", new BlockPos(512.5, 106.5, 512.5));
        }
        if (IMPORTANT_WAYPOINTS.contains(event.newLocation) && SkyblockHud.config.mining.autoWaypoint) {
            if (!waypoints.containsKey(event.newLocation.getDisplayName())) {
                waypoints.put(event.newLocation.getDisplayName(), Minecraft.getMinecraft().thePlayer.getPosition());
            }
        }
    }

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event){
        if (event.type != 2 && LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.CRYSTALHOLLOWS)) {
            Matcher matcher = LOCATION_MESSAGE_REGEX.matcher(event.message.getUnformattedText());
            if (!matcher.find()) return;
            ChatStyle style = new ChatStyle();
            style.setParentStyle(event.message.getChatStyle());
            ClickEvent.Action action = SkyblockHud.config.mining.chatWaypointMode == 0 ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND;
            style.setChatClickEvent(new ClickEvent(
                    action,
                    "/sbhpoints addat " + matcher.group(2) + " " + matcher.group(3) + " " + matcher.group(4) + " " + matcher.group(1)
            ));
            style.setChatHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ChatComponentText("Click to add waypoint!").setChatStyle(new ChatStyle().setBold(true))
            ));
            event.message.setChatStyle(style);
        }
    }

    private static String copyWayPoint(String name){
        BlockPos pos = waypoints.get(name);
        if (pos == null) {
            return null;
        }
        return "{" + name + " : (" + pos.getX() + "/" + pos.getY() + "/" + pos.getZ() + ")}";
    }

    public static class WaypointCommand extends SimpleCommand {

        public WaypointCommand() {
            super(
                "sbhpoints",
                new ProcessCommandRunnable() {
                    @Override
                    public void processCommand(ICommandSender sender, String[] args) {
                        if (args.length == 0) return;
                        String subCommand = args[0].toLowerCase();
                        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                        switch (subCommand) {
                            case "add":
                                if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.CRYSTALHOLLOWS)) {
                                    if (!CrystalWaypoints.waypoints.containsKey(name) && name.length() > 1) {
                                        CrystalWaypoints.waypoints.put(name, sender.getPosition().add(0.5, 0.5, 0.5));
                                    } else if (name.length() < 2) {
                                        sbhMessage(sender, "Waypoint name needs to be longer than 1");
                                    } else {
                                        sbhMessage(sender, "Waypoint already exists!");
                                    }
                                }
                                break;

                            case "remove":
                                if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.CRYSTALHOLLOWS)) {
                                    if (CrystalWaypoints.waypoints.containsKey(name)) {
                                        CrystalWaypoints.waypoints.remove(name);
                                    } else {
                                        sbhMessage(sender, "Waypoint doesnt exist!");
                                    }
                                }
                                break;

                            case "move":
                                if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.CRYSTALHOLLOWS)) {
                                    if (CrystalWaypoints.waypoints.containsKey(name)) {
                                        CrystalWaypoints.waypoints.put(name, sender.getPosition().add(0.5, 0.5, 0.5));
                                    } else {
                                        sbhMessage(sender, "Waypoint doesnt exist!");
                                    }
                                }
                                break;

                            case "clear":
                                CrystalWaypoints.waypoints.clear();
                                break;

                            case "addat":
                                if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.CRYSTALHOLLOWS)) {
                                    name = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                                    try {
                                        if (!CrystalWaypoints.waypoints.containsKey(name)) {
                                            CrystalWaypoints.waypoints.put(name, parseBlockPos(sender, args, 1, true));
                                        } else if (name.length() < 2) {
                                            sbhMessage(sender, "Waypoint name needs to be longer than 1");
                                        } else {
                                            sbhMessage(sender, "Waypoint already exists!");
                                        }
                                    } catch (Exception e) {
                                        sbhMessage(sender, "Error!");
                                    }
                                }
                                break;

                            case "copy":
                                String copyText = copyWayPoint(name);
                                if (copyText == null) {
                                    sbhMessage(sender, "No waypoint with that name!");
                                    break;
                                }
                                StringSelection clipboard = new StringSelection(copyText);
                                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(clipboard, clipboard);
                                break;

                            case "send":
                                String sendText = copyWayPoint(name);
                                if (sendText == null) {
                                    sbhMessage(sender, "No waypoint with that name!");
                                    break;
                                }
                                Minecraft.getMinecraft().thePlayer.sendChatMessage(sendText);
                                break;
                        }
                    }
                },
                new TabCompleteRunnable() {
                    @Override
                    public List<String> tabComplete(ICommandSender sender, String[] args, BlockPos pos) {
                        if (args.length == 2 && Utils.equalsIgnoreCaseAnyOf(args[0], "remove", "copy", "move", "send")) {
                            return getListOfStringsMatchingLastWord(args, waypoints.keySet());
                        }
                        if (args.length == 1) {
                            return getListOfStringsMatchingLastWord(args, Lists.newArrayList("add", "clear", "remove", "copy", "addat", "move", "send"));
                        }
                        if (args.length > 1 && args[0].equalsIgnoreCase("addat")) {
                            return func_175771_a(args, 1, pos);
                        }
                        return null;
                    }
                }
            );
        }

        private static void sbhMessage(ICommandSender sender, String message) {
            sender.addChatMessage(new ChatComponentText("[" + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "SkyBlockHud" + EnumChatFormatting.RESET + "] : " + EnumChatFormatting.GRAY + message));
        }
    }
}
