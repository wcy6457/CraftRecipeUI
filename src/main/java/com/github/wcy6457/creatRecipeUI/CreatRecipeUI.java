package com.github.wcy6457.creatRecipeUI;

import com.github.wcy6457.creatRecipeUI.rcipeManager.RecipeManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CreatRecipeUI extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        RecipeManager rm = new RecipeManager(this);
        //对象rm将从 resources/recipes.yml 加载配方
        rm.recipeRegister();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
