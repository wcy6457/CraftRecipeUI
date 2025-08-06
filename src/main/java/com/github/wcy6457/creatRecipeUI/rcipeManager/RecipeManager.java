package com.github.wcy6457.creatRecipeUI.rcipeManager;

import com.github.wcy6457.creatRecipeUI.config.RecipeConfigLoader;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class RecipeManager {

    private final JavaPlugin plugin;

    public RecipeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void recipeRegister() {
        RecipeConfigLoader loader = new RecipeConfigLoader(plugin.getDataFolder(),plugin);
        for (RecipeConfigLoader.RecipeData data : loader.loadRecipes()) {// loader 实现了从 yml 文件中加载配方
            /*
            填充即将注册的配方
            */
            NamespacedKey key = new NamespacedKey(plugin, data.key);
            ItemStack result = new ItemStack(data.result);
            ShapedRecipe recipe = new ShapedRecipe(key, result);

            recipe.shape(data.shape.toArray(new String[0]));
            data.ingredients.forEach(recipe::setIngredient);


            Bukkit.addRecipe(recipe);
            //向bukkit注册配方
        }
    }
}
