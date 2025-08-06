package com.github.wcy6457.creatRecipeUI.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

public class RecipeConfigLoader {

    private final FileConfiguration config;
    private final Plugin plugin;

    public RecipeConfigLoader(File dataFolder, Plugin plugin) {
        File file = new File(dataFolder, "recipes.yml");
        if (!file.exists()) {
            plugin.getLogger().warning("CreatRecipeUI找不到recipes.yml,将生成一个带示例的文件");
            plugin.saveResource("recipes.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        this.plugin = plugin;
    }

    public List<RecipeData> loadRecipes() {
        List<RecipeData> recipes = new ArrayList<>();
        if (!config.contains("recipes")) return recipes;

        for (String key : config.getConfigurationSection("recipes").getKeys(false)) {
            String path = "recipes." + key;
            Material result = Material.valueOf(config.getString(path + ".result"));
            List<String> shape = config.getStringList(path + ".shape");

            Map<Character, Material> ingredients = new HashMap<>();
            for (String symbol : config.getConfigurationSection(path + ".ingredients").getKeys(false)) {
                Material mat = Material.valueOf(config.getString(path + ".ingredients." + symbol));
                ingredients.put(symbol.charAt(0), mat);
            }

            recipes.add(new RecipeData(key, result, shape, ingredients));
        }

        plugin.getLogger().info("CreatRecipeUI从yml中加载了"+recipes.size()+"个配方数据");

        return recipes;
    }

    public static class RecipeData {
        public final String key;
        public final Material result;
        public final List<String> shape;
        public final Map<Character, Material> ingredients;

        public RecipeData(String key, Material result, List<String> shape, Map<Character, Material> ingredients) {
            this.key = key;
            this.result = result;
            this.shape = shape;
            this.ingredients = ingredients;
        }
    }
}
