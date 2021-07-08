package com.thatgravyboat.skyblockhud.commands;

import java.util.List;
import java.util.Set;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import org.apache.commons.lang3.ArrayUtils;

public abstract class SimpleSubCommand extends CommandBase {

    private final String commandName;
    private final Set<String> subCommands;

    public SimpleSubCommand(String commandName, Set<String> subCommands) {
        this.commandName = commandName;
        this.subCommands = subCommands;
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + commandName;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            processNoSubCommand(sender);
            return;
        }
        if (subCommands.contains(args[0])) {
            processSubCommand(sender, args[0], ArrayUtils.remove(args, 0));
            return;
        }
        processBadSubCommand(sender, args[0]);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, subCommands);
        }
        return null;
    }

    abstract void processSubCommand(ICommandSender sender, String subCommand, String[] args);

    abstract void processNoSubCommand(ICommandSender sender);

    public void processBadSubCommand(ICommandSender sender, String subCommand) {}
}
