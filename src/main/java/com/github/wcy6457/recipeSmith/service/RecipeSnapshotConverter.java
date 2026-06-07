package com.github.wcy6457.recipeSmith.service;

import com.github.wcy6457.recipeSmith.model.ManagedRecipeType;
import com.github.wcy6457.recipeSmith.model.RecipeChoiceDefinition;
import com.github.wcy6457.recipeSmith.model.RecipeDefinition;
import com.github.wcy6457.recipeSmith.model.RecipeOperation;
import com.github.wcy6457.recipeSmith.util.RecipeKeys;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.CookingRecipe;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecipeSnapshotConverter {
    public RecipeDefinition fromRecipe(String id, Recipe recipe, RecipeOperation operation, NamespacedKey sourceKey) {
        ManagedRecipeType type = typeOf(recipe);
        if (type == null) {
            throw new IllegalArgumentException("This recipe type cannot be rebuilt by RecipeSmith");
        }
        RecipeDefinition definition = new RecipeDefinition(RecipeKeys.sanitizePath(id), operation, type);
        definition.sourceKey(sourceKey);
        if (type.requiresResult()) {
            definition.result(recipe.getResult());
        }
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            definition.shape(List.of(shapedRecipe.getShape()));
            LinkedHashMap<Character, RecipeChoiceDefinition> ingredients = new LinkedHashMap<>();
            for (Map.Entry<Character, RecipeChoice> entry : shapedRecipe.getChoiceMap().entrySet()) {
                RecipeChoiceDefinition choice = choice(entry.getValue());
                if (choice != null) {
                    ingredients.put(entry.getKey(), choice);
                }
            }
            definition.shapedIngredients(ingredients);
        } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            ArrayList<RecipeChoiceDefinition> ingredients = new ArrayList<>();
            for (RecipeChoice recipeChoice : shapelessRecipe.getChoiceList()) {
                RecipeChoiceDefinition choice = choice(recipeChoice);
                if (choice != null) {
                    ingredients.add(choice);
                }
            }
            definition.shapelessIngredients(ingredients);
        } else if (recipe instanceof CookingRecipe<?> cookingRecipe) {
            definition.input(choice(cookingRecipe.getInputChoice()));
            definition.experience(cookingRecipe.getExperience());
            definition.cookingTime(cookingRecipe.getCookingTime());
        } else if (recipe instanceof StonecuttingRecipe stonecuttingRecipe) {
            definition.input(choice(stonecuttingRecipe.getInputChoice()));
        } else if (recipe instanceof SmithingTransformRecipe smithingTransformRecipe) {
            definition.template(choice(smithingTransformRecipe.getTemplate()));
            definition.base(choice(smithingTransformRecipe.getBase()));
            definition.addition(choice(smithingTransformRecipe.getAddition()));
            definition.copyDataComponents(smithingTransformRecipe.willCopyDataComponents());
        } else if (recipe instanceof SmithingTrimRecipe smithingTrimRecipe) {
            definition.template(choice(smithingTrimRecipe.getTemplate()));
            definition.base(choice(smithingTrimRecipe.getBase()));
            definition.addition(choice(smithingTrimRecipe.getAddition()));
            definition.copyDataComponents(smithingTrimRecipe.willCopyDataComponents());
            definition.trimPattern(RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.TRIM_PATTERN)
                    .getKeyOrThrow(smithingTrimRecipe.getTrimPattern())
                    .toString());
        } else if (recipe instanceof TransmuteRecipe transmuteRecipe) {
            definition.input(choice(transmuteRecipe.getInput()));
            definition.material(choice(transmuteRecipe.getMaterial()));
        }
        definition.validateBasic();
        return definition;
    }

    public ManagedRecipeType typeOf(Recipe recipe) {
        if (recipe instanceof ShapedRecipe) {
            return ManagedRecipeType.SHAPED;
        }
        if (recipe instanceof ShapelessRecipe) {
            return ManagedRecipeType.SHAPELESS;
        }
        if (recipe instanceof FurnaceRecipe) {
            return ManagedRecipeType.FURNACE;
        }
        if (recipe instanceof BlastingRecipe) {
            return ManagedRecipeType.BLASTING;
        }
        if (recipe instanceof SmokingRecipe) {
            return ManagedRecipeType.SMOKING;
        }
        if (recipe instanceof CampfireRecipe) {
            return ManagedRecipeType.CAMPFIRE;
        }
        if (recipe instanceof StonecuttingRecipe) {
            return ManagedRecipeType.STONECUTTING;
        }
        if (recipe instanceof SmithingTransformRecipe) {
            return ManagedRecipeType.SMITHING_TRANSFORM;
        }
        if (recipe instanceof SmithingTrimRecipe) {
            return ManagedRecipeType.SMITHING_TRIM;
        }
        if (recipe instanceof TransmuteRecipe) {
            return ManagedRecipeType.TRANSMUTE;
        }
        return null;
    }

    public NamespacedKey keyOf(Recipe recipe) {
        if (recipe instanceof Keyed keyed) {
            return keyed.getKey();
        }
        return null;
    }

    private RecipeChoiceDefinition choice(RecipeChoice choice) {
        if (choice == null || choice == RecipeChoice.empty()) {
            return null;
        }
        try {
            if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
                List<ItemStack> choices = exactChoice.getChoices();
                return choices.isEmpty() ? null : new RecipeChoiceDefinition(choices.get(0));
            }
            return new RecipeChoiceDefinition(choice.getItemStack());
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
