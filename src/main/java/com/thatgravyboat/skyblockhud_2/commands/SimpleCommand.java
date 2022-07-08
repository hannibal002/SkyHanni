package com.thatgravyboat.skyblockhud_2.commands;

import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

/**
 @author Moulberry
 **/
public class SimpleCommand extends CommandBase {

    private final String commandName;
    private final ProcessCommandRunnable runnable;
    private TabCompleteRunnable tabRunnable;

    public SimpleCommand(String commandName, ProcessCommandRunnable runnable) {
        this.commandName = commandName;
        this.runnable = runnable;
    }

    public SimpleCommand(String commandName, ProcessCommandRunnable runnable, TabCompleteRunnable tabRunnable) {
        this.commandName = commandName;
        this.runnable = runnable;
        this.tabRunnable = tabRunnable;
    }

    public abstract static class ProcessCommandRunnable {

        public abstract void processCommand(ICommandSender sender, String[] args);
    }

    public abstract static class TabCompleteRunnable {

        public abstract List<String> tabComplete(ICommandSender sender, String[] args, BlockPos pos);
    }

    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getCommandUsage(ICommandSender sender) {
        return "/" + commandName;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        runnable.processCommand(sender, args);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (tabRunnable != null) return tabRunnable.tabComplete(sender, args, pos);
        return null;
    }
}
