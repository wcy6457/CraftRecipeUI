package com.github.wcy6457.creatRecipeUI.command;

import com.github.wcy6457.creatRecipeUI.CreatRecipeUI;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {

    private final CreatRecipeUI plugin;

    public CommandHandler(CreatRecipeUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.languageManager.get("player_info.command_not_player"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.languageManager.get("player_info.command_usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "menu" -> {
                //TODO
                player.sendMessage("菜单功能尚未实现。");
            }
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
