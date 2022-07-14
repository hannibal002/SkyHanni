package at.lorenz.mod.config.commands;

import at.lorenz.mod.LorenzMod;
import at.lorenz.mod.config.config.ConfigEditor;
import at.lorenz.mod.config.core.GuiScreenElementWrapper;
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
