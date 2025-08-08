package com.github.wcy6457.creatRecipeUI.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CruTabCompleter implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("menu", "lang");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("lang")) {
            return Arrays.asList("zh_cn", "en_us");
        }

        return Collections.emptyList();
    }
}
