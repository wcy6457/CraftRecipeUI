package com.github.wcy6457.creatRecipeUI.rcipeManager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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
        registerTestSword();
    }

    private void registerTestSword() {
        ItemStack result = new ItemStack(Material.DIAMOND_SWORD);
        NamespacedKey key = new NamespacedKey(plugin, "test_sword");
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        recipe.shape(" D ", " D ", " S ");
        recipe.setIngredient('D', Material.DIRT);
        recipe.setIngredient('S', Material.STICK);

        Bukkit.addRecipe(recipe);
    }
}