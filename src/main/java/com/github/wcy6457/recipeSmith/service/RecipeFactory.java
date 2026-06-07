package com.github.wcy6457.recipeSmith.service;

import com.github.wcy6457.recipeSmith.model.ManagedRecipeType;
import com.github.wcy6457.recipeSmith.model.RecipeChoiceDefinition;
import com.github.wcy6457.recipeSmith.model.RecipeDefinition;
import com.github.wcy6457.recipeSmith.model.RecipeOperation;
import com.github.wcy6457.recipeSmith.util.RecipeKeys;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.SmithingTrimRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.TransmuteRecipe;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RecipeFactory {
    private static final char[] SHAPED_KEYS = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};

    public Recipe build(RecipeDefinition definition) {
        definition.validateBasic();
        if (definition.operation() == RecipeOperation.DISABLE) {
            throw new IllegalArgumentException("disable changes do not create a recipe");
        }
        NamespacedKey key = activeKey(definition);
        ManagedRecipeType type = definition.type();
        ItemStack result = definition.result();
        if (type == ManagedRecipeType.SHAPED) {
            return buildShaped(key, definition, result);
        }
        if (type == ManagedRecipeType.SHAPELESS) {
            return buildShapeless(key, definition, result);
        }
        if (type == ManagedRecipeType.FURNACE) {
            return new FurnaceRecipe(key, result, required(definition.input(), "input").toRecipeChoice(), definition.experience(), definition.cookingTime());
        }
        if (type == ManagedRecipeType.BLASTING) {
            return new BlastingRecipe(key, result, required(definition.input(), "input").toRecipeChoice(), definition.experience(), definition.cookingTime());
        }
        if (type == ManagedRecipeType.SMOKING) {
            return new SmokingRecipe(key, result, required(definition.input(), "input").toRecipeChoice(), definition.experience(), definition.cookingTime());
        }
        if (type == ManagedRecipeType.CAMPFIRE) {
            return new CampfireRecipe(key, result, required(definition.input(), "input").toRecipeChoice(), definition.experience(), definition.cookingTime());
        }
        if (type == ManagedRecipeType.STONECUTTING) {
            return new StonecuttingRecipe(key, result, required(definition.input(), "input").toRecipeChoice());
        }
        if (type == ManagedRecipeType.SMITHING_TRANSFORM) {
            return new SmithingTransformRecipe(
                    key,
                    result,
                    choiceOrEmpty(definition.template()),
                    choiceOrEmpty(required(definition.base(), "base")),
                    choiceOrEmpty(required(definition.addition(), "addition")),
                    definition.copyDataComponents()
            );
        }
        if (type == ManagedRecipeType.SMITHING_TRIM) {
            return new SmithingTrimRecipe(
                    key,
                    choiceOrEmpty(required(definition.template(), "template")),
                    choiceOrEmpty(required(definition.base(), "base")),
                    choiceOrEmpty(required(definition.addition(), "addition")),
                    trimPattern(definition.trimPattern()),
                    definition.copyDataComponents()
            );
        }
        if (type == ManagedRecipeType.TRANSMUTE) {
            Material resultType = result.getType();
            return new TransmuteRecipe(
                    key,
                    resultType,
                    required(definition.input(), "input").toRecipeChoice(),
                    required(definition.material(), "material").toRecipeChoice()
            );
        }
        throw new IllegalArgumentException("Unsupported recipe type: " + type);
    }

    public NamespacedKey activeKey(RecipeDefinition definition) {
        if (definition.operation() == RecipeOperation.REPLACE) {
            return RecipeKeys.replacementKey(definition.sourceKey());
        }
        return RecipeKeys.addKey(definition.id());
    }

    private Recipe buildShaped(NamespacedKey key, RecipeDefinition definition, ItemStack result) {
        List<String> shape = normalizeShape(definition.shape());
        Map<Character, RecipeChoiceDefinition> ingredients = definition.shapedIngredients();
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape.toArray(String[]::new));
        Set<Character> used = new HashSet<>();
        for (String row : shape) {
            for (char symbol : row.toCharArray()) {
                if (symbol != ' ') {
                    used.add(symbol);
                }
            }
        }
        if (used.isEmpty()) {
            throw new IllegalArgumentException("shaped recipe needs at least one ingredient");
        }
        for (char symbol : used) {
            RecipeChoiceDefinition choice = ingredients.get(symbol);
            if (choice == null) {
                throw new IllegalArgumentException("missing shaped ingredient " + symbol);
            }
            recipe.setIngredient(symbol, choice.toRecipeChoice());
        }
        return recipe;
    }

    private Recipe buildShapeless(NamespacedKey key, RecipeDefinition definition, ItemStack result) {
        List<RecipeChoiceDefinition> ingredients = definition.shapelessIngredients();
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("shapeless recipe needs at least one ingredient");
        }
        if (ingredients.size() > 9) {
            throw new IllegalArgumentException("shapeless recipe cannot have more than 9 ingredients");
        }
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        for (RecipeChoiceDefinition ingredient : ingredients) {
            recipe.addIngredient(ingredient.toRecipeChoice());
        }
        return recipe;
    }

    public static List<String> shapeFromGrid(ItemStack[] grid) {
        StringBuilder[] rows = {new StringBuilder(), new StringBuilder(), new StringBuilder()};
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int index = row * 3 + column;
                ItemStack item = grid[index];
                rows[row].append(item == null || item.isEmpty() ? ' ' : SHAPED_KEYS[index]);
            }
        }
        return List.of(rows[0].toString(), rows[1].toString(), rows[2].toString());
    }

    public static Map<Character, RecipeChoiceDefinition> ingredientsFromGrid(ItemStack[] grid) {
        java.util.LinkedHashMap<Character, RecipeChoiceDefinition> ingredients = new java.util.LinkedHashMap<>();
        for (int index = 0; index < Math.min(grid.length, SHAPED_KEYS.length); index++) {
            ItemStack item = grid[index];
            if (item != null && !item.isEmpty()) {
                ingredients.put(SHAPED_KEYS[index], new RecipeChoiceDefinition(item));
            }
        }
        return ingredients;
    }

    private static List<String> normalizeShape(List<String> rawShape) {
        if (rawShape == null || rawShape.isEmpty()) {
            throw new IllegalArgumentException("shape is required");
        }
        if (rawShape.size() > 3) {
            throw new IllegalArgumentException("shape cannot have more than 3 rows");
        }
        int width = -1;
        java.util.ArrayList<String> rows = new java.util.ArrayList<>();
        for (String rawRow : rawShape) {
            String row = rawRow == null ? "" : rawRow;
            if (row.isEmpty() || row.length() > 3) {
                throw new IllegalArgumentException("shape rows must be 1 to 3 characters");
            }
            if (width == -1) {
                width = row.length();
            }
            if (row.length() != width) {
                throw new IllegalArgumentException("shape must be rectangular");
            }
            rows.add(row);
        }
        return rows;
    }

    private static RecipeChoiceDefinition required(RecipeChoiceDefinition choice, String name) {
        if (choice == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return choice;
    }

    private static RecipeChoice choiceOrEmpty(RecipeChoiceDefinition choice) {
        return choice == null ? RecipeChoice.empty() : choice.toRecipeChoice();
    }

    private static TrimPattern trimPattern(String key) {
        String normalized = key == null ? "bolt" : key.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains(":")) {
            normalized = normalized.substring(normalized.indexOf(':') + 1);
        }
        return switch (normalized) {
            case "coast" -> TrimPattern.COAST;
            case "dune" -> TrimPattern.DUNE;
            case "eye" -> TrimPattern.EYE;
            case "flow" -> TrimPattern.FLOW;
            case "host" -> TrimPattern.HOST;
            case "raiser" -> TrimPattern.RAISER;
            case "rib" -> TrimPattern.RIB;
            case "sentry" -> TrimPattern.SENTRY;
            case "shaper" -> TrimPattern.SHAPER;
            case "silence" -> TrimPattern.SILENCE;
            case "snout" -> TrimPattern.SNOUT;
            case "spire" -> TrimPattern.SPIRE;
            case "tide" -> TrimPattern.TIDE;
            case "vex" -> TrimPattern.VEX;
            case "ward" -> TrimPattern.WARD;
            case "wayfinder" -> TrimPattern.WAYFINDER;
            case "wild" -> TrimPattern.WILD;
            default -> TrimPattern.BOLT;
        };
    }
}
