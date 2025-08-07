package com.github.wcy6457.creatRecipeUI;

import com.github.wcy6457.creatRecipeUI.rcipeManager.RecipeManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CreatRecipeUI extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getLogger().info("—————————————————————");
        Bukkit.getLogger().info("|CreatRecipeUI正在启动|");
        Bukkit.getLogger().info("—————————————————————");
        RecipeManager rm = new RecipeManager(this);
        //对象rm将从 resources/recipes.yml 加载配方
        rm.recipeRegister();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
