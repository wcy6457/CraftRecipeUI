package com.github.wcy6457.creatRecipeUI;

import com.github.wcy6457.creatRecipeUI.command.CommandHandler;
import com.github.wcy6457.creatRecipeUI.command.CruTabCompleter;
import com.github.wcy6457.creatRecipeUI.manager.RecipeManager;
import com.github.wcy6457.creatRecipeUI.manager.LanguageManager;
import com.github.wcy6457.creatRecipeUI.manager.UIManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CreatRecipeUI extends JavaPlugin {

    public LanguageManager languageManager;
    public UIManager uiManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.languageManager = new LanguageManager(this);
        this.languageManager.load(getConfig().getString("language", "en_us"));

        Bukkit.getLogger().info("—————————————————————————————————————————");
        Bukkit.getLogger().info(this.languageManager.get("log.plugin_onEnable"));
        Bukkit.getLogger().info("—————————————————————————————————————————");


        RecipeManager rm = new RecipeManager(this);
        //对象rm将从 resources/recipes.yml 加载配方
        rm.registerRecipe();

        this.uiManager = new UIManager(this);
        this.uiManager.registerUI();

        PluginCommand cruCommand = getCommand("cru");
        if (cruCommand != null) {
            cruCommand.setExecutor(new CommandHandler(this));
            cruCommand.setTabCompleter(new CruTabCompleter());
        }
        //注册命令处理器
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
