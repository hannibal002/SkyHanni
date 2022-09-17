package at.hannibal2.skyhanni.config.commands;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.config.ConfigEditor;
import at.hannibal2.skyhanni.config.core.GuiScreenElementWrapper;
import at.hannibal2.skyhanni.features.PlayerMarker;
import at.hannibal2.skyhanni.test.LorenzTest;
import at.hannibal2.skyhanni.test.command.CopyItemCommand;
import at.hannibal2.skyhanni.test.command.CopyNearbyEntitiesCommand;
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

        ClientCommandHandler.instance.registerCommand(
                new SimpleCommand(
                        "shreloadlocalrepo",
                        new SimpleCommand.ProcessCommandRunnable() {
                            public void processCommand(ICommandSender sender, String[] args) {
                                SkyHanniMod.repo.reloadLocalRepo();
                            }
                        }
                )
        );

        ClientCommandHandler.instance.registerCommand(
                new SimpleCommand(
                        "shupdaterepo",
                        new SimpleCommand.ProcessCommandRunnable() {
                            public void processCommand(ICommandSender sender, String[] args) {
                                SkyHanniMod.repo.updateRepo();
                            }
                        }
                )
        );
        ClientCommandHandler.instance.registerCommand(
                new SimpleCommand(
                        "testhanni",
                        new SimpleCommand.ProcessCommandRunnable() {
                            public void processCommand(ICommandSender sender, String[] args) {
                                LorenzTest.Companion.testCommand(args);
                            }
                        }
                )
        );
        ClientCommandHandler.instance.registerCommand(
                new SimpleCommand(
                        "copyentities",
                        new SimpleCommand.ProcessCommandRunnable() {
                            public void processCommand(ICommandSender sender, String[] args) {
                                CopyNearbyEntitiesCommand.INSTANCE.command(args);
                            }
                        }
                )
        );
        ClientCommandHandler.instance.registerCommand(
                new SimpleCommand(
                        "copyitem",
                        new SimpleCommand.ProcessCommandRunnable() {
                            public void processCommand(ICommandSender sender, String[] args) {
                                CopyItemCommand.INSTANCE.command(args);
                            }
                        }
                )
        );
        ClientCommandHandler.instance.registerCommand(
                new SimpleCommand(
                        "shconfigsave",
                        new SimpleCommand.ProcessCommandRunnable() {
                            public void processCommand(ICommandSender sender, String[] args) {
                                SkyHanniMod.configManager.saveConfig();
                            }
                        }
                )
        );
        ClientCommandHandler.instance.registerCommand(
                new SimpleCommand(
                        "shmarkplayer",
                        new SimpleCommand.ProcessCommandRunnable() {
                            public void processCommand(ICommandSender sender, String[] args) {
                                PlayerMarker.Companion.command(args);
                            }
                        }
                )
        );
        ClientCommandHandler.instance.registerCommand(
                new SimpleCommand(
                        "togglepacketlog",
                        new SimpleCommand.ProcessCommandRunnable() {
                            public void processCommand(ICommandSender sender, String[] args) {
                                LorenzTest.Companion.togglePacketLog();
                            }
                        }
                )
        );
        ClientCommandHandler.instance.registerCommand(
                new SimpleCommand(
                        "shreloadlisteners",
                        new SimpleCommand.ProcessCommandRunnable() {
                            public void processCommand(ICommandSender sender, String[] args) {
                                LorenzTest.Companion.reloadListeners();
                            }
                        }
                )
        );
    }
}
