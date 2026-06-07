package com.github.wcy6457.recipeSmith.model;

import org.bukkit.inventory.ItemStack;

public record RecipeCatalogEntry(
        String id,
        RecipeOperation operation,
        ManagedRecipeType type,
        String key,
        String sourceKey,
        ItemStack icon,
        long revision,
        boolean invalid,
        String message
) {
    public ItemStack iconCopy() {
        return icon == null ? null : icon.clone();
    }
}
