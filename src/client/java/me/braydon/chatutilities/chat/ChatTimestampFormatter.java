package me.braydon.chatutilities.chat;

import me.braydon.chatutilities.client.ChatUtilitiesClientOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/** Builds optional chat timestamp prefixes from client settings. */
public final class ChatTimestampFormatter {

    private ChatTimestampFormatter() {}

    public static MutableComponent nowComponent() {
        return componentAtMillis(System.currentTimeMillis());
    }

    public static MutableComponent componentAtMillis(long epochMs) {
        String plain = formatPlainAtMillis(epochMs);
        if (plain.isEmpty()) {
            return Component.empty();
        }
        int rgb = ChatUtilitiesClientOptions.getChatTimestampColorRgb() & 0xFFFFFF;
        // Hover text must not bake a “relative” phrase: vanilla evaluates ShowText once, so use exact time only.
        Component hover = Component.translatable("chat-utilities.chat_timestamp.hover", formatExactDateTimeAtMillis(epochMs));
        Style style =
                Style.EMPTY
                        .withColor(rgb)
                        .withHoverEvent(new HoverEvent.ShowText(hover));
        return Component.literal(plain).withStyle(style);
    }

    /** Plain text (with trailing space when non-empty) for vanilla stacking / display. */
    public static String formatNowPlain() {
        return formatPlainAtMillis(System.currentTimeMillis());
    }

    /**
     * Same as chat display: empty when timestamps disabled; otherwise formatted segment plus one trailing space before
     * message body.
     */
    public static String formatPlainAtMillis(long epochMs) {
        if (!ChatUtilitiesClientOptions.isChatTimestampsEnabled()) {
            return "";
        }
        return formatSegmentAtMillis(epochMs, ChatUtilitiesClientOptions.getChatTimestampFormatPattern()) + " ";
    }

    /**
     * Settings preview: formats {@code epochMs} with {@code pattern} (or default if blank), ignores the timestamps
     * enabled flag. Includes one trailing space like in chat when non-empty.
     */
    public static String previewPlainForPattern(String pattern, long epochMs) {
        String raw = pattern == null || pattern.isBlank()
                ? ChatUtilitiesClientOptions.CHAT_TIMESTAMP_FORMAT_DEFAULT
                : pattern.strip();
        String seg = formatSegmentAtMillis(epochMs, raw);
        return seg.isEmpty() ? "" : seg + " ";
    }

    private static String formatSegmentAtMillis(long epochMs, String rawPattern) {
        String raw = rawPattern == null || rawPattern.isBlank()
                ? ChatUtilitiesClientOptions.CHAT_TIMESTAMP_FORMAT_DEFAULT
                : rawPattern.strip();
        DateTimeFormatter fmt;
        try {
            fmt = DateTimeFormatter.ofPattern(raw, Locale.US);
        } catch (IllegalArgumentException ignored) {
            fmt =
                    DateTimeFormatter.ofPattern(
                            ChatUtilitiesClientOptions.CHAT_TIMESTAMP_FORMAT_DEFAULT, Locale.US);
        }
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
        try {
            return zdt.format(fmt).strip();
        } catch (Exception ignored) {
            return zdt.format(
                            DateTimeFormatter.ofPattern(
                                    ChatUtilitiesClientOptions.CHAT_TIMESTAMP_FORMAT_DEFAULT, Locale.US))
                    .strip();
        }
    }

    private static String formatExactDateTimeAtMillis(long epochMs) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
        DateTimeFormatter fmt =
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM)
                        .withLocale(Locale.getDefault());
        try {
            return zdt.format(fmt);
        } catch (Exception ignored) {
            return zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT));
        }
    }

    /** Styled prefix for vanilla chat (empty when timestamps off). */
    public static Component prefixVanillaChat() {
        MutableComponent c = nowComponent();
        return c.getString().isEmpty() ? Component.empty() : c;
    }
}
