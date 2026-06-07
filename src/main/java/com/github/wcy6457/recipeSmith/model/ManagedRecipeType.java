package com.github.wcy6457.recipeSmith.model;

import java.util.Locale;

public enum ManagedRecipeType {
    SHAPED("shaped", "Shaped Crafting"),
    SHAPELESS("shapeless", "Shapeless Crafting"),
    FURNACE("furnace", "Furnace"),
    BLASTING("blasting", "Blasting"),
    SMOKING("smoking", "Smoking"),
    CAMPFIRE("campfire", "Campfire"),
    STONECUTTING("stonecutting", "Stonecutting"),
    SMITHING_TRANSFORM("smithing_transform", "Smithing Transform"),
    SMITHING_TRIM("smithing_trim", "Smithing Trim"),
    TRANSMUTE("transmute", "Transmute");

    private final String yamlName;
    private final String displayName;

    ManagedRecipeType(String yamlName, String displayName) {
        this.yamlName = yamlName;
        this.displayName = displayName;
    }

    public String yamlName() {
        return yamlName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isCooking() {
        return this == FURNACE || this == BLASTING || this == SMOKING || this == CAMPFIRE;
    }

    public boolean requiresResult() {
        return this != SMITHING_TRIM;
    }

    public static ManagedRecipeType fromYaml(String value) {
        if (value == null) {
            throw new IllegalArgumentException("type is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (ManagedRecipeType type : values()) {
            if (type.yamlName.equals(normalized) || type.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported recipe type: " + value);
    }
}
