package com.thatgravyboat.skyblockhud.commands;

import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.config.SBHConfigEditor;
import com.thatgravyboat.skyblockhud.core.GuiScreenElementWrapper;
import com.thatgravyboat.skyblockhud.handlers.MapHandler;
import com.thatgravyboat.skyblockhud.location.LocationHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;
import org.apache.commons.lang3.StringUtils;

public class Commands {

    private static final SimpleCommand.ProcessCommandRunnable settingsRunnable = new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length > 0) {
                SkyblockHud.screenToOpen =
                    new GuiScreenElementWrapper(new SBHConfigEditor(SkyblockHud.config, StringUtils.join(args, " ")));
            } else {
                SkyblockHud.screenToOpen = new GuiScreenElementWrapper(new SBHConfigEditor(SkyblockHud.config));
            }
        }
    };

    private static final SimpleCommand settingsCommand = new SimpleCommand("sbh", settingsRunnable);
    private static final SimpleCommand settingsCommand2 = new SimpleCommand("sbhsettings", settingsRunnable);
    private static final SimpleCommand settingsCommand3 = new SimpleCommand("sbhud", settingsRunnable);

    private static final SimpleCommand mapCommand = new SimpleCommand(
        "sbhmap",
        new SimpleCommand.ProcessCommandRunnable() {
            public void processCommand(ICommandSender sender, String[] args) {
                if (LocationHandler.getCurrentLocation().getCategory().getMap() != null) SkyblockHud.screenToOpen =
                    new MapHandler.MapScreen();
            }
        }
    );

    public static void init() {
        ClientCommandHandler.instance.registerCommand(settingsCommand);
        ClientCommandHandler.instance.registerCommand(settingsCommand2);
        ClientCommandHandler.instance.registerCommand(settingsCommand3);
        ClientCommandHandler.instance.registerCommand(mapCommand);
    }
}
