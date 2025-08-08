package com.github.wcy6457.creatRecipeUI.command;

import com.github.wcy6457.creatRecipeUI.CreatRecipeUI;
import com.github.wcy6457.creatRecipeUI.ui.Menu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandHandler implements CommandExecutor {

    private final CreatRecipeUI plugin;

    public CommandHandler(CreatRecipeUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.languageManager.get("player_info.command_not_player"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.languageManager.get("player_info.command_usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "menu" -> this.plugin.uiManager.viewFrame.open(Menu.class , player);
            case "lang" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.languageManager.get("player_info.command_lang"));
                    return true;
                }

                String lang = args[1].toLowerCase();

                if (lang.equals("zh_cn") || lang.equals("en_us")) {
                    plugin.getConfig().set("language", lang);
                    plugin.saveConfig();

                    plugin.languageManager.reload(lang);
                    player.sendMessage(plugin.languageManager.get("player_info.command_lang_success", lang));
                } else {
                    player.sendMessage(plugin.languageManager.get("player_info.command_lang_err", lang));
                }
            }
            default -> player.sendMessage(plugin.languageManager.get("player_info.command_error"));
        }

        return true;
    }
}
