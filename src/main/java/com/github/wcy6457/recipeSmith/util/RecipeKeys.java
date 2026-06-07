package com.github.wcy6457.recipeSmith.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

public final class RecipeKeys {
    public static final String NAMESPACE = "recipesmith";

    private RecipeKeys() {
    }

    public static NamespacedKey addKey(String id) {
        return new NamespacedKey(NAMESPACE, sanitizePath(id));
    }

    public static NamespacedKey replacementKey(NamespacedKey sourceKey) {
        return new NamespacedKey(NAMESPACE, "replacement/" + sourceKey.getNamespace() + "/" + sourceKey.getKey());
    }

    public static NamespacedKey parse(String value, Plugin plugin) {
        NamespacedKey key = NamespacedKey.fromString(value, plugin);
        if (key == null) {
            throw new IllegalArgumentException("Invalid namespaced key: " + value);
        }
        return key;
    }

    public static String sanitizePath(String value) {
        String sanitized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('\\', '/')
                .replaceAll("[^a-z0-9_./-]", "_")
                .replaceAll("/+", "/")
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        return sanitized;
    }

    public static String generatedId(String prefix) {
        return sanitizePath(prefix + "_" + Long.toString(System.currentTimeMillis(), 36));
    }
}
