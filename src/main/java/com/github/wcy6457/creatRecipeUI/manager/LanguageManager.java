package com.github.wcy6457.creatRecipeUI.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final JavaPlugin plugin;
    private final Map<String, String> messages = new HashMap<>();
    private String currentLocale = "en_US";

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(String locale) {
        this.currentLocale = locale;
        messages.clear();

        File langFile = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file not found: " + langFile.getName());
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);
        for (String key : config.getKeys(true)) {
            if (!config.isConfigurationSection(key)) {
                messages.put(key, config.getString(key));
            }
        }

        plugin.getLogger().info("Loaded language: " + locale + " with " + messages.size() + " entries.");
    }

    public String get(String key, Object... args) {
        String raw = messages.getOrDefault(key, key);
        return MessageFormat.format(raw, args);
    }

    public String getCurrentLocale() {
        return currentLocale;
    }
}
