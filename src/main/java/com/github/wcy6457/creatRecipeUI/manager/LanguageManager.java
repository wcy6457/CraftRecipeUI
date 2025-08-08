package com.github.wcy6457.creatRecipeUI.manager;

import com.github.wcy6457.creatRecipeUI.CreatRecipeUI;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final CreatRecipeUI plugin;
    private final Map<String, String> messages = new HashMap<>();
    private String currentLocale = "en_US";
    private final int expectedVersion = 1;

    public LanguageManager(CreatRecipeUI plugin) {
        this.plugin = plugin;
    }

    public void load(String locale) {
        ensureLangFileExists(locale);
        //提取语言文件

        this.currentLocale = locale;
        messages.clear();

        File langFile = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file not found: " + langFile.getName());
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);

        int fileVersion = config.getInt("version", -1);

        if (fileVersion < this.expectedVersion) {
            plugin.getLogger().warning("Language file version outdated: " + locale);

            if (langFile.renameTo(new File(langFile.getParent(), locale + ".yml.bak"))) {
                // 从 jar 中提取最新语言文件
                plugin.saveResource("lang/" + locale + ".yml", true);
                plugin.getLogger().info("Extracted the latest language file from the jar: " + locale + ".yml");
                config = YamlConfiguration.loadConfiguration(langFile);
            } else {
                plugin.getLogger().warning("Encountered an issue, unable to update the language file");
            }
        }

        for (String key : config.getKeys(true)) {
            if (!config.isConfigurationSection(key)) {
                messages.put(key, config.getString(key));
            }
        }

        plugin.getLogger().info("Loaded language: " + locale + " with " + messages.size() + " entries.");
    }

    /**
     *
     * @param key 用于查找语言文件中的文本条目，例如 "log.recipe_created"
     * @param args 可变参数，可自由传入任意个，按{0} {1} {2} ......自动替换文本条目中的内容
     * @return 返回格式化后的文本内容，如在lang文件中没有找到对应条目则返回默认的键值
     */
    public String get(String key, Object... args) {
        String raw = messages.getOrDefault(key, key);
        return MessageFormat.format(raw, args);
    }

    /**
     *
     * @return 当前设置的语言
     */
    public String getCurrentLocale() {
        return currentLocale;
    }

    public void reload(String lang) {
        this.load(lang);
    }

    private void ensureLangFileExists(String locale) {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        File langFile = new File(langDir, locale + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + locale + ".yml", false);
            plugin.getLogger().info("Extracted default language file: " + locale + ".yml");
        }
    }


}
