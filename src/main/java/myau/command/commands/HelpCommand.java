package myau.command.commands;

import myau.ATALOSS;
import myau.command.Command;
import myau.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class HelpCommand extends Command {
    public HelpCommand() {
        super(new ArrayList<>(Arrays.asList("help", "commands")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (!ATALOSS.moduleManager.modules.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sCommands:&r", ATALOSS.clientName));
            for (Command command : ATALOSS.commandManager.commands) {
                if (!(command instanceof ModuleCommand)) {
                    ChatUtil.sendFormatted(String.format("&7»&r .%s&r", String.join(" &7/&r .", command.names)));
                }
            }
        }
    }
}
