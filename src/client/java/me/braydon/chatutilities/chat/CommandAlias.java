package me.braydon.chatutilities.chat;

import java.util.Objects;

/** One client-side command alias: first token {@code from} expands to {@code to}; arguments are preserved. */
public record CommandAlias(String from, String to) {
    public CommandAlias {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        from = stripSlash(from.strip());
        to = stripSlash(to.strip());
    }

    private static String stripSlash(String s) {
        if (s.startsWith("/")) {
            return s.substring(1);
        }
        return s;
    }
}
