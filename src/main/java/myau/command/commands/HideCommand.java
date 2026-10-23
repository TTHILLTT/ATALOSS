package myau.command.commands;

import myau.ATALOSS;
import myau.command.Command;
import myau.module.Module;
import myau.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class HideCommand extends Command {
    public HideCommand() {
        super(new ArrayList<>(Arrays.asList("hide", "h")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            ChatUtil.sendFormatted(
                    String.format("%sUsage: .%s <&omodule&r>&r", ATALOSS.clientName, args.get(0).toLowerCase(Locale.ROOT))
            );
        } else if (!args.get(1).equals("*")) {
            Module module = ATALOSS.moduleManager.getModule(args.get(1));
            if (module == null) {
                ChatUtil.sendFormatted(String.format("%sModule &o%s&r not found&r", ATALOSS.clientName, args.get(1)));
            } else if (module.isHidden()) {
                ChatUtil.sendFormatted(String.format("%s&o%s&r is already hidden in HUD&r", ATALOSS.clientName, module.getName()));
            } else {
                module.setHidden(true);
                ChatUtil.sendFormatted(String.format("%s&o%s&r has been hidden in HUD&r", ATALOSS.clientName, module.getName()));
            }
        } else {
            for (Module module : ATALOSS.moduleManager.modules.values()) {
                module.setHidden(true);
            }
            ChatUtil.sendFormatted(String.format("%sAll modules have been hidden in HUD&r", ATALOSS.clientName));
        }
    }
}
