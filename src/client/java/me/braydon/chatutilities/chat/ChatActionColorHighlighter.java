package me.braydon.chatutilities.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies color (and optional ChatFormatting-style decorations) on matched plain-text ranges while preserving the
 * original component tree’s literal boundaries and base styles (clicks, hovers, existing colors outside matches).
 */
public final class ChatActionColorHighlighter {

    private ChatActionColorHighlighter() {}

    /** Highlight to merge onto the base style for each matched code unit (UTF-16 index in plain text). */
    public record Rule(Pattern pattern, Style highlightOverlay) {}

    private record LiteralSeg(String text, Style style) {}

    public static Component apply(Component message, List<Rule> rules) {
        if (message == null || rules == null || rules.isEmpty()) {
            return message;
        }
        List<LiteralSeg> segs = new ArrayList<>();
        message.visit(
                (style, segment) -> {
                    if (!segment.isEmpty()) {
                        segs.add(new LiteralSeg(segment, style));
                    }
                    return Optional.empty();
                },
                Style.EMPTY);
        if (segs.isEmpty()) {
            return message;
        }
        StringBuilder plain = new StringBuilder();
        List<Integer> charSeg = new ArrayList<>();
        for (int si = 0; si < segs.size(); si++) {
            String t = segs.get(si).text;
            for (int k = 0; k < t.length(); k++) {
                charSeg.add(si);
                plain.append(t.charAt(k));
            }
        }
        int n = plain.length();
        if (n == 0) {
            return message;
        }
        Style[] overlay = new Style[n];
        for (Rule r : rules) {
            Matcher m = r.pattern().matcher(plain);
            while (m.find()) {
                int a = Math.max(0, m.start());
                int b = Math.min(n, m.end());
                Style h = r.highlightOverlay();
                for (int i = a; i < b; i++) {
                    overlay[i] = h;
                }
            }
        }
        boolean any = false;
        for (Style s : overlay) {
            if (s != null) {
                any = true;
                break;
            }
        }
        if (!any) {
            return message;
        }
        Style[] merged = new Style[n];
        for (int i = 0; i < n; i++) {
            Style base = segs.get(charSeg.get(i)).style;
            merged[i] = overlay[i] == null ? base : mergeHighlight(base, overlay[i]);
        }
        MutableComponent out = Component.empty();
        int i = 0;
        while (i < n) {
            Style runStyle = merged[i];
            int j = i + 1;
            while (j < n && styleEquals(merged[j], runStyle)) {
                j++;
            }
            out.append(Component.literal(plain.substring(i, j)).withStyle(runStyle));
            i = j;
        }
        return out;
    }

    private static boolean styleEquals(Style a, Style b) {
        return Objects.equals(a, b);
    }

    static Style mergeHighlight(Style base, Style ov) {
        Style s = base;
        TextColor tc = ov.getColor();
        if (tc != null) {
            s = s.withColor(tc);
        }
        if (ov.isBold()) {
            s = s.withBold(true);
        }
        if (ov.isItalic()) {
            s = s.withItalic(true);
        }
        if (ov.isUnderlined()) {
            s = s.withUnderlined(true);
        }
        if (ov.isStrikethrough()) {
            s = s.withStrikethrough(true);
        }
        if (ov.isObfuscated()) {
            s = s.withObfuscated(true);
        }
        return s;
    }

    /** Builds overlay style for a highlight rule (RGB + optional decorations). */
    public static Style highlightOverlayStyle(
            int rgb,
            boolean bold,
            boolean italic,
            boolean underlined,
            boolean strikethrough,
            boolean obfuscated) {
        Style s = Style.EMPTY.withColor(TextColor.fromRgb(rgb & 0xFFFFFF));
        if (bold) {
            s = s.withBold(true);
        }
        if (italic) {
            s = s.withItalic(true);
        }
        if (underlined) {
            s = s.withUnderlined(true);
        }
        if (strikethrough) {
            s = s.withStrikethrough(true);
        }
        if (obfuscated) {
            s = s.withObfuscated(true);
        }
        return s;
    }
}
