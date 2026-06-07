package com.github.wcy6457.recipeSmith.model;

import java.util.UUID;

public final class AdminRecipeSession {
    private final UUID sessionId;
    private final RecipeDefinition draft;
    private final long expectedRevision;
    private final boolean creating;

    public AdminRecipeSession(UUID sessionId, RecipeDefinition draft, long expectedRevision, boolean creating) {
        this.sessionId = sessionId;
        this.draft = draft;
        this.expectedRevision = expectedRevision;
        this.creating = creating;
    }

    public UUID sessionId() {
        return sessionId;
    }

    public RecipeDefinition draft() {
        return draft;
    }

    public long expectedRevision() {
        return expectedRevision;
    }

    public boolean creating() {
        return creating;
    }
}
