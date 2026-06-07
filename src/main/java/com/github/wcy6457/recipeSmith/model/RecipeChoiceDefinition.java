package com.github.wcy6457.recipeSmith.model;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

public final class RecipeChoiceDefinition {
    private final ItemStack exactItem;

    public RecipeChoiceDefinition(ItemStack exactItem) {
        if (exactItem == null || exactItem.isEmpty()) {
            throw new IllegalArgumentException("Exact ingredient cannot be empty");
        }
        this.exactItem = normalized(exactItem);
    }

    public ItemStack exactItem() {
        return exactItem.clone();
    }

    public RecipeChoice toRecipeChoice() {
        return new RecipeChoice.ExactChoice(exactItem.clone());
    }

    public static ItemStack normalized(ItemStack item) {
        ItemStack copy = item.clone();
        copy.setAmount(1);
        return copy;
    }
}
