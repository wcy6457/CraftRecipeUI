package com.github.wcy6457.recipeSmith.i18n;

import com.github.wcy6457.recipeSmith.model.ManagedRecipeType;
import com.github.wcy6457.recipeSmith.model.RecipeOperation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

public final class LanguageService {
    public static final String DEFAULT_LANGUAGE = "zh_cn";
    private static final String FALLBACK_RESOURCE = "lang/" + DEFAULT_LANGUAGE + ".yml";

    private final JavaPlugin plugin;
    private YamlConfiguration activeLanguage = new YamlConfiguration();
    private YamlConfiguration fallbackLanguage = new YamlConfiguration();
    private String language = DEFAULT_LANGUAGE;

    public LanguageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        ensureFiles();
        plugin.reloadConfig();
        language = normalize(plugin.getConfig().getString("language", DEFAULT_LANGUAGE));
        fallbackLanguage = loadBundledLanguage(FALLBACK_RESOURCE);
        activeLanguage = YamlConfiguration.loadConfiguration(languageFile(language));
        if (activeLanguage.getKeys(true).isEmpty()) {
            plugin.getLogger().warning("Language pack " + language + " is empty; falling back to " + DEFAULT_LANGUAGE + ".");
            language = DEFAULT_LANGUAGE;
            activeLanguage = fallbackLanguage;
        }
    }

    public String language() {
        return language;
    }

    public Set<String> availableLanguages() {
        ensureFiles();
        TreeSet<String> languages = new TreeSet<>();
        File langDir = new File(plugin.getDataFolder(), "lang");
        File[] files = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                languages.add(file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT));
            }
        }
        return languages;
    }

    public boolean setLanguage(String requestedLanguage) {
        String normalized = normalize(requestedLanguage);
        File file = languageFile(normalized);
        if (!file.exists()) {
            return false;
        }
        plugin.getConfig().set("language", normalized);
        plugin.saveConfig();
        load();
        return true;
    }

    public Component component(String key, NamedTextColor color, Arg... args) {
        return Component.text(plain(key, args), color);
    }

    public Component component(String key, Arg... args) {
        return Component.text(plain(key, args));
    }

    public String plain(String key, Arg... args) {
        return apply(rawString(key), args);
    }

    public List<String> list(String key, Arg... args) {
        List<String> values = activeLanguage.getStringList(key);
        if (values.isEmpty()) {
            values = fallbackLanguage.getStringList(key);
        }
        if (values.isEmpty()) {
            return List.of();
        }
        ArrayList<String> translated = new ArrayList<>(values.size());
        for (String value : values) {
            translated.add(apply(value, args));
        }
        return translated;
    }

    public String typeName(ManagedRecipeType type) {
        return plain("type." + type.yamlName());
    }

    public String operationName(RecipeOperation operation) {
        return plain("operation." + operation.yamlName());
    }

    public static Arg arg(String key, Object value) {
        return new Arg(key, value == null ? "" : String.valueOf(value));
    }

    private void ensureFiles() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder: " + plugin.getDataFolder());
        }
        plugin.saveDefaultConfig();
        saveLanguageResource("lang/zh_cn.yml");
        saveLanguageResource("lang/en_us.yml");
    }

    private void saveLanguageResource(String resourcePath) {
        File target = new File(plugin.getDataFolder(), resourcePath);
        if (target.exists()) {
            return;
        }
        try {
            plugin.saveResource(resourcePath, false);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().log(Level.WARNING, "Missing bundled language resource " + resourcePath, exception);
        }
    }

    private YamlConfiguration loadBundledLanguage(String resourcePath) {
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource(resourcePath), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Could not load bundled language resource " + resourcePath, exception);
            return new YamlConfiguration();
        }
    }

    private String rawString(String key) {
        String value = activeLanguage.getString(key);
        if (value == null) {
            value = fallbackLanguage.getString(key);
        }
        return value == null ? key : value;
    }

    private String apply(String value, Arg... args) {
        String result = value;
        for (Arg arg : args) {
            result = result.replace("{" + arg.key() + "}", arg.value());
        }
        return result;
    }

    private File languageFile(String language) {
        return new File(plugin.getDataFolder(), "lang/" + normalize(language) + ".yml");
    }

    private String normalize(String language) {
        if (language == null || language.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        return language.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    public record Arg(String key, String value) {
    }
}
