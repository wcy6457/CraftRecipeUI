package com.github.wcy6457.recipeSmith.gui;

import com.github.wcy6457.recipeSmith.model.AdminRecipeSession;
import com.github.wcy6457.recipeSmith.model.ManagedRecipeType;
import com.github.wcy6457.recipeSmith.model.RecipeCatalogEntry;
import com.github.wcy6457.recipeSmith.model.RecipeChoiceDefinition;
import com.github.wcy6457.recipeSmith.model.RecipeDefinition;
import com.github.wcy6457.recipeSmith.model.RecipeOperation;
import com.github.wcy6457.recipeSmith.service.RecipeFactory;
import com.github.wcy6457.recipeSmith.service.RecipeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RecipeGuiManager implements Listener {
    public static final String PERMISSION_VIEW = "recipesmith.view";
    public static final String PERMISSION_ADMIN = "recipesmith.admin";
    public static final String PERMISSION_RELOAD = "recipesmith.reload";

    private static final int PAGE_SIZE = 45;
    private static final int SLOT_PREVIOUS = 45;
    private static final int SLOT_BACK = 48;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_NEXT = 53;
    private static final int SLOT_SAVE = 49;
    private static final int SLOT_CANCEL = 53;
    private static final int SLOT_PATTERN = 40;
    private static final int SLOT_EXPERIENCE_DOWN = 38;
    private static final int SLOT_EXPERIENCE_UP = 39;
    private static final int SLOT_TIME_DOWN = 41;
    private static final int SLOT_TIME_UP = 42;
    private static final int SLOT_COPY_DATA = 39;
    private static final int[] SHAPED_GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int[] SHAPELESS_GRID = {10, 11, 12, 13, 14, 15, 16, 17, 18};

    private final Plugin plugin;
    private final RecipeService recipeService;
    private final NamespacedKey guiItemKey;

    public RecipeGuiManager(Plugin plugin, RecipeService recipeService) {
        this.plugin = plugin;
        this.recipeService = recipeService;
        this.guiItemKey = new NamespacedKey(plugin, "gui_item");
    }

    public void openCatalog(Player player, boolean admin, int page) {
        if (!canView(player)) {
            player.sendMessage(Component.text("You do not have permission to view recipe changes.", NamedTextColor.RED));
            return;
        }
        List<RecipeCatalogEntry> entries = recipeService.catalog().entries();
        int maxPage = Math.max(0, (entries.size() - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, maxPage));
        CatalogHolder holder = new CatalogHolder(admin, safePage);
        Inventory inventory = createInventory(holder, 54, admin ? "RecipeSmith Changes (Admin)" : "RecipeSmith Changes");
        holder.inventory(inventory);
        int start = safePage * PAGE_SIZE;
        for (int index = 0; index < PAGE_SIZE && start + index < entries.size(); index++) {
            RecipeCatalogEntry entry = entries.get(start + index);
            inventory.setItem(index, catalogItem(entry, admin));
            holder.entry(index, entry.id());
        }
        inventory.setItem(SLOT_PREVIOUS, button(Material.ARROW, "Previous Page", NamedTextColor.YELLOW, List.of("Page " + (safePage + 1))));
        inventory.setItem(SLOT_CLOSE, button(Material.BARRIER, "Close", NamedTextColor.RED, List.of()));
        inventory.setItem(SLOT_NEXT, button(Material.ARROW, "Next Page", NamedTextColor.YELLOW, List.of("Page " + (safePage + 1))));
        if (admin) {
            inventory.setItem(SLOT_BACK, button(Material.CRAFTING_TABLE, "Admin Menu", NamedTextColor.GREEN, List.of()));
        }
        player.openInventory(inventory);
    }

    public void openAdmin(Player player) {
        if (!canAdmin(player)) {
            player.sendMessage(Component.text("You do not have permission to manage recipes.", NamedTextColor.RED));
            return;
        }
        AdminHolder holder = new AdminHolder();
        Inventory inventory = createInventory(holder, 54, "RecipeSmith Admin");
        holder.inventory(inventory);
        inventory.setItem(10, button(Material.CRAFTING_TABLE, "Create Shaped", NamedTextColor.GREEN, List.of()));
        inventory.setItem(11, button(Material.CHEST, "Create Shapeless", NamedTextColor.GREEN, List.of()));
        inventory.setItem(12, button(Material.FURNACE, "Create Furnace", NamedTextColor.GREEN, List.of()));
        inventory.setItem(13, button(Material.BLAST_FURNACE, "Create Blasting", NamedTextColor.GREEN, List.of()));
        inventory.setItem(14, button(Material.SMOKER, "Create Smoking", NamedTextColor.GREEN, List.of()));
        inventory.setItem(15, button(Material.CAMPFIRE, "Create Campfire", NamedTextColor.GREEN, List.of()));
        inventory.setItem(16, button(Material.STONECUTTER, "Create Stonecutting", NamedTextColor.GREEN, List.of()));
        inventory.setItem(19, button(Material.SMITHING_TABLE, "Create Smithing Transform", NamedTextColor.GREEN, List.of()));
        inventory.setItem(20, button(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, "Create Smithing Trim", NamedTextColor.GREEN, List.of()));
        inventory.setItem(21, button(Material.PURPLE_SHULKER_BOX, "Create Transmute", NamedTextColor.GREEN, List.of()));
        inventory.setItem(24, button(Material.BOOK, "View RecipeSmith Changes", NamedTextColor.AQUA, List.of("Left click to browse", "Right click entries there to delete")));
        inventory.setItem(25, button(Material.COMPASS, "Find Recipes For Held Result", NamedTextColor.AQUA, List.of("Left click entries to replace", "Right click entries to disable")));
        inventory.setItem(31, button(Material.REDSTONE, "Reload YAML", NamedTextColor.YELLOW, List.of("Requires " + PERMISSION_RELOAD)));
        inventory.setItem(SLOT_CLOSE, button(Material.BARRIER, "Close", NamedTextColor.RED, List.of()));
        player.openInventory(inventory);
    }

    public void openSourceRecipes(Player player, int page) {
        if (!canAdmin(player)) {
            player.sendMessage(Component.text("You do not have permission to manage recipes.", NamedTextColor.RED));
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.isEmpty()) {
            player.sendMessage(Component.text("Hold a result item first.", NamedTextColor.RED));
            return;
        }
        List<Recipe> recipes = recipeService.recipesFor(hand);
        int maxPage = Math.max(0, (recipes.size() - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, maxPage));
        SourceHolder holder = new SourceHolder(safePage, recipes);
        Inventory inventory = createInventory(holder, 54, "Recipes For Held Result");
        holder.inventory(inventory);
        int start = safePage * PAGE_SIZE;
        for (int index = 0; index < PAGE_SIZE && start + index < recipes.size(); index++) {
            Recipe recipe = recipes.get(start + index);
            NamespacedKey key = recipeService.snapshotConverter().keyOf(recipe);
            ManagedRecipeType type = recipeService.snapshotConverter().typeOf(recipe);
            inventory.setItem(index, sourceRecipeItem(recipe, key, type));
            holder.slot(index, start + index);
        }
        inventory.setItem(SLOT_PREVIOUS, button(Material.ARROW, "Previous Page", NamedTextColor.YELLOW, List.of()));
        inventory.setItem(SLOT_BACK, button(Material.CRAFTING_TABLE, "Admin Menu", NamedTextColor.GREEN, List.of()));
        inventory.setItem(SLOT_CLOSE, button(Material.BARRIER, "Close", NamedTextColor.RED, List.of()));
        inventory.setItem(SLOT_NEXT, button(Material.ARROW, "Next Page", NamedTextColor.YELLOW, List.of()));
        player.openInventory(inventory);
    }

    public void openEditor(Player player, RecipeDefinition draft, long expectedRevision, boolean creating) {
        if (!canAdmin(player)) {
            player.sendMessage(Component.text("You do not have permission to manage recipes.", NamedTextColor.RED));
            return;
        }
        RecipeDefinition workingCopy = draft.copy();
        EditorHolder holder = new EditorHolder(new AdminRecipeSession(UUID.randomUUID(), workingCopy, expectedRevision, creating));
        Inventory inventory = createInventory(holder, 54, (creating ? "Create " : "Edit ") + workingCopy.type().displayName());
        holder.inventory(inventory);
        renderEditor(inventory, holder);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof RecipeSmithHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null) {
            return;
        }
        if (holder instanceof CatalogHolder catalogHolder) {
            handleCatalogClick(player, catalogHolder, event.getSlot(), event.getClick());
        } else if (holder instanceof AdminHolder) {
            handleAdminClick(player, event.getSlot());
        } else if (holder instanceof SourceHolder sourceHolder) {
            handleSourceClick(player, sourceHolder, event.getSlot(), event.getClick());
        } else if (holder instanceof EditorHolder editorHolder) {
            handleEditorClick(player, editorHolder, event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof RecipeSmithHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof EditorHolder editorHolder) {
            editorHolder.selectedSlot(null);
        }
    }

    private void handleCatalogClick(Player player, CatalogHolder holder, int slot, ClickType clickType) {
        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == SLOT_BACK && holder.admin()) {
            openAdmin(player);
            return;
        }
        if (slot == SLOT_PREVIOUS) {
            openCatalog(player, holder.admin(), holder.page() - 1);
            return;
        }
        if (slot == SLOT_NEXT) {
            openCatalog(player, holder.admin(), holder.page() + 1);
            return;
        }
        String id = holder.entry(slot);
        if (id == null || !holder.admin()) {
            return;
        }
        recipeService.find(id).ifPresent(definition -> {
            if (clickType.isRightClick()) {
                recipeService.deleteChange(definition.id(), definition.revision()).thenAccept(result -> sendSaveResult(player, result));
                Bukkit.getScheduler().runTaskLater(plugin, () -> openCatalog(player, true, holder.page()), 2L);
                return;
            }
            if (definition.operation() == RecipeOperation.DISABLE) {
                player.sendMessage(Component.text("Disable entries can only be deleted or recreated.", NamedTextColor.YELLOW));
                return;
            }
            openEditor(player, definition, definition.revision(), false);
        });
    }

    private void handleAdminClick(Player player, int slot) {
        switch (slot) {
            case 10 -> openEditor(player, recipeService.createDraft(ManagedRecipeType.SHAPED), 0, true);
            case 11 -> openEditor(player, recipeService.createDraft(ManagedRecipeType.SHAPELESS), 0, true);
            case 12 -> openEditor(player, recipeService.createDraft(ManagedRecipeType.FURNACE), 0, true);
            case 13 -> openEditor(player, recipeService.createDraft(ManagedRecipeType.BLASTING), 0, true);
            case 14 -> openEditor(player, recipeService.createDraft(ManagedRecipeType.SMOKING), 0, true);
            case 15 -> openEditor(player, recipeService.createDraft(ManagedRecipeType.CAMPFIRE), 0, true);
            case 16 -> openEditor(player, recipeService.createDraft(ManagedRecipeType.STONECUTTING), 0, true);
            case 19 -> openEditor(player, recipeService.createDraft(ManagedRecipeType.SMITHING_TRANSFORM), 0, true);
            case 20 -> openEditor(player, recipeService.createDraft(ManagedRecipeType.SMITHING_TRIM), 0, true);
            case 21 -> openEditor(player, recipeService.createDraft(ManagedRecipeType.TRANSMUTE), 0, true);
            case 24 -> openCatalog(player, true, 0);
            case 25 -> openSourceRecipes(player, 0);
            case 31 -> {
                if (!player.hasPermission(PERMISSION_RELOAD)) {
                    player.sendMessage(Component.text("You do not have permission to reload recipes.", NamedTextColor.RED));
                    return;
                }
                player.sendMessage(Component.text("Reloading RecipeSmith YAML...", NamedTextColor.YELLOW));
                recipeService.reloadFromDisk().thenRun(() -> player.sendMessage(Component.text("RecipeSmith YAML reloaded.", NamedTextColor.GREEN)));
            }
            case SLOT_CLOSE -> player.closeInventory();
            default -> {
            }
        }
    }

    private void handleSourceClick(Player player, SourceHolder holder, int slot, ClickType clickType) {
        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == SLOT_BACK) {
            openAdmin(player);
            return;
        }
        if (slot == SLOT_PREVIOUS) {
            openSourceRecipes(player, holder.page() - 1);
            return;
        }
        if (slot == SLOT_NEXT) {
            openSourceRecipes(player, holder.page() + 1);
            return;
        }
        Integer recipeIndex = holder.recipeIndex(slot);
        if (recipeIndex == null || recipeIndex < 0 || recipeIndex >= holder.recipes().size()) {
            return;
        }
        Recipe recipe = holder.recipes().get(recipeIndex);
        NamespacedKey key = recipeService.snapshotConverter().keyOf(recipe);
        if (key == null) {
            player.sendMessage(Component.text("This recipe has no key.", NamedTextColor.RED));
            return;
        }
        if (clickType.isRightClick()) {
            RecipeDefinition disable = recipeService.createDisableDraft(key);
            recipeService.saveChange(disable, 0).thenAccept(result -> sendSaveResult(player, result));
            Bukkit.getScheduler().runTaskLater(plugin, () -> openSourceRecipes(player, holder.page()), 2L);
            return;
        }
        try {
            openEditor(player, recipeService.createReplaceDraft(recipe), 0, true);
        } catch (RuntimeException exception) {
            player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        }
    }

    private void handleEditorClick(Player player, EditorHolder holder, InventoryClickEvent event) {
        if (!canAdmin(player)) {
            player.closeInventory();
            return;
        }
        Inventory top = event.getView().getTopInventory();
        int rawSlot = event.getRawSlot();
        int slot = event.getSlot();
        if (rawSlot >= top.getSize()) {
            ItemStack clicked = event.getCurrentItem();
            Integer selectedSlot = holder.selectedSlot();
            if (selectedSlot != null && clicked != null && !clicked.isEmpty()) {
                top.setItem(selectedSlot, clicked.clone());
                updateSelectionIndicator(top, holder);
            }
            return;
        }
        if (slot == SLOT_CANCEL) {
            openAdmin(player);
            return;
        }
        if (slot == SLOT_SAVE) {
            try {
                RecipeDefinition draft = buildDraftFromEditor(top, holder.session().draft());
                recipeService.saveChange(draft, holder.session().expectedRevision()).thenAccept(result -> sendSaveResult(player, result));
                Bukkit.getScheduler().runTaskLater(plugin, () -> openCatalog(player, true, 0), 2L);
            } catch (RuntimeException exception) {
                player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
            }
            return;
        }
        if (slot == SLOT_PATTERN && holder.session().draft().type() == ManagedRecipeType.SMITHING_TRIM) {
            syncEditorSlotsToDraft(top, holder.session().draft());
            cycleTrimPattern(holder.session().draft(), event.getClick().isRightClick());
            renderEditor(top, holder);
            return;
        }
        if (slot == SLOT_COPY_DATA && isSmithing(holder.session().draft().type())) {
            syncEditorSlotsToDraft(top, holder.session().draft());
            holder.session().draft().copyDataComponents(!holder.session().draft().copyDataComponents());
            renderEditor(top, holder);
            return;
        }
        if (holder.session().draft().type().isCooking()) {
            syncEditorSlotsToDraft(top, holder.session().draft());
            adjustCooking(holder.session().draft(), slot, event.getClick());
            renderEditor(top, holder);
            return;
        }
        if (isEditableSlot(holder.session().draft().type(), slot)) {
            if (event.getClick().isRightClick()) {
                renderSlot(top, slot, null, editableSlotLabel(holder.session().draft().type(), slot));
                holder.selectedSlot(null);
            } else {
                ItemStack cursor = event.getCursor();
                if (cursor != null && !cursor.isEmpty()) {
                    top.setItem(slot, cursor.clone());
                }
                holder.selectedSlot(slot);
            }
            updateSelectionIndicator(top, holder);
        }
    }

    private RecipeDefinition buildDraftFromEditor(Inventory inventory, RecipeDefinition draft) {
        RecipeDefinition copy = draft.copy();
        syncEditorSlotsToDraft(inventory, copy);
        copy.validateBasic();
        return copy;
    }

    private void syncEditorSlotsToDraft(Inventory inventory, RecipeDefinition copy) {
        ManagedRecipeType type = copy.type();
        if (type.requiresResult()) {
            copy.result(itemOrNull(inventory.getItem(24)));
        }
        if (type == ManagedRecipeType.SHAPED) {
            ItemStack[] grid = new ItemStack[9];
            for (int index = 0; index < SHAPED_GRID.length; index++) {
                grid[index] = itemOrNull(inventory.getItem(SHAPED_GRID[index]));
            }
            copy.shape(RecipeFactory.shapeFromGrid(grid));
            copy.shapedIngredients(RecipeFactory.ingredientsFromGrid(grid));
        } else if (type == ManagedRecipeType.SHAPELESS) {
            ArrayList<RecipeChoiceDefinition> ingredients = new ArrayList<>();
            for (int slot : SHAPELESS_GRID) {
                ItemStack item = itemOrNull(inventory.getItem(slot));
                if (item != null) {
                    ingredients.add(new RecipeChoiceDefinition(item));
                }
            }
            copy.shapelessIngredients(ingredients);
        } else if (type.isCooking() || type == ManagedRecipeType.STONECUTTING) {
            copy.input(choiceOrNull(inventory.getItem(20)));
        } else if (type == ManagedRecipeType.SMITHING_TRANSFORM) {
            ItemStack template = itemOrNull(inventory.getItem(19));
            copy.template(template == null ? null : new RecipeChoiceDefinition(template));
            copy.base(choiceOrNull(inventory.getItem(20)));
            copy.addition(choiceOrNull(inventory.getItem(21)));
        } else if (type == ManagedRecipeType.SMITHING_TRIM) {
            copy.template(choiceOrNull(inventory.getItem(19)));
            copy.base(choiceOrNull(inventory.getItem(20)));
            copy.addition(choiceOrNull(inventory.getItem(21)));
        } else if (type == ManagedRecipeType.TRANSMUTE) {
            copy.input(choiceOrNull(inventory.getItem(20)));
            copy.material(choiceOrNull(inventory.getItem(21)));
        }
    }

    private void renderEditor(Inventory inventory, EditorHolder holder) {
        RecipeDefinition draft = holder.session().draft();
        inventory.clear();
        fillFrame(inventory);
        inventory.setItem(4, button(Material.NAME_TAG, draft.id(), NamedTextColor.AQUA, List.of(draft.operation().yamlName(), draft.type().displayName())));
        inventory.setItem(SLOT_SAVE, button(Material.LIME_CONCRETE, "Save", NamedTextColor.GREEN, List.of("Saves YAML and reapplies recipes")));
        inventory.setItem(SLOT_CANCEL, button(Material.BARRIER, "Cancel", NamedTextColor.RED, List.of()));
        ManagedRecipeType type = draft.type();
        if (type == ManagedRecipeType.SHAPED) {
            renderShaped(inventory, draft);
        } else if (type == ManagedRecipeType.SHAPELESS) {
            renderShapeless(inventory, draft);
        } else if (type.isCooking()) {
            renderSingleInput(inventory, draft, "Input");
            inventory.setItem(SLOT_EXPERIENCE_DOWN, button(Material.REDSTONE, "Experience -0.5", NamedTextColor.YELLOW, List.of("Current " + draft.experience())));
            inventory.setItem(SLOT_EXPERIENCE_UP, button(Material.EMERALD, "Experience +0.5", NamedTextColor.YELLOW, List.of("Current " + draft.experience())));
            inventory.setItem(SLOT_TIME_DOWN, button(Material.CLOCK, "Time -20", NamedTextColor.YELLOW, List.of("Current " + draft.cookingTime() + " ticks")));
            inventory.setItem(SLOT_TIME_UP, button(Material.CLOCK, "Time +20", NamedTextColor.YELLOW, List.of("Current " + draft.cookingTime() + " ticks")));
        } else if (type == ManagedRecipeType.STONECUTTING) {
            renderSingleInput(inventory, draft, "Input");
        } else if (type == ManagedRecipeType.SMITHING_TRANSFORM) {
            renderSlot(inventory, 19, draft.template() == null ? null : draft.template().exactItem(), "Template (optional)");
            renderSlot(inventory, 20, draft.base() == null ? null : draft.base().exactItem(), "Base");
            renderSlot(inventory, 21, draft.addition() == null ? null : draft.addition().exactItem(), "Addition");
            renderResult(inventory, draft);
            inventory.setItem(SLOT_COPY_DATA, button(Material.REPEATER, "Copy Data Components", draft.copyDataComponents() ? NamedTextColor.GREEN : NamedTextColor.RED, List.of(Boolean.toString(draft.copyDataComponents()))));
        } else if (type == ManagedRecipeType.SMITHING_TRIM) {
            renderSlot(inventory, 19, draft.template() == null ? null : draft.template().exactItem(), "Template");
            renderSlot(inventory, 20, draft.base() == null ? null : draft.base().exactItem(), "Base");
            renderSlot(inventory, 21, draft.addition() == null ? null : draft.addition().exactItem(), "Addition");
            inventory.setItem(SLOT_PATTERN, button(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, "Trim Pattern", NamedTextColor.YELLOW, List.of(draft.trimPattern())));
            inventory.setItem(SLOT_COPY_DATA, button(Material.REPEATER, "Copy Data Components", draft.copyDataComponents() ? NamedTextColor.GREEN : NamedTextColor.RED, List.of(Boolean.toString(draft.copyDataComponents()))));
        } else if (type == ManagedRecipeType.TRANSMUTE) {
            renderSlot(inventory, 20, draft.input() == null ? null : draft.input().exactItem(), "Input");
            renderSlot(inventory, 21, draft.material() == null ? null : draft.material().exactItem(), "Material");
            renderResult(inventory, draft);
        }
        Integer selectedSlot = holder.selectedSlot();
        updateSelectionIndicator(inventory, holder);
    }

    private void renderShaped(Inventory inventory, RecipeDefinition draft) {
        Map<Character, RecipeChoiceDefinition> ingredients = draft.shapedIngredients();
        char[] symbols = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
        for (int index = 0; index < SHAPED_GRID.length; index++) {
            RecipeChoiceDefinition choice = ingredients.get(symbols[index]);
            renderSlot(inventory, SHAPED_GRID[index], choice == null ? null : choice.exactItem(), "Ingredient " + symbols[index]);
        }
        renderResult(inventory, draft);
    }

    private void renderShapeless(Inventory inventory, RecipeDefinition draft) {
        List<RecipeChoiceDefinition> ingredients = draft.shapelessIngredients();
        for (int index = 0; index < SHAPELESS_GRID.length; index++) {
            RecipeChoiceDefinition choice = index < ingredients.size() ? ingredients.get(index) : null;
            renderSlot(inventory, SHAPELESS_GRID[index], choice == null ? null : choice.exactItem(), "Ingredient " + (index + 1));
        }
        renderResult(inventory, draft);
    }

    private void renderSingleInput(Inventory inventory, RecipeDefinition draft, String label) {
        renderSlot(inventory, 20, draft.input() == null ? null : draft.input().exactItem(), label);
        renderResult(inventory, draft);
    }

    private void renderResult(Inventory inventory, RecipeDefinition draft) {
        renderSlot(inventory, 24, draft.result(), "Result");
    }

    private void renderSlot(Inventory inventory, int slot, ItemStack item, String label) {
        if (item == null || item.isEmpty()) {
            inventory.setItem(slot, button(Material.LIGHT_GRAY_STAINED_GLASS_PANE, label, NamedTextColor.GRAY, List.of("Left click to select", "Right click to clear")));
            return;
        }
        inventory.setItem(slot, item.clone());
    }

    private void updateSelectionIndicator(Inventory inventory, EditorHolder holder) {
        Integer selectedSlot = holder.selectedSlot();
        if (selectedSlot != null && isEditableSlot(holder.session().draft().type(), selectedSlot)) {
            inventory.setItem(46, button(Material.YELLOW_STAINED_GLASS_PANE, "Selected Slot " + selectedSlot, NamedTextColor.YELLOW, List.of("Click an item in your inventory to copy it")));
        } else {
            inventory.setItem(46, button(Material.GRAY_STAINED_GLASS_PANE, "No Slot Selected", NamedTextColor.GRAY, List.of("Left click an editor slot first")));
        }
    }

    private String editableSlotLabel(ManagedRecipeType type, int slot) {
        if (slot == 24) {
            return "Result";
        }
        if (type == ManagedRecipeType.SMITHING_TRANSFORM || type == ManagedRecipeType.SMITHING_TRIM) {
            if (slot == 19) {
                return "Template";
            }
            if (slot == 20) {
                return "Base";
            }
            if (slot == 21) {
                return "Addition";
            }
        }
        if (type == ManagedRecipeType.TRANSMUTE && slot == 21) {
            return "Material";
        }
        return "Input";
    }

    private void adjustCooking(RecipeDefinition draft, int slot, ClickType clickType) {
        if (slot == SLOT_EXPERIENCE_DOWN) {
            draft.experience(Math.max(0, draft.experience() - 0.5F));
        } else if (slot == SLOT_EXPERIENCE_UP) {
            draft.experience(draft.experience() + 0.5F);
        } else if (slot == SLOT_TIME_DOWN) {
            draft.cookingTime(Math.max(0, draft.cookingTime() - (clickType.isShiftClick() ? 100 : 20)));
        } else if (slot == SLOT_TIME_UP) {
            draft.cookingTime(draft.cookingTime() + (clickType.isShiftClick() ? 100 : 20));
        }
    }

    private void cycleTrimPattern(RecipeDefinition draft, boolean backwards) {
        String[] patterns = {
                "minecraft:bolt", "minecraft:coast", "minecraft:dune", "minecraft:eye", "minecraft:flow",
                "minecraft:host", "minecraft:raiser", "minecraft:rib", "minecraft:sentry", "minecraft:shaper",
                "minecraft:silence", "minecraft:snout", "minecraft:spire", "minecraft:tide", "minecraft:vex",
                "minecraft:ward", "minecraft:wayfinder", "minecraft:wild"
        };
        int current = 0;
        for (int index = 0; index < patterns.length; index++) {
            if (patterns[index].equalsIgnoreCase(draft.trimPattern())) {
                current = index;
                break;
            }
        }
        int next = backwards ? current - 1 : current + 1;
        if (next < 0) {
            next = patterns.length - 1;
        }
        if (next >= patterns.length) {
            next = 0;
        }
        draft.trimPattern(patterns[next]);
    }

    private boolean isEditableSlot(ManagedRecipeType type, int slot) {
        if (type == ManagedRecipeType.SHAPED) {
            return contains(SHAPED_GRID, slot) || slot == 24;
        }
        if (type == ManagedRecipeType.SHAPELESS) {
            return contains(SHAPELESS_GRID, slot) || slot == 24;
        }
        if (type.isCooking() || type == ManagedRecipeType.STONECUTTING) {
            return slot == 20 || slot == 24;
        }
        if (type == ManagedRecipeType.SMITHING_TRANSFORM) {
            return slot == 19 || slot == 20 || slot == 21 || slot == 24;
        }
        if (type == ManagedRecipeType.SMITHING_TRIM) {
            return slot == 19 || slot == 20 || slot == 21;
        }
        if (type == ManagedRecipeType.TRANSMUTE) {
            return slot == 20 || slot == 21 || slot == 24;
        }
        return false;
    }

    private boolean isSmithing(ManagedRecipeType type) {
        return type == ManagedRecipeType.SMITHING_TRANSFORM || type == ManagedRecipeType.SMITHING_TRIM;
    }

    private ItemStack catalogItem(RecipeCatalogEntry entry, boolean admin) {
        ItemStack icon = entry.iconCopy();
        if (icon == null || icon.isEmpty()) {
            icon = new ItemStack(Material.KNOWLEDGE_BOOK);
        }
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text(entry.id(), entry.invalid() ? NamedTextColor.RED : NamedTextColor.AQUA));
        ArrayList<Component> lore = new ArrayList<>();
        lore.add(Component.text(entry.operation().yamlName() + " / " + entry.type().displayName(), NamedTextColor.GRAY));
        lore.add(Component.text(entry.key(), NamedTextColor.DARK_GRAY));
        if (entry.sourceKey() != null) {
            lore.add(Component.text("source: " + entry.sourceKey(), NamedTextColor.DARK_GRAY));
        }
        if (entry.invalid()) {
            lore.add(Component.text("invalid: " + entry.message(), NamedTextColor.RED));
        } else if (admin) {
            lore.add(Component.text("Left click edit, right click delete", NamedTextColor.YELLOW));
        }
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack sourceRecipeItem(Recipe recipe, NamespacedKey key, ManagedRecipeType type) {
        ItemStack icon = recipe.getResult();
        if (icon == null || icon.isEmpty()) {
            icon = new ItemStack(type == ManagedRecipeType.SMITHING_TRIM ? Material.SMITHING_TABLE : Material.KNOWLEDGE_BOOK);
        }
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text(key == null ? "Unkeyed recipe" : key.toString(), type == null ? NamedTextColor.RED : NamedTextColor.AQUA));
        ArrayList<Component> lore = new ArrayList<>();
        lore.add(Component.text(type == null ? "Unsupported API recipe" : type.displayName(), NamedTextColor.GRAY));
        if (type != null) {
            lore.add(Component.text("Left click replace", NamedTextColor.YELLOW));
            lore.add(Component.text("Right click disable", NamedTextColor.YELLOW));
        }
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private void fillFrame(Inventory inventory) {
        ItemStack pane = button(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY, List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) {
                inventory.setItem(slot, pane);
            }
        }
    }

    private Inventory createInventory(RecipeSmithHolder holder, int size, String title) {
        return Bukkit.createInventory(holder, size, Component.text(title));
    }

    private ItemStack button(Material material, String name, NamedTextColor color, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color));
        meta.getPersistentDataContainer().set(guiItemKey, PersistentDataType.BYTE, (byte) 1);
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(line -> Component.text(line, NamedTextColor.GRAY)).toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack itemOrNull(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(guiItemKey, PersistentDataType.BYTE)) {
            return null;
        }
        return item.clone();
    }

    private RecipeChoiceDefinition choiceOrNull(ItemStack item) {
        ItemStack realItem = itemOrNull(item);
        return realItem == null ? null : new RecipeChoiceDefinition(realItem);
    }

    private boolean contains(int[] slots, int slot) {
        for (int value : slots) {
            if (value == slot) {
                return true;
            }
        }
        return false;
    }

    private boolean canView(Player player) {
        return player.hasPermission(PERMISSION_VIEW);
    }

    private boolean canAdmin(Player player) {
        return player.hasPermission(PERMISSION_ADMIN);
    }

    private void sendSaveResult(Player player, RecipeService.SaveResult result) {
        NamedTextColor color = result.success() ? NamedTextColor.GREEN : result.conflict() ? NamedTextColor.YELLOW : NamedTextColor.RED;
        player.sendMessage(Component.text(result.message(), color));
    }

    private abstract static class RecipeSmithHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        void inventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }

    private static final class CatalogHolder extends RecipeSmithHolder {
        private final boolean admin;
        private final int page;
        private final Map<Integer, String> entries = new HashMap<>();

        private CatalogHolder(boolean admin, int page) {
            this.admin = admin;
            this.page = page;
        }

        boolean admin() {
            return admin;
        }

        int page() {
            return page;
        }

        void entry(int slot, String id) {
            entries.put(slot, id);
        }

        String entry(int slot) {
            return entries.get(slot);
        }
    }

    private static final class AdminHolder extends RecipeSmithHolder {
    }

    private static final class SourceHolder extends RecipeSmithHolder {
        private final int page;
        private final List<Recipe> recipes;
        private final Map<Integer, Integer> slotToRecipeIndex = new HashMap<>();

        private SourceHolder(int page, List<Recipe> recipes) {
            this.page = page;
            this.recipes = List.copyOf(recipes);
        }

        int page() {
            return page;
        }

        List<Recipe> recipes() {
            return recipes;
        }

        void slot(int slot, int recipeIndex) {
            slotToRecipeIndex.put(slot, recipeIndex);
        }

        Integer recipeIndex(int slot) {
            return slotToRecipeIndex.get(slot);
        }
    }

    private static final class EditorHolder extends RecipeSmithHolder {
        private final AdminRecipeSession session;
        private Integer selectedSlot;

        private EditorHolder(AdminRecipeSession session) {
            this.session = session;
        }

        AdminRecipeSession session() {
            return session;
        }

        Integer selectedSlot() {
            return selectedSlot;
        }

        void selectedSlot(Integer selectedSlot) {
            this.selectedSlot = selectedSlot;
        }
    }
}
