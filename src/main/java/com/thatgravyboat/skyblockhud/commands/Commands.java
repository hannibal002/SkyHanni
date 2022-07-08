package com.thatgravyboat.skyblockhud.commands;

import at.lorenz.mod.LorenzMod;
import com.thatgravyboat.skyblockhud.config.SBHConfigEditor;
import com.thatgravyboat.skyblockhud.core.GuiScreenElementWrapper;
import com.thatgravyboat.skyblockhud.handlers.CrystalWaypoints;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;
import org.apache.commons.lang3.StringUtils;

public class Commands {

    private static final boolean devMode = false;

    private static final SimpleCommand.ProcessCommandRunnable settingsRunnable = new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length > 0) {
                LorenzMod.screenToOpen = new GuiScreenElementWrapper(new SBHConfigEditor(LorenzMod.feature, StringUtils.join(args, " ")));
            } else {
                LorenzMod.screenToOpen = new GuiScreenElementWrapper(new SBHConfigEditor(LorenzMod.feature));
            }
        }
    };

    //    private static final SimpleSubCommand devCommand = new SimpleSubCommand("sbhdev", ImmutableSet.of("copyNpcSkin", "copyBossBar", "copyScoreboard", "copyActionBar", "mobDeathLogging")) {
    //        @Override
    //        void processSubCommand(ICommandSender sender, String subCommand, String[] args) {
    //            StringSelection clipboard = null;
    //            switch (subCommand) {
    //                case "copyBossBar":
    //                    clipboard = new StringSelection(BossStatus.bossName);
    //                    break;
    //                case "copyScoreboard":
    //                    StringBuilder builder = new StringBuilder();
    //                    LeaderboardGetter.getCachedScores().forEach(s -> builder.append(s).append("\n"));
    //                    clipboard = new StringSelection(builder.toString());
    //                    break;
    //                case "copyActionBar":
    //                    clipboard = new StringSelection(ActionBarParsing.lastLowActionBar);
    //                    break;
    //                case "copySkin":
    //                    Entity entity = Minecraft.getMinecraft().objectMouseOver.entityHit;
    //                    if (entity instanceof AbstractClientPlayer) {
    //                        clipboard = new StringSelection("http://textures.minecraft.net/texture/" + ((AbstractClientPlayer) entity).getLocationSkin().getResourcePath().replace("skins/", ""));
    //                    } else {
    //                        sendSBHMessage(sender, "Not a player!");
    //                    }
    //                    break;
    //                case "mobDeathLogging":
    //                    DevModeConstants.mobDeathLogging = !DevModeConstants.mobDeathLogging;
    //                    sendSBHMessage(sender, "Mob Death Logging " + (DevModeConstants.mobDeathLogging ? "Enabled!" : "Disabled!"));
    //            }
    //            if (clipboard != null) {
    //                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(clipboard, clipboard);
    //                sendSBHMessage(sender, "Info copied to clipboard!");
    //            }
    //        }
    //
    //        @Override
    //        void processNoSubCommand(ICommandSender sender) {
    //            devMode = !devMode;
    //            sender.addChatMessage(new ChatComponentText("Dev Mode " + (devMode ? "Enabled!" : "Disabled!")));
    //        }
    //    };

    private static final SimpleCommand settingsCommand = new SimpleCommand("lm", settingsRunnable);
    private static final SimpleCommand settingsCommand2 = new SimpleCommand("lorenzmod", settingsRunnable);

    //    private static final SimpleCommand mapCommand = new SimpleCommand(
    //        "sbhmap",
    //        new SimpleCommand.ProcessCommandRunnable() {
    //            public void processCommand(ICommandSender sender, String[] args) {
    //                if (LocationHandler.getCurrentLocation().getCategory().getMap() != null && SkyblockHud.hasSkyblockScoreboard()) SkyblockHud.screenToOpen = new MapHandler.MapScreen();
    //            }
    //        }
    //    );

    public static void init() {
        ClientCommandHandler.instance.registerCommand(settingsCommand);
        ClientCommandHandler.instance.registerCommand(settingsCommand2);
        //        ClientCommandHandler.instance.registerCommand(mapCommand);
        //        ClientCommandHandler.instance.registerCommand(devCommand);
        ClientCommandHandler.instance.registerCommand(new CrystalWaypoints.WaypointCommand());
    }
    //    private static void sendSBHMessage(ICommandSender sender, String message) {
    //        sender.addChatMessage(new ChatComponentText("[" + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "SkyBlockHud" + EnumChatFormatting.RESET + "] : " + EnumChatFormatting.GRAY + message));
    //    }
}
