package com.github.wcy6457.creatRecipeUI.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class RecipeConfigLoader {

    private final FileConfiguration config;
    private final JavaPlugin plugin;

    public RecipeConfigLoader(File dataFolder, JavaPlugin plugin) {
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

            // 使用 matchMaterial 支持命名空间ID
            String resultId = config.getString(path + ".result");
            if(resultId == null){
                plugin.getLogger().warning("在"+path+"中未找到result，请检查是否配置错误");
            }
            Material result = Material.matchMaterial(resultId);
            if (result == null) {
                plugin.getLogger().warning("无效的结果物品ID: " + resultId + "，跳过配方: " + key);
                continue;
            }

            List<String> shape = config.getStringList(path + ".shape");

            Map<Character, Material> ingredients = new HashMap<>();
            for (String symbol : config.getConfigurationSection(path + ".ingredients").getKeys(false)) {
                String ingredientId = config.getString(path + ".ingredients." + symbol);
                Material mat = Material.matchMaterial(ingredientId);
                if (mat == null) {
                    plugin.getLogger().warning("无效的原料物品ID: " + ingredientId + "，符号: " + symbol + "，跳过配方: " + key);
                    continue;
                }
                ingredients.put(symbol.charAt(0), mat);
            }

            recipes.add(new RecipeData(key, result, shape, ingredients));
        }

        plugin.getLogger().info("CreatRecipeUI从yml中加载了" + recipes.size() + "个配方数据,即将向bukkit注册");

        return recipes;
    }

    public record RecipeData(String key, Material result, List<String> shape, Map<Character, Material> ingredients) {
    }
}

