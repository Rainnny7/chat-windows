package me.braydon.chatutilities.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A match pattern with one or more {@link ChatActionEffect}s (e.g. multiple sounds for the same text). */
public final class ChatActionGroup {

    private String patternSource;
    private final List<ChatActionEffect> effects = new ArrayList<>();

    public ChatActionGroup(String patternSource) {
        this.patternSource = patternSource == null ? "" : patternSource;
    }

    /** Copy constructor for profile rename. */
    public ChatActionGroup(ChatActionGroup other) {
        this.patternSource = other.patternSource;
        for (ChatActionEffect e : other.effects) {
            this.effects.add(e.copy());
        }
    }

    public String getPatternSource() {
        return patternSource;
    }

    public void setPatternSource(String patternSource) {
        this.patternSource = patternSource == null ? "" : patternSource;
    }

    public List<ChatActionEffect> getEffects() {
        return effects;
    }

    public void addEffect(ChatActionEffect effect) {
        effects.add(Objects.requireNonNull(effect));
    }
}
