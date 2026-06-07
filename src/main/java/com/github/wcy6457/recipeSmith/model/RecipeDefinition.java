package com.github.wcy6457.recipeSmith.model;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RecipeDefinition {
    public static final int DEFAULT_COOKING_TIME = 200;

    private String id;
    private RecipeOperation operation;
    private ManagedRecipeType type;
    private NamespacedKey sourceKey;
    private ItemStack result;
    private List<String> shape = new ArrayList<>(List.of("   ", "   ", "   "));
    private Map<Character, RecipeChoiceDefinition> shapedIngredients = new HashMap<>();
    private List<RecipeChoiceDefinition> shapelessIngredients = new ArrayList<>();
    private RecipeChoiceDefinition input;
    private RecipeChoiceDefinition material;
    private RecipeChoiceDefinition template;
    private RecipeChoiceDefinition base;
    private RecipeChoiceDefinition addition;
    private float experience;
    private int cookingTime = DEFAULT_COOKING_TIME;
    private String trimPattern = "minecraft:bolt";
    private boolean copyDataComponents = true;
    private long revision;
    private String invalidReason;

    public RecipeDefinition(String id, RecipeOperation operation, ManagedRecipeType type) {
        this.id = id;
        this.operation = operation;
        this.type = type;
    }

    public RecipeDefinition copy() {
        RecipeDefinition copy = new RecipeDefinition(id, operation, type);
        copy.sourceKey = sourceKey;
        copy.result = result == null ? null : result.clone();
        copy.shape = new ArrayList<>(shape);
        copy.shapedIngredients = new HashMap<>(shapedIngredients);
        copy.shapelessIngredients = new ArrayList<>(shapelessIngredients);
        copy.input = input;
        copy.material = material;
        copy.template = template;
        copy.base = base;
        copy.addition = addition;
        copy.experience = experience;
        copy.cookingTime = cookingTime;
        copy.trimPattern = trimPattern;
        copy.copyDataComponents = copyDataComponents;
        copy.revision = revision;
        copy.invalidReason = invalidReason;
        return copy;
    }

    public String id() {
        return id;
    }

    public void id(String id) {
        this.id = id;
    }

    public RecipeOperation operation() {
        return operation;
    }

    public void operation(RecipeOperation operation) {
        this.operation = operation;
    }

    public ManagedRecipeType type() {
        return type;
    }

    public void type(ManagedRecipeType type) {
        this.type = type;
    }

    public NamespacedKey sourceKey() {
        return sourceKey;
    }

    public void sourceKey(NamespacedKey sourceKey) {
        this.sourceKey = sourceKey;
    }

    public ItemStack result() {
        return result == null ? null : result.clone();
    }

    public void result(ItemStack result) {
        this.result = result == null ? null : result.clone();
    }

    public List<String> shape() {
        return List.copyOf(shape);
    }

    public void shape(List<String> shape) {
        this.shape = new ArrayList<>(shape);
    }

    public Map<Character, RecipeChoiceDefinition> shapedIngredients() {
        return Map.copyOf(shapedIngredients);
    }

    public void shapedIngredients(Map<Character, RecipeChoiceDefinition> shapedIngredients) {
        this.shapedIngredients = new HashMap<>(shapedIngredients);
    }

    public List<RecipeChoiceDefinition> shapelessIngredients() {
        return List.copyOf(shapelessIngredients);
    }

    public void shapelessIngredients(List<RecipeChoiceDefinition> shapelessIngredients) {
        this.shapelessIngredients = new ArrayList<>(shapelessIngredients);
    }

    public RecipeChoiceDefinition input() {
        return input;
    }

    public void input(RecipeChoiceDefinition input) {
        this.input = input;
    }

    public RecipeChoiceDefinition material() {
        return material;
    }

    public void material(RecipeChoiceDefinition material) {
        this.material = material;
    }

    public RecipeChoiceDefinition template() {
        return template;
    }

    public void template(RecipeChoiceDefinition template) {
        this.template = template;
    }

    public RecipeChoiceDefinition base() {
        return base;
    }

    public void base(RecipeChoiceDefinition base) {
        this.base = base;
    }

    public RecipeChoiceDefinition addition() {
        return addition;
    }

    public void addition(RecipeChoiceDefinition addition) {
        this.addition = addition;
    }

    public float experience() {
        return experience;
    }

    public void experience(float experience) {
        this.experience = Math.max(0, experience);
    }

    public int cookingTime() {
        return cookingTime;
    }

    public void cookingTime(int cookingTime) {
        this.cookingTime = Math.max(0, cookingTime);
    }

    public String trimPattern() {
        return trimPattern;
    }

    public void trimPattern(String trimPattern) {
        this.trimPattern = trimPattern == null || trimPattern.isBlank() ? "minecraft:bolt" : trimPattern;
    }

    public boolean copyDataComponents() {
        return copyDataComponents;
    }

    public void copyDataComponents(boolean copyDataComponents) {
        this.copyDataComponents = copyDataComponents;
    }

    public long revision() {
        return revision;
    }

    public void revision(long revision) {
        this.revision = revision;
    }

    public String invalidReason() {
        return invalidReason;
    }

    public void invalidReason(String invalidReason) {
        this.invalidReason = invalidReason;
    }

    public boolean isInvalid() {
        return invalidReason != null && !invalidReason.isBlank();
    }

    public String displayKey() {
        if (operation == RecipeOperation.ADD) {
            return "recipesmith:" + id;
        }
        if (sourceKey != null) {
            return sourceKey.toString();
        }
        return "recipesmith:" + id;
    }

    public void validateBasic() {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        if (!id.matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("id may only contain lowercase letters, numbers, _, -, ., /");
        }
        Objects.requireNonNull(operation, "operation is required");
        if (operation == RecipeOperation.DISABLE) {
            if (sourceKey == null) {
                throw new IllegalArgumentException("disable changes require source-key");
            }
            return;
        }
        Objects.requireNonNull(type, "type is required");
        if (operation == RecipeOperation.REPLACE && sourceKey == null) {
            throw new IllegalArgumentException("replace changes require source-key");
        }
        if (type.requiresResult() && (result == null || result.isEmpty())) {
            throw new IllegalArgumentException("result is required and cannot be air");
        }
    }
}
