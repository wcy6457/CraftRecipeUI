package com.github.wcy6457.creatRecipeUI.rcipeManager;

import com.github.wcy6457.creatRecipeUI.config.RecipeConfigLoader;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class RecipeManager {

    private final JavaPlugin plugin;
    public int sum = 0;             //成功注册计数
    public int err_sum = 0;         //注册失败计数

    public RecipeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void recipeRegister() {
        RecipeConfigLoader loader = new RecipeConfigLoader(plugin.getDataFolder(),plugin);
        for (RecipeConfigLoader.RecipeData data : loader.loadRecipes()) {// loader 实现了从 yml 文件中加载配方
            /*
            填充即将注册的配方
            */
            NamespacedKey key = new NamespacedKey(plugin, data.key());
            ItemStack result = new ItemStack(data.result());
            ShapedRecipe recipe = new ShapedRecipe(key, result);

            recipe.shape(data.shape().toArray(new String[0]));
            data.ingredients().forEach(recipe::setIngredient);


            try {
                Bukkit.addRecipe(recipe);
            } catch (Exception e) {
                err_sum++;
                continue;
            }
            sum++;
            //向bukkit注册配方,同时计数。
        }

        if (err_sum != 0) {
            plugin.getLogger().warning("共有"+err_sum+"个配方加载失败");
        }

        plugin.getLogger().info("共有"+sum+"个配方加载成功");
    }
}
