package com.thatgravyboat.amod.commands;

import at.lorenz.mod.LorenzMod;
import com.thatgravyboat.amod.config.ConfigEditor;
import com.thatgravyboat.amod.core.GuiScreenElementWrapper;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;
import org.apache.commons.lang3.StringUtils;

public class Commands {

    private static final boolean devMode = false;

    private static final SimpleCommand.ProcessCommandRunnable settingsRunnable = new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length > 0) {
                LorenzMod.screenToOpen = new GuiScreenElementWrapper(new ConfigEditor(LorenzMod.feature, StringUtils.join(args, " ")));
            } else {
                LorenzMod.screenToOpen = new GuiScreenElementWrapper(new ConfigEditor(LorenzMod.feature));
            }
        }
    };

    private static final SimpleCommand settingsCommand = new SimpleCommand("lm", settingsRunnable);
    private static final SimpleCommand settingsCommand2 = new SimpleCommand("lorenzmod", settingsRunnable);

    public static void init() {
        ClientCommandHandler.instance.registerCommand(settingsCommand);
        ClientCommandHandler.instance.registerCommand(settingsCommand2);
    }
}
