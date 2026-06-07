package com.github.wcy6457.recipeSmith.service;

import com.github.wcy6457.recipeSmith.model.ManagedRecipeType;
import com.github.wcy6457.recipeSmith.model.RecipeCatalog;
import com.github.wcy6457.recipeSmith.model.RecipeCatalogEntry;
import com.github.wcy6457.recipeSmith.model.RecipeDefinition;
import com.github.wcy6457.recipeSmith.model.RecipeOperation;
import com.github.wcy6457.recipeSmith.repository.RecipeRepository;
import com.github.wcy6457.recipeSmith.util.RecipeKeys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public final class RecipeService {
    public static final int APPLY_PER_TICK = 50;

    private final Plugin plugin;
    private final ExecutorService ioExecutor;
    private final RecipeRepository repository;
    private final RecipeFactory recipeFactory = new RecipeFactory();
    private final RecipeSnapshotConverter snapshotConverter = new RecipeSnapshotConverter();
    private final AtomicLong revisionCounter = new AtomicLong(1);
    private final Map<NamespacedKey, Recipe> originalRecipes = new HashMap<>();
    private final Set<NamespacedKey> activeManagedKeys = new HashSet<>();
    private final Object applyLock = new Object();

    private List<RecipeDefinition> definitions = List.of();
    private RecipeCatalog catalog = RecipeCatalog.empty();
    private CompletableFuture<Void> applyChain = CompletableFuture.completedFuture(null);

    public RecipeService(Plugin plugin) {
        this.plugin = plugin;
        this.ioExecutor = Executors.newFixedThreadPool(2, new ThreadFactory() {
            private int index = 1;

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "RecipeSmith-IO-" + index++);
                thread.setDaemon(true);
                return thread;
            }
        });
        this.repository = new RecipeRepository(plugin, ioExecutor);
    }

    public CompletableFuture<Void> reloadFromDisk() {
        return repository.loadAsync()
                .thenCompose(loaded -> queueApply(loaded, true))
                .exceptionally(exception -> {
                    plugin.getLogger().log(Level.SEVERE, "Could not reload recipes.yml", exception);
                    return null;
                });
    }

    public void shutdown() {
        runOnMain(this::restoreManagedRecipes);
        ioExecutor.shutdownNow();
    }

    public RecipeCatalog catalog() {
        return catalog;
    }

    public List<RecipeDefinition> definitions() {
        return definitions.stream().map(RecipeDefinition::copy).toList();
    }

    public Optional<RecipeDefinition> find(String id) {
        return definitions.stream()
                .filter(definition -> definition.id().equals(id))
                .findFirst()
                .map(RecipeDefinition::copy);
    }

    public RecipeSnapshotConverter snapshotConverter() {
        return snapshotConverter;
    }

    public CompletableFuture<SaveResult> saveChange(RecipeDefinition draft, long expectedRevision) {
        CompletableFuture<SaveResult> result = new CompletableFuture<>();
        runOnMain(() -> {
            try {
                RecipeDefinition next = draft.copy();
                next.id(RecipeKeys.sanitizePath(next.id()));
                next.invalidReason(null);
                next.validateBasic();
                if (next.operation() != RecipeOperation.ADD && Bukkit.getRecipe(next.sourceKey()) == null && !originalRecipes.containsKey(next.sourceKey())) {
                    result.complete(SaveResult.invalid("Source recipe does not exist: " + next.sourceKey()));
                    return;
                }
                if (next.operation() != RecipeOperation.DISABLE) {
                    recipeFactory.build(next);
                }

                ArrayList<RecipeDefinition> updated = new ArrayList<>(definitions.stream().map(RecipeDefinition::copy).toList());
                int existingIndex = findIndex(updated, next.id());
                if (existingIndex >= 0) {
                    RecipeDefinition existing = updated.get(existingIndex);
                    if (existing.revision() != expectedRevision) {
                        result.complete(SaveResult.conflict("This recipe changed while you were editing it."));
                        return;
                    }
                    updated.set(existingIndex, withNewRevision(next));
                } else {
                    if (expectedRevision != 0) {
                        result.complete(SaveResult.conflict("The original recipe entry no longer exists."));
                        return;
                    }
                    updated.add(withNewRevision(next));
                }

                String conflict = sourceConflict(updated);
                if (conflict != null) {
                    result.complete(SaveResult.conflict(conflict));
                    return;
                }

                queueApply(updated, false).thenRun(() -> repository.saveAsync(definitions)
                        .exceptionally(exception -> {
                            plugin.getLogger().log(Level.SEVERE, "Could not save recipes.yml", exception);
                            return null;
                        }));
                result.complete(SaveResult.saved("Saved " + next.id()));
            } catch (RuntimeException exception) {
                result.complete(SaveResult.invalid(exception.getMessage()));
            }
        });
        return result;
    }

    public CompletableFuture<SaveResult> deleteChange(String id, long expectedRevision) {
        CompletableFuture<SaveResult> result = new CompletableFuture<>();
        runOnMain(() -> {
            ArrayList<RecipeDefinition> updated = new ArrayList<>(definitions.stream().map(RecipeDefinition::copy).toList());
            int index = findIndex(updated, id);
            if (index < 0) {
                result.complete(SaveResult.conflict("That change no longer exists."));
                return;
            }
            if (updated.get(index).revision() != expectedRevision) {
                result.complete(SaveResult.conflict("This recipe changed while you were editing it."));
                return;
            }
            updated.remove(index);
            queueApply(updated, false).thenRun(() -> repository.saveAsync(definitions)
                    .exceptionally(exception -> {
                        plugin.getLogger().log(Level.SEVERE, "Could not save recipes.yml", exception);
                        return null;
                    }));
            result.complete(SaveResult.saved("Deleted " + id));
        });
        return result;
    }

    public RecipeDefinition createDraft(ManagedRecipeType type) {
        RecipeDefinition draft = new RecipeDefinition(RecipeKeys.generatedId(type.yamlName()), RecipeOperation.ADD, type);
        draft.cookingTime(defaultCookingTime(type));
        draft.result(new ItemStack(Material.STONE));
        if (type == ManagedRecipeType.SMITHING_TRIM) {
            draft.result(null);
        }
        return draft;
    }

    public RecipeDefinition createDisableDraft(NamespacedKey sourceKey) {
        RecipeDefinition draft = new RecipeDefinition(
                RecipeKeys.sanitizePath("disable/" + sourceKey.getNamespace() + "/" + sourceKey.getKey()),
                RecipeOperation.DISABLE,
                ManagedRecipeType.SHAPED
        );
        draft.sourceKey(sourceKey);
        return draft;
    }

    public RecipeDefinition createReplaceDraft(Recipe sourceRecipe) {
        NamespacedKey sourceKey = snapshotConverter.keyOf(sourceRecipe);
        if (sourceKey == null) {
            throw new IllegalArgumentException("Recipe has no key and cannot be replaced");
        }
        return snapshotConverter.fromRecipe(
                RecipeKeys.sanitizePath("replace/" + sourceKey.getNamespace() + "/" + sourceKey.getKey()),
                sourceRecipe,
                RecipeOperation.REPLACE,
                sourceKey
        );
    }

    public List<Recipe> recipesFor(ItemStack result) {
        if (result == null || result.isEmpty()) {
            return List.of();
        }
        return Bukkit.getRecipesFor(result);
    }

    private CompletableFuture<Void> queueApply(List<RecipeDefinition> incoming, boolean bumpAllRevisions) {
        synchronized (applyLock) {
            applyChain = applyChain.handle((ignored, throwable) -> null).thenCompose(ignored -> {
                CompletableFuture<Void> future = new CompletableFuture<>();
                runOnMain(() -> applyDefinitionsBatched(incoming, bumpAllRevisions, future));
                return future;
            });
            return applyChain;
        }
    }

    private void applyDefinitionsBatched(List<RecipeDefinition> incoming, boolean bumpAllRevisions, CompletableFuture<Void> future) {
        restoreManagedRecipes();
        List<RecipeDefinition> prepared = prepareDefinitions(incoming, bumpAllRevisions);
        this.definitions = prepared;
        applyBatch(prepared, 0, future);
    }

    private void applyBatch(List<RecipeDefinition> prepared, int start, CompletableFuture<Void> future) {
        int end = Math.min(prepared.size(), start + APPLY_PER_TICK);
        for (int index = start; index < end; index++) {
            RecipeDefinition definition = prepared.get(index);
            if (!definition.isInvalid()) {
                applyOne(definition);
            }
        }
        if (end < prepared.size()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> applyBatch(prepared, end, future), 1L);
            return;
        }
        refreshCatalog();
        future.complete(null);
    }

    private void applyOne(RecipeDefinition definition) {
        try {
            if (definition.operation() == RecipeOperation.DISABLE) {
                removeSource(definition.sourceKey());
                return;
            }
            if (definition.operation() == RecipeOperation.REPLACE) {
                removeSource(definition.sourceKey());
            }
            NamespacedKey activeKey = recipeFactory.activeKey(definition);
            Recipe existing = Bukkit.getRecipe(activeKey);
            if (existing != null && !activeManagedKeys.contains(activeKey)) {
                definition.invalidReason("Active key already exists: " + activeKey);
                plugin.getLogger().warning(definition.id() + " skipped: " + definition.invalidReason());
                return;
            }
            Recipe recipe = recipeFactory.build(definition);
            if (!Bukkit.addRecipe(recipe, true)) {
                definition.invalidReason("Bukkit rejected recipe " + activeKey);
                plugin.getLogger().warning(definition.id() + " skipped: " + definition.invalidReason());
                return;
            }
            activeManagedKeys.add(activeKey);
        } catch (RuntimeException exception) {
            definition.invalidReason(exception.getMessage());
            plugin.getLogger().log(Level.WARNING, "Could not apply RecipeSmith change " + definition.id(), exception);
        }
    }

    private void removeSource(NamespacedKey sourceKey) {
        if (sourceKey == null) {
            throw new IllegalArgumentException("source-key is required");
        }
        Recipe existing = Bukkit.getRecipe(sourceKey);
        if (existing == null && !originalRecipes.containsKey(sourceKey)) {
            throw new IllegalArgumentException("source recipe does not exist: " + sourceKey);
        }
        if (!originalRecipes.containsKey(sourceKey) && existing != null) {
            originalRecipes.put(sourceKey, existing);
        }
        Bukkit.removeRecipe(sourceKey, true);
    }

    private void restoreManagedRecipes() {
        for (NamespacedKey activeKey : List.copyOf(activeManagedKeys)) {
            Bukkit.removeRecipe(activeKey, true);
        }
        activeManagedKeys.clear();
        for (Map.Entry<NamespacedKey, Recipe> entry : originalRecipes.entrySet()) {
            if (Bukkit.getRecipe(entry.getKey()) == null) {
                Bukkit.addRecipe(entry.getValue(), true);
            }
        }
    }

    private List<RecipeDefinition> prepareDefinitions(List<RecipeDefinition> incoming, boolean bumpAllRevisions) {
        LinkedHashMap<String, RecipeDefinition> byId = new LinkedHashMap<>();
        HashSet<String> duplicateIds = new HashSet<>();
        for (RecipeDefinition definition : incoming) {
            RecipeDefinition copy = definition.copy();
            try {
                copy.id(RecipeKeys.sanitizePath(copy.id()));
            } catch (RuntimeException exception) {
                copy.invalidReason(exception.getMessage());
            }
            if (byId.containsKey(copy.id())) {
                duplicateIds.add(copy.id());
            }
            byId.putIfAbsent(copy.id(), copy);
        }
        Map<NamespacedKey, String> sourceOwners = new HashMap<>();
        ArrayList<RecipeDefinition> prepared = new ArrayList<>(byId.values());
        for (RecipeDefinition definition : prepared) {
            if (bumpAllRevisions || definition.revision() == 0) {
                definition.revision(revisionCounter.getAndIncrement());
            }
            if (duplicateIds.contains(definition.id())) {
                definition.invalidReason("duplicate id: " + definition.id());
                continue;
            }
            definition.invalidReason(null);
            try {
                definition.validateBasic();
                if (definition.sourceKey() != null) {
                    String previousOwner = sourceOwners.putIfAbsent(definition.sourceKey(), definition.id());
                    if (previousOwner != null && !previousOwner.equals(definition.id())) {
                        definition.invalidReason("source-key is already managed by " + previousOwner);
                    }
                }
            } catch (RuntimeException exception) {
                definition.invalidReason(exception.getMessage());
            }
        }
        return List.copyOf(prepared);
    }

    private void refreshCatalog() {
        ArrayList<RecipeCatalogEntry> entries = new ArrayList<>();
        for (RecipeDefinition definition : definitions) {
            entries.add(new RecipeCatalogEntry(
                    definition.id(),
                    definition.operation(),
                    definition.type(),
                    definition.displayKey(),
                    definition.sourceKey() == null ? null : definition.sourceKey().toString(),
                    icon(definition),
                    definition.revision(),
                    definition.isInvalid(),
                    definition.invalidReason()
            ));
        }
        catalog = new RecipeCatalog(entries, revisionCounter.get());
    }

    private ItemStack icon(RecipeDefinition definition) {
        if (definition.isInvalid()) {
            return new ItemStack(Material.BARRIER);
        }
        ItemStack result = definition.result();
        if (result != null && !result.isEmpty()) {
            return result;
        }
        if (definition.input() != null) {
            return definition.input().exactItem();
        }
        if (definition.template() != null) {
            return definition.template().exactItem();
        }
        return new ItemStack(Material.KNOWLEDGE_BOOK);
    }

    private RecipeDefinition withNewRevision(RecipeDefinition definition) {
        RecipeDefinition copy = definition.copy();
        copy.revision(revisionCounter.getAndIncrement());
        return copy;
    }

    private int findIndex(List<RecipeDefinition> definitions, String id) {
        for (int index = 0; index < definitions.size(); index++) {
            if (definitions.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private String sourceConflict(List<RecipeDefinition> definitions) {
        HashMap<NamespacedKey, String> owners = new HashMap<>();
        for (RecipeDefinition definition : definitions) {
            if (definition.sourceKey() == null) {
                continue;
            }
            String previous = owners.putIfAbsent(definition.sourceKey(), definition.id());
            if (previous != null && !previous.equals(definition.id())) {
                return definition.sourceKey() + " is already managed by " + previous;
            }
        }
        return null;
    }

    private int defaultCookingTime(ManagedRecipeType type) {
        if (type == ManagedRecipeType.BLASTING || type == ManagedRecipeType.SMOKING) {
            return 100;
        }
        if (type == ManagedRecipeType.CAMPFIRE) {
            return 600;
        }
        return RecipeDefinition.DEFAULT_COOKING_TIME;
    }

    private void runOnMain(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public record SaveResult(boolean success, boolean conflict, String message) {
        public static SaveResult saved(String message) {
            return new SaveResult(true, false, message);
        }

        public static SaveResult conflict(String message) {
            return new SaveResult(false, true, message);
        }

        public static SaveResult invalid(String message) {
            return new SaveResult(false, false, message);
        }
    }
}
