package at.hannibal2.skyhanni.config.commands;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.config.config.ConfigEditor;
import at.hannibal2.skyhanni.config.core.GuiScreenElementWrapper;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;
import org.apache.commons.lang3.StringUtils;

public class Commands {

    private static final SimpleCommand.ProcessCommandRunnable mainMenu = new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length > 0) {
                SkyHanniMod.screenToOpen = new GuiScreenElementWrapper(new ConfigEditor(SkyHanniMod.feature, StringUtils.join(args, " ")));
            } else {
                SkyHanniMod.screenToOpen = new GuiScreenElementWrapper(new ConfigEditor(SkyHanniMod.feature));
            }
        }
    };

    public static void init() {
        ClientCommandHandler.instance.registerCommand(new SimpleCommand("sh", mainMenu));
        ClientCommandHandler.instance.registerCommand(new SimpleCommand("skyhanni", mainMenu));

        ClientCommandHandler.instance.registerCommand(new SimpleCommand("shreloadlocalrepo", new SimpleCommand.ProcessCommandRunnable() {
            public void processCommand(ICommandSender sender, String[] args) {
                SkyHanniMod.repo.reloadLocalRepo();
            }
        }));

        ClientCommandHandler.instance.registerCommand(new SimpleCommand("shupdaterepo", new SimpleCommand.ProcessCommandRunnable() {
            public void processCommand(ICommandSender sender, String[] args) {
                SkyHanniMod.repo.updateRepo();
            }
        }));
    }
}
