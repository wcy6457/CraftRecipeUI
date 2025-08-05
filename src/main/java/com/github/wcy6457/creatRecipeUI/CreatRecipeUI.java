package com.github.wcy6457.creatRecipeUI;

import com.github.wcy6457.creatRecipeUI.rcipeManager.RecipeManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CreatRecipeUI extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        RecipeManager rm = new RecipeManager(this);
        rm.recipeRegister();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
