package com.thatgravyboat.skyblockhud.commands;

import com.google.common.collect.ImmutableSet;
import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.api.LeaderboardGetter;
import com.thatgravyboat.skyblockhud.config.SBHConfigEditor;
import com.thatgravyboat.skyblockhud.core.GuiScreenElementWrapper;
import com.thatgravyboat.skyblockhud.handlers.MapHandler;
import com.thatgravyboat.skyblockhud.location.LocationHandler;
import com.thatgravyboat.skyblockhud.playerstats.ActionBarParsing;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class Commands {

    private static boolean devMode = false;

    private static final SimpleCommand.ProcessCommandRunnable settingsRunnable = new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length > 0) {
                SkyblockHud.screenToOpen = new GuiScreenElementWrapper(new SBHConfigEditor(SkyblockHud.config, StringUtils.join(args, " ")));
            } else {
                SkyblockHud.screenToOpen = new GuiScreenElementWrapper(new SBHConfigEditor(SkyblockHud.config));
            }
        }
    };

    private static final SimpleSubCommand devCommand = new SimpleSubCommand("sbhdev", ImmutableSet.of("copyBossBar", "copyScoreboard", "copyActionBar")) {

        @Override
        void processSubCommand(ICommandSender sender, String subCommand, String[] args) {
            StringSelection clipboard = null;
            switch (subCommand) {
                case "copyBossBar":
                    clipboard = new StringSelection(BossStatus.bossName);
                    break;
                case "copyScoreboard":
                    StringBuilder builder = new StringBuilder();
                    LeaderboardGetter.getCachedScores().forEach(s -> builder.append(s).append("\n"));
                    clipboard = new StringSelection(builder.toString());
                    break;
                case "copyActionBar":
                    clipboard = new StringSelection(ActionBarParsing.lastLowActionBar);
                    break;
            }
            if (clipboard != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(clipboard, clipboard);
                sender.addChatMessage(new ChatComponentText(
                        "[" +
                        EnumChatFormatting.RED +
                        EnumChatFormatting.BOLD +
                        "SkyBlockHud" +
                        EnumChatFormatting.RESET +
                        "] : " +
                        EnumChatFormatting.GRAY +
                        "Info copied to clipboard!"
                ));
            }
        }

        @Override
        void processNoSubCommand(ICommandSender sender) {
            devMode = !devMode;
            sender.addChatMessage(new ChatComponentText("Dev Mode " + (devMode ? "Enabled" : "Disabled") + "!"));
        }
    };

    private static final SimpleCommand settingsCommand = new SimpleCommand("sbh", settingsRunnable);
    private static final SimpleCommand settingsCommand2 = new SimpleCommand("sbhsettings", settingsRunnable);
    private static final SimpleCommand settingsCommand3 = new SimpleCommand("sbhud", settingsRunnable);

    private static final SimpleCommand mapCommand = new SimpleCommand(
        "sbhmap",
        new SimpleCommand.ProcessCommandRunnable() {
            public void processCommand(ICommandSender sender, String[] args) {
                if (LocationHandler.getCurrentLocation().getCategory().getMap() != null && SkyblockHud.hasSkyblockScoreboard())
                    SkyblockHud.screenToOpen = new MapHandler.MapScreen();
            }
        }
    );

    public static void init() {
        ClientCommandHandler.instance.registerCommand(settingsCommand);
        ClientCommandHandler.instance.registerCommand(settingsCommand2);
        ClientCommandHandler.instance.registerCommand(settingsCommand3);
        ClientCommandHandler.instance.registerCommand(mapCommand);
        ClientCommandHandler.instance.registerCommand(devCommand);
    }
}
