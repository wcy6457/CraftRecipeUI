package com.github.wcy6457.creatRecipeUI;

import com.github.wcy6457.creatRecipeUI.manager.RecipeManager;
import com.github.wcy6457.creatRecipeUI.manager.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CreatRecipeUI extends JavaPlugin {

    public LanguageManager languageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.languageManager = new LanguageManager(this);
        this.languageManager.load(getConfig().getString("language", "en_US"));

        Bukkit.getLogger().info("—————————————————————");
        Bukkit.getLogger().info(this.languageManager.get("log.plugin_onEnable"));
        Bukkit.getLogger().info("—————————————————————");
        RecipeManager rm = new RecipeManager(this);
        //对象rm将从 resources/recipes.yml 加载配方
        rm.registerRecipe();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
