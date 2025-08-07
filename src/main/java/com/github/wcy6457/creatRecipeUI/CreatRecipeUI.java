package com.github.wcy6457.creatRecipeUI;

import com.github.wcy6457.creatRecipeUI.manager.RecipeManager;
import com.github.wcy6457.creatRecipeUI.manager.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CreatRecipeUI extends JavaPlugin {

    LanguageManager languageManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getLogger().info("—————————————————————");
        Bukkit.getLogger().info("|CreatRecipeUI正在启动|");
        Bukkit.getLogger().info("—————————————————————");
        RecipeManager rm = new RecipeManager(this);
        //对象rm将从 resources/recipes.yml 加载配方
        rm.registerRecipe();

        String locale = getConfig().getString("default-language", "en_US");

        this.languageManager = new LanguageManager(this);
        this.languageManager.load(locale);

        getLogger().info(this.languageManager.get("log.recipe_created", "Iron Sword"));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
