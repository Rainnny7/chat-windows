package me.braydon.chatutilities.chat;

import me.braydon.chatutilities.client.ChatUtilitiesClientOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public record ChatWindowLine(Component baseContent, int addedGuiTick, int stackCount, long addedWallTimeMs) {
    public ChatWindowLine {
        stackCount = Math.max(1, stackCount);
    }

    public static ChatWindowLine single(Component message, int tick, long wallTimeMs) {
        return new ChatWindowLine(message, tick, 1, wallTimeMs);
    }

    /** Full line for rendering and hit-testing (includes gray {@code (xN)} when stacked). */
    public Component styled() {
        Component body =
                stackCount <= 1
                        ? baseContent
                        : Component.empty()
                                .append(baseContent)
                                .append(Component.literal(" (x" + stackCount + ")").withStyle(ChatFormatting.GRAY));
        if (!ChatUtilitiesClientOptions.isChatTimestampsEnabled()) {
            return body;
        }
        MutableComponent ts = ChatTimestampFormatter.componentAtMillis(addedWallTimeMs);
        if (ts.getString().isEmpty()) {
            return body;
        }
        return Component.empty().append(ts).append(body);
    }

    boolean sameStackAs(ChatWindowLine incoming) {
        String a = ChatUtilitiesManager.plainTextForMatching(baseContent);
        String b = ChatUtilitiesManager.plainTextForMatching(incoming.baseContent);
        if (a.isBlank() || b.isBlank()) {
            return false;
        }
        return a.equals(b);
    }

    ChatWindowLine mergedWithRepeat() {
        return new ChatWindowLine(baseContent, addedGuiTick, stackCount + 1, addedWallTimeMs);
    }
}
