package com.github.wcy6457.creatRecipeUI.config;

import com.github.wcy6457.creatRecipeUI.manager.RecipeManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class RecipeConfigLoader {

    private final FileConfiguration config;
    private final JavaPlugin plugin;
    private final RecipeManager rm;

    public RecipeConfigLoader(File dataFolder, JavaPlugin plugin, RecipeManager rm) {
        File file = new File(dataFolder, "recipes.yml");

        if (!file.exists()) {
            plugin.getLogger().warning("CreatRecipeUI找不到recipes.yml,将生成一个带示例的文件");
            plugin.saveResource("recipes.yml", false);
        }

        this.config = YamlConfiguration.loadConfiguration(file);
        this.plugin = plugin;
        this.rm = rm;
    }

    public List<RecipeData> loadRecipes() {
        List<RecipeData> recipes = new ArrayList<>();
        if (!config.contains("recipes")){
            plugin.getLogger().warning("请检查recipes.yml文件！");
            return recipes;
        }

        for (String key : config.getConfigurationSection("recipes").getKeys(false)) {
            String path = "recipes." + key;

            // 使用 matchMaterial 支持命名空间ID
            String resultId = config.getString(path + ".result");

            Material result;
            try {
                result = Material.matchMaterial(resultId);
            } catch (Exception e) {
                plugin.getLogger().warning("无效的结果物品ID: " + resultId + "，位于：" + path + "，跳过配方: " + key);
                this.rm.err_sum++;
                continue;
            }

            List<String> shape = config.getStringList(path + ".shape");

            Map<Character, Material> ingredients = new HashMap<>();
            for (String symbol : config.getConfigurationSection(path + ".ingredients").getKeys(false)) {
                String ingredientId = config.getString(path + ".ingredients." + symbol);
                Material mat;
                try {
                    mat = Material.matchMaterial(ingredientId);
                } catch (Exception e) {
                    plugin.getLogger().warning("无效的原料物品ID: " + ingredientId + "，位于：" + path + "，符号: " + symbol + "，跳过配方: " + key);
                    this.rm.err_sum++;
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

