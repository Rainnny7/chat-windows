package me.braydon.chatutilities.chat;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.braydon.chatutilities.client.ChatUtilitiesClientOptions;
import me.braydon.chatutilities.mixin.client.ChatComponentAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Merges consecutive duplicate chat lines in the vanilla HUD into one line with a gray {@code (xN)} suffix.
 */
public final class VanillaChatRepeatStacker {
    /** Plain text after formatting strip; matches a trailing repeat suffix from this mod. */
    private static final Pattern PLAIN_STACK_SUFFIX = Pattern.compile(" \\(x(\\d+)\\)$");

    /** Last sibling is only {@code (xN)} (stack counter). */
    private static final Pattern LITERAL_COUNTER_ONLY = Pattern.compile("^\\(x\\d+\\)$");

    /**
     * Leading timestamp injected by {@link ChatTimestampFormatter} — e.g. {@code [12:34] } or
     * {@code [12:34:56] }.  Stripped before key comparison so messages that arrive in different
     * clock-minutes still merge into a single stack.
     */
    private static final Pattern TIMESTAMP_PREFIX =
            Pattern.compile("^\\s*\\[\\d{1,2}:\\d{2}(?::\\d{2})?\\]\\s*");

    private VanillaChatRepeatStacker() {}

    public static void afterAddMessage(ChatComponent chat, Component ignoredLatestDuplicateParam) {
        if (!ChatUtilitiesClientOptions.isStackRepeatedMessages()) {
            return;
        }
        ChatComponentAccess access = (ChatComponentAccess) chat;
        List<GuiMessage> msgs = access.chatUtilities$getAllMessages();
        if (msgs.size() < 2) {
            return;
        }
        GuiMessage newest = msgs.get(0);
        GuiMessage older = msgs.get(1);
        if (!plainStackKeysEqual(newest.content(), older.content())) {
            return;
        }
        int next = parseDisplayedCount(older.content()) + 1;
        Component base = stackBaseContent(newest.content(), older.content());
        // If the resolved base collapses to an empty key, refuse to merge — merging would produce
        // a suffix-only line like "(x2)" that then self-matches with any other empty-key line.
        if (plainStackKey(base).isEmpty()) {
            return;
        }
        GuiMessage merged =
                new GuiMessage(
                        // Preserve the original line's time so smooth-chat doesn't re-animate when it becomes (xN).
                        older.addedTime(),
                        withStackSuffix(base, next),
                        newest.signature(),
                        newest.tag());
        msgs.remove(0);
        msgs.remove(0);
        msgs.add(0, merged);
        ChatMessageRebuildGuard.enter();
        try {
            access.chatUtilities$refreshTrimmedMessages();
        } finally {
            ChatMessageRebuildGuard.exit();
        }
    }

    private static Component stackBaseContent(Component newest, Component older) {
        Component strippedNew = stripTrailingCounterSibling(newest);
        // Never allow the base to collapse to nothing, or we end up rendering lines that look like "(x2)" only.
        if (ChatUtilitiesManager.plainTextForMatching(strippedNew).strip().isEmpty()) {
            return newest;
        }
        boolean shortened =
                !ChatUtilitiesManager.plainTextForMatching(strippedNew)
                        .equals(ChatUtilitiesManager.plainTextForMatching(newest));
        if (parseDisplayedCount(newest) <= 1 || shortened) {
            return strippedNew;
        }
        Component strippedOlder = stripTrailingCounterSibling(older);
        if (ChatUtilitiesManager.plainTextForMatching(strippedOlder).strip().isEmpty()) {
            return older;
        }
        return strippedOlder;
    }

    /**
     * Removes a trailing sibling that is only {@code (xN)} (gray counter), matching how {@link #withStackSuffix}
     * appends the counter.
     */
    private static Component stripTrailingCounterSibling(Component c) {
        if (!(c instanceof MutableComponent mc)) {
            return c;
        }
        List<Component> sibs = mc.getSiblings();
        if (sibs.isEmpty()) {
            return c;
        }
        Component last = sibs.get(sibs.size() - 1);
        String pl = ChatUtilitiesManager.plainTextForMatching(last).strip();
        if (!LITERAL_COUNTER_ONLY.matcher(pl).matches()) {
            return c;
        }
        MutableComponent out = Component.empty();
        for (int i = 0; i < sibs.size() - 1; i++) {
            out.append(sibs.get(i));
        }
        if (sibs.size() == 1 && out.getString().isEmpty()) {
            return c;
        }
        return out;
    }

    private static boolean plainStackKeysEqual(Component a, Component b) {
        String ka = plainStackKey(a);
        // An empty key means the text collapses to nothing (e.g. a bare suffix like "(x2)").
        // Never treat two empty keys as matching — that would stack suffix-only lines endlessly.
        if (ka.isEmpty()) {
            return false;
        }
        return ka.equals(plainStackKey(b));
    }

    private static String plainStackKey(Component c) {
        String p = ChatUtilitiesManager.plainTextForMatching(c);
        // Strip a leading timestamp added by ChatTimestampFormatter so messages that arrive in
        // different clock-minutes still merge into one stack entry.
        Matcher ts = TIMESTAMP_PREFIX.matcher(p);
        if (ts.find()) {
            p = p.substring(ts.end());
        }
        Matcher m = PLAIN_STACK_SUFFIX.matcher(p);
        if (m.find()) {
            p = p.substring(0, m.start());
        }
        p = stripInvisibleEverywhere(p);
        // Collapse whitespace so visually identical lines stack even if spacing differs.
        p = p.replaceAll("\\s+", " ").strip();
        return p;
    }

    private static String stripInvisibleEverywhere(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\uFEFF'
                    || c == '\u200B'
                    || c == '\u200C'
                    || c == '\u200D'
                    || c == '\u200E'
                    || c == '\u200F'
                    || c == '\u2060'
                    || (c >= '\u2066' && c <= '\u2069')) {
                continue;
            }
            b.append(c);
        }
        return b.toString();
    }

    private static int parseDisplayedCount(Component c) {
        String p = ChatUtilitiesManager.plainTextForMatching(c);
        Matcher m = PLAIN_STACK_SUFFIX.matcher(p);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 1;
    }

    private static Component withStackSuffix(Component base, int count) {
        return Component.empty()
                .append(base)
                .append(Component.literal(" (x" + count + ")").withStyle(ChatFormatting.GRAY));
    }
}
