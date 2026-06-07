package com.github.wcy6457.recipeSmith.repository;

import com.github.wcy6457.recipeSmith.model.ManagedRecipeType;
import com.github.wcy6457.recipeSmith.model.RecipeChoiceDefinition;
import com.github.wcy6457.recipeSmith.model.RecipeDefinition;
import com.github.wcy6457.recipeSmith.model.RecipeOperation;
import com.github.wcy6457.recipeSmith.util.RecipeKeys;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;

public final class RecipeRepository {
    public static final int SCHEMA_VERSION = 1;

    private final Plugin plugin;
    private final File file;
    private final Executor ioExecutor;

    public RecipeRepository(Plugin plugin, Executor ioExecutor) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "recipes.yml");
        this.ioExecutor = ioExecutor;
    }

    public CompletableFuture<List<RecipeDefinition>> loadAsync() {
        return CompletableFuture.supplyAsync(this::loadNow, ioExecutor);
    }

    public CompletableFuture<Void> saveAsync(List<RecipeDefinition> definitions) {
        List<RecipeDefinition> snapshot = definitions.stream().map(RecipeDefinition::copy).toList();
        return CompletableFuture.runAsync(() -> saveNow(snapshot), ioExecutor);
    }

    private List<RecipeDefinition> loadNow() {
        ensureDefaultFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int schemaVersion = config.getInt("schema-version", SCHEMA_VERSION);
        if (schemaVersion != SCHEMA_VERSION) {
            plugin.getLogger().warning("Unsupported recipes.yml schema-version " + schemaVersion + "; attempting v1 parse.");
        }
        ArrayList<RecipeDefinition> definitions = new ArrayList<>();
        Object rawChanges = config.get("changes");
        if (rawChanges instanceof List<?> list) {
            int index = 0;
            for (Object raw : list) {
                definitions.add(readChange(raw, "change_" + index));
                index++;
            }
            return definitions;
        }
        ConfigurationSection section = config.getConfigurationSection("changes");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                definitions.add(readChange(section.get(key), key));
            }
        }
        return definitions;
    }

    private void saveNow(List<RecipeDefinition> definitions) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder: " + plugin.getDataFolder());
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("schema-version", SCHEMA_VERSION);
        ArrayList<Map<String, Object>> changes = new ArrayList<>();
        for (RecipeDefinition definition : definitions) {
            changes.add(writeChange(definition));
        }
        config.set("changes", changes);

        Path target = file.toPath();
        Path temp = target.resolveSibling(file.getName() + ".tmp");
        try {
            config.save(temp.toFile());
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailed) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save " + file, exception);
        }
    }

    private RecipeDefinition readChange(Object raw, String fallbackId) {
        try {
            Map<String, Object> map = asMap(raw);
            String id = string(map, "id", fallbackId);
            RecipeOperation operation = RecipeOperation.fromYaml(string(map, "operation", null));
            ManagedRecipeType type = map.containsKey("type") ? ManagedRecipeType.fromYaml(string(map, "type", null)) : ManagedRecipeType.SHAPED;
            RecipeDefinition definition = new RecipeDefinition(RecipeKeys.sanitizePath(id), operation, type);
            String sourceKey = string(map, "source-key", null);
            if (sourceKey != null && !sourceKey.isBlank()) {
                definition.sourceKey(RecipeKeys.parse(sourceKey, plugin));
            }
            definition.result(readOptionalItem(map.get("result")));
            if (map.containsKey("shape")) {
                definition.shape(readShape(map.get("shape")));
            }
            if (type == ManagedRecipeType.SHAPED) {
                definition.shapedIngredients(readShapedIngredients(map.get("ingredients")));
            } else if (type == ManagedRecipeType.SHAPELESS) {
                definition.shapelessIngredients(readChoiceList(map.get("ingredients")));
            }
            definition.input(readChoice(map.get("input")));
            definition.material(readChoice(map.get("material")));
            definition.template(readChoice(map.get("template")));
            definition.base(readChoice(map.get("base")));
            definition.addition(readChoice(map.get("addition")));
            definition.experience(number(map, "experience", 0).floatValue());
            definition.cookingTime(number(map, "cooking-time", RecipeDefinition.DEFAULT_COOKING_TIME).intValue());
            definition.trimPattern(string(map, "trim-pattern", "minecraft:bolt"));
            Object copyDataComponents = map.get("copy-data-components");
            if (copyDataComponents instanceof Boolean value) {
                definition.copyDataComponents(value);
            }
            try {
                definition.validateBasic();
            } catch (RuntimeException validationError) {
                definition.invalidReason(validationError.getMessage());
            }
            return definition;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Invalid RecipeSmith YAML entry " + fallbackId, exception);
            RecipeDefinition invalid = new RecipeDefinition(RecipeKeys.sanitizePath(fallbackId), RecipeOperation.ADD, ManagedRecipeType.SHAPED);
            invalid.invalidReason(exception.getMessage());
            return invalid;
        }
    }

    private Map<String, Object> writeChange(RecipeDefinition definition) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", definition.id());
        map.put("operation", definition.operation().yamlName());
        if (definition.operation() != RecipeOperation.DISABLE) {
            map.put("type", definition.type().yamlName());
        }
        if (definition.sourceKey() != null) {
            map.put("source-key", definition.sourceKey().toString());
        }
        if (definition.result() != null && !definition.result().isEmpty()) {
            map.put("result", definition.result());
        }
        if (definition.operation() != RecipeOperation.DISABLE) {
            if (definition.type() == ManagedRecipeType.SHAPED) {
                map.put("shape", definition.shape());
                LinkedHashMap<String, Object> ingredients = new LinkedHashMap<>();
                for (Map.Entry<Character, RecipeChoiceDefinition> entry : definition.shapedIngredients().entrySet()) {
                    ingredients.put(String.valueOf(entry.getKey()), entry.getValue().exactItem());
                }
                map.put("ingredients", ingredients);
            } else if (definition.type() == ManagedRecipeType.SHAPELESS) {
                map.put("ingredients", definition.shapelessIngredients().stream().map(RecipeChoiceDefinition::exactItem).toList());
            }
            putChoice(map, "input", definition.input());
            putChoice(map, "material", definition.material());
            putChoice(map, "template", definition.template());
            putChoice(map, "base", definition.base());
            putChoice(map, "addition", definition.addition());
            if (definition.type() != null && definition.type().isCooking()) {
                map.put("experience", definition.experience());
                map.put("cooking-time", definition.cookingTime());
            }
            if (definition.type() == ManagedRecipeType.SMITHING_TRIM) {
                map.put("trim-pattern", definition.trimPattern());
            }
            if (definition.type() == ManagedRecipeType.SMITHING_TRANSFORM || definition.type() == ManagedRecipeType.SMITHING_TRIM) {
                map.put("copy-data-components", definition.copyDataComponents());
            }
        }
        if (definition.isInvalid()) {
            map.put("invalid-note", definition.invalidReason());
        }
        return map;
    }

    private void ensureDefaultFile() {
        if (file.exists()) {
            return;
        }
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder: " + plugin.getDataFolder());
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("schema-version", SCHEMA_VERSION);
        config.set("changes", List.of());
        try {
            config.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create " + file, exception);
        }
    }

    private static void putChoice(Map<String, Object> map, String key, RecipeChoiceDefinition choice) {
        if (choice != null) {
            map.put(key, choice.exactItem());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object raw) {
        if (raw instanceof ConfigurationSection section) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            for (String key : section.getKeys(false)) {
                map.put(key, section.get(key));
            }
            return map;
        }
        if (raw instanceof Map<?, ?> source) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                map.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return map;
        }
        throw new IllegalArgumentException("Expected map, got " + raw);
    }

    private static String string(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static Number number(Map<String, Object> map, String key, Number defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number;
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static List<String> readShape(Object raw) {
        ArrayList<String> rows = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object value : list) {
                rows.add(String.valueOf(value));
            }
        }
        return rows.isEmpty() ? List.of("   ", "   ", "   ") : rows;
    }

    private static Map<Character, RecipeChoiceDefinition> readShapedIngredients(Object raw) {
        LinkedHashMap<Character, RecipeChoiceDefinition> result = new LinkedHashMap<>();
        if (raw == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : asMap(raw).entrySet()) {
            if (entry.getKey().isEmpty()) {
                continue;
            }
            RecipeChoiceDefinition choice = readChoice(entry.getValue());
            if (choice != null) {
                result.put(entry.getKey().charAt(0), choice);
            }
        }
        return result;
    }

    private static List<RecipeChoiceDefinition> readChoiceList(Object raw) {
        ArrayList<RecipeChoiceDefinition> result = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object value : list) {
                RecipeChoiceDefinition choice = readChoice(value);
                if (choice != null) {
                    result.add(choice);
                }
            }
        }
        return result;
    }

    private static RecipeChoiceDefinition readChoice(Object raw) {
        ItemStack item = readOptionalItem(raw);
        return item == null || item.isEmpty() ? null : new RecipeChoiceDefinition(item);
    }

    @SuppressWarnings("unchecked")
    private static ItemStack readOptionalItem(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof ItemStack itemStack) {
            return itemStack.clone();
        }
        if (raw instanceof Map<?, ?> rawMap) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                map.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return ItemStack.deserialize(map);
        }
        throw new IllegalArgumentException("Expected ItemStack, got " + raw.getClass().getSimpleName());
    }
}
