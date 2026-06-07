package com.github.wcy6457.recipeSmith.command;

import com.github.wcy6457.recipeSmith.gui.RecipeGuiManager;
import com.github.wcy6457.recipeSmith.i18n.LanguageService;
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
    private final LanguageService language;
    private final Plugin plugin;

    public RecipesCommand(RecipeService recipeService, RecipeGuiManager guiManager, LanguageService language, Plugin plugin) {
        this.recipeService = recipeService;
        this.guiManager = guiManager;
        this.language = language;
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
            case "language", "lang" -> language(sender, args);
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
            addMatching(suggestions, args.length == 0 ? "" : args[0], List.of("view", "admin", "list", "reload", "language", "add", "replace", "disable"));
            return suggestions;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("language") || args[0].equalsIgnoreCase("lang"))) {
            addMatching(suggestions, args[1], List.copyOf(language.availableLanguages()));
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
        sender.sendMessage(language.component("command.changes-count", NamedTextColor.AQUA, LanguageService.arg("count", entries.size())));
        entries.stream().limit(20).forEach(entry -> {
            NamedTextColor color = entry.invalid() ? NamedTextColor.RED : NamedTextColor.GRAY;
            sender.sendMessage(language.component(
                    "command.list-entry",
                    color,
                    LanguageService.arg("id", entry.id()),
                    LanguageService.arg("operation", language.operationName(entry.operation())),
                    LanguageService.arg("key", entry.key())
            ));
        });
        if (entries.size() > 20) {
            sender.sendMessage(language.component("command.list-more", NamedTextColor.GRAY));
        }
    }

    private void reload(CommandSender sender) {
        if (!require(sender, RecipeGuiManager.PERMISSION_RELOAD)) {
            return;
        }
        sender.sendMessage(language.component("command.reloading", NamedTextColor.YELLOW));
        language.load();
        recipeService.reloadFromDisk().thenRun(() -> sender.sendMessage(language.component("command.reloaded", NamedTextColor.GREEN)));
    }

    private void language(CommandSender sender, String[] args) {
        if (!require(sender, RecipeGuiManager.PERMISSION_LANGUAGE)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(this.language.component("command.language-current", NamedTextColor.AQUA, LanguageService.arg("language", this.language.language())));
            sender.sendMessage(this.language.component("command.language-available", NamedTextColor.GRAY, LanguageService.arg("languages", String.join(", ", this.language.availableLanguages()))));
            sender.sendMessage(this.language.component("command.language-usage", NamedTextColor.GRAY));
            return;
        }
        String requestedLanguage = args[1];
        if (!this.language.setLanguage(requestedLanguage)) {
            sender.sendMessage(this.language.component("command.language-missing", NamedTextColor.RED, LanguageService.arg("language", requestedLanguage)));
            return;
        }
        sender.sendMessage(this.language.component("command.language-set", NamedTextColor.GREEN, LanguageService.arg("language", this.language.language())));
    }

    private void add(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !require(sender, RecipeGuiManager.PERMISSION_ADMIN)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(language.component("command.usage-add", NamedTextColor.RED));
            return;
        }
        try {
            ManagedRecipeType type = ManagedRecipeType.fromYaml(args[1]);
            guiManager.openEditor(player, recipeService.createDraft(type), 0, true);
        } catch (RuntimeException exception) {
            sender.sendMessage(language.component("message.save-invalid", NamedTextColor.RED, LanguageService.arg("reason", exception.getMessage())));
        }
    }

    private void replace(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !require(sender, RecipeGuiManager.PERMISSION_ADMIN)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(language.component("command.usage-replace", NamedTextColor.RED));
            return;
        }
        try {
            NamespacedKey key = RecipeKeys.parse(args[1], plugin);
            Recipe recipe = Bukkit.getRecipe(key);
            if (recipe == null) {
                sender.sendMessage(language.component("command.recipe-not-found", NamedTextColor.RED, LanguageService.arg("key", key)));
                return;
            }
            RecipeDefinition draft = recipeService.createReplaceDraft(recipe);
            if (args.length >= 3) {
                draft.type(ManagedRecipeType.fromYaml(args[2]));
            }
            guiManager.openEditor(player, draft, 0, true);
        } catch (RuntimeException exception) {
            sender.sendMessage(language.component("message.save-invalid", NamedTextColor.RED, LanguageService.arg("reason", exception.getMessage())));
        }
    }

    private void disable(CommandSender sender, String[] args) {
        if (!require(sender, RecipeGuiManager.PERMISSION_ADMIN)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(language.component("command.usage-disable", NamedTextColor.RED));
            return;
        }
        try {
            NamespacedKey key = RecipeKeys.parse(args[1], plugin);
            RecipeDefinition draft = recipeService.createDisableDraft(key);
            recipeService.saveChange(draft, 0).thenAccept(result -> {
                NamedTextColor color = result.success() ? NamedTextColor.GREEN : NamedTextColor.RED;
                sender.sendMessage(saveResult(result, color));
            });
        } catch (RuntimeException exception) {
            sender.sendMessage(language.component("message.save-invalid", NamedTextColor.RED, LanguageService.arg("reason", exception.getMessage())));
        }
    }

    private void help(CommandSender sender) {
        for (String line : language.list("command.help")) {
            sender.sendMessage(Component.text(line, NamedTextColor.GRAY));
        }
    }

    private boolean require(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sender.sendMessage(language.component("command.missing-permission", NamedTextColor.RED, LanguageService.arg("permission", permission)));
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(language.component("command.player-only", NamedTextColor.RED));
        return null;
    }

    private Component saveResult(RecipeService.SaveResult result, NamedTextColor color) {
        List<LanguageService.Arg> args = result.placeholders().entrySet().stream()
                .map(entry -> LanguageService.arg(entry.getKey(), entry.getValue()))
                .toList();
        return language.component(result.messageKey(), color, args.toArray(LanguageService.Arg[]::new));
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
