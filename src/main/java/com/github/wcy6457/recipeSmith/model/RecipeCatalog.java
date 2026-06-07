package com.github.wcy6457.recipeSmith.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RecipeCatalog {
    private static final RecipeCatalog EMPTY = new RecipeCatalog(List.of(), 0);

    private final List<RecipeCatalogEntry> entries;
    private final long revision;

    public RecipeCatalog(List<RecipeCatalogEntry> entries, long revision) {
        ArrayList<RecipeCatalogEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(RecipeCatalogEntry::id));
        this.entries = List.copyOf(sorted);
        this.revision = revision;
    }

    public static RecipeCatalog empty() {
        return EMPTY;
    }

    public List<RecipeCatalogEntry> entries() {
        return entries;
    }

    public long revision() {
        return revision;
    }
}
