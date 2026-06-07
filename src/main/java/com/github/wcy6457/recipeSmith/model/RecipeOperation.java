package com.github.wcy6457.recipeSmith.model;

import java.util.Locale;

public enum RecipeOperation {
    ADD("add"),
    REPLACE("replace"),
    DISABLE("disable");

    private final String yamlName;

    RecipeOperation(String yamlName) {
        this.yamlName = yamlName;
    }

    public String yamlName() {
        return yamlName;
    }

    public static RecipeOperation fromYaml(String value) {
        if (value == null) {
            throw new IllegalArgumentException("operation is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (RecipeOperation operation : values()) {
            if (operation.yamlName.equals(normalized)) {
                return operation;
            }
        }
        throw new IllegalArgumentException("Unsupported operation: " + value);
    }
}
