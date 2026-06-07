package com.github.wcy6457.recipeSmith.command;

import com.github.wcy6457.recipeSmith.gui.RecipeGuiManager;
import com.github.wcy6457.recipeSmith.model.ManagedRecipeType;
import com.github.wcy6457.recipeSmith.model.RecipeCatalogEntry;
import com.github.wcy6457.recipeSmith.model.RecipeDefinition;
import com.github.wcy6457.recipeSmith.service.RecipeService;
import com.github.wcy6457.recipeSmith.util.RecipeKeys;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class RecipesCommand implements BasicCommand {
    private final RecipeService recipeService;
    private final RecipeGuiManager guiManager;
    private final Plugin plugin;

    public RecipesCommand(RecipeService recipeService, RecipeGuiManager guiManager, Plugin plugin) {
        this.recipeService = recipeService;
        this.guiManager = guiManager;
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull CommandSourceStack commandSourceStack, @NotNull String[] args) {
        CommandSender sender = commandSourceStack.getSender();
        if (args.length == 0 || args[0].equalsIgnoreCase("view")) {
            Player player = requirePlayer(sender);
            if (player != null) {
                guiManager.openCatalog(player, false, pageArg(args, 1));
            }
            return;
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "admin" -> {
                Player player = requirePlayer(sender);
                if (player != null && require(sender, RecipeGuiManager.PERMISSION_ADMIN)) {
                    guiManager.openAdmin(player);
                }
            }
            case "list" -> list(sender);
            case "reload" -> reload(sender);
            case "add" -> add(sender, args);
            case "replace" -> replace(sender, args);
            case "disable" -> disable(sender, args);
            default -> help(sender);
        }
    }

    @Override
    public Collection<String> suggest(@NotNull CommandSourceStack commandSourceStack, @NotNull String[] args) {
        ArrayList<String> suggestions = new ArrayList<>();
        if (args.length <= 1) {
            addMatching(suggestions, args.length == 0 ? "" : args[0], List.of("view", "admin", "list", "reload", "add", "replace", "disable"));
            return suggestions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            addMatching(suggestions, args[1], typeNames());
            return suggestions;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("replace") || args[0].equalsIgnoreCase("disable"))) {
            recipeService.catalog().entries().stream()
                    .map(RecipeCatalogEntry::sourceKey)
                    .filter(value -> value != null && value.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .forEach(suggestions::add);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("replace")) {
            addMatching(suggestions, args[2], typeNames());
        }
        return suggestions;
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission(RecipeGuiManager.PERMISSION_VIEW);
    }

    private void list(CommandSender sender) {
        if (!require(sender, RecipeGuiManager.PERMISSION_VIEW)) {
            return;
        }
        List<RecipeCatalogEntry> entries = recipeService.catalog().entries();
        sender.sendMessage(Component.text("RecipeSmith changes: " + entries.size(), NamedTextColor.AQUA));
        entries.stream().limit(20).forEach(entry -> {
            NamedTextColor color = entry.invalid() ? NamedTextColor.RED : NamedTextColor.GRAY;
            sender.sendMessage(Component.text("- " + entry.id() + " " + entry.operation().yamlName() + " " + entry.key(), color));
        });
        if (entries.size() > 20) {
            sender.sendMessage(Component.text("Open /recipes view for the full list.", NamedTextColor.GRAY));
        }
    }

    private void reload(CommandSender sender) {
        if (!require(sender, RecipeGuiManager.PERMISSION_RELOAD)) {
            return;
        }
        sender.sendMessage(Component.text("Reloading RecipeSmith YAML...", NamedTextColor.YELLOW));
        recipeService.reloadFromDisk().thenRun(() -> sender.sendMessage(Component.text("RecipeSmith YAML reloaded.", NamedTextColor.GREEN)));
    }

    private void add(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !require(sender, RecipeGuiManager.PERMISSION_ADMIN)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /recipes add <type>", NamedTextColor.RED));
            return;
        }
        try {
            ManagedRecipeType type = ManagedRecipeType.fromYaml(args[1]);
            guiManager.openEditor(player, recipeService.createDraft(type), 0, true);
        } catch (RuntimeException exception) {
            sender.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        }
    }

    private void replace(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !require(sender, RecipeGuiManager.PERMISSION_ADMIN)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /recipes replace <source-key>", NamedTextColor.RED));
            return;
        }
        try {
            NamespacedKey key = RecipeKeys.parse(args[1], plugin);
            Recipe recipe = Bukkit.getRecipe(key);
            if (recipe == null) {
                sender.sendMessage(Component.text("Recipe not found: " + key, NamedTextColor.RED));
                return;
            }
            RecipeDefinition draft = recipeService.createReplaceDraft(recipe);
            if (args.length >= 3) {
                draft.type(ManagedRecipeType.fromYaml(args[2]));
            }
            guiManager.openEditor(player, draft, 0, true);
        } catch (RuntimeException exception) {
            sender.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        }
    }

    private void disable(CommandSender sender, String[] args) {
        if (!require(sender, RecipeGuiManager.PERMISSION_ADMIN)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /recipes disable <source-key>", NamedTextColor.RED));
            return;
        }
        try {
            NamespacedKey key = RecipeKeys.parse(args[1], plugin);
            RecipeDefinition draft = recipeService.createDisableDraft(key);
            recipeService.saveChange(draft, 0).thenAccept(result -> {
                NamedTextColor color = result.success() ? NamedTextColor.GREEN : NamedTextColor.RED;
                sender.sendMessage(Component.text(result.message(), color));
            });
        } catch (RuntimeException exception) {
            sender.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        }
    }

    private void help(CommandSender sender) {
        sender.sendMessage(Component.text("/recipes view", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/recipes admin", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/recipes add <type>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/recipes replace <source-key>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/recipes disable <source-key>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/recipes reload", NamedTextColor.GRAY));
    }

    private boolean require(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sender.sendMessage(Component.text("Missing permission: " + permission, NamedTextColor.RED));
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(Component.text("This command requires a player.", NamedTextColor.RED));
        return null;
    }

    private int pageArg(String[] args, int index) {
        if (args.length <= index) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(args[index]) - 1);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private List<String> typeNames() {
        return java.util.Arrays.stream(ManagedRecipeType.values()).map(ManagedRecipeType::yamlName).toList();
    }

    private void addMatching(List<String> suggestions, String input, List<String> values) {
        String normalized = input.toLowerCase(Locale.ROOT);
        values.stream().filter(value -> value.startsWith(normalized)).forEach(suggestions::add);
    }
}
